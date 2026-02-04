package com.eventara.rule.evaluation;

import com.eventara.alert.service.AlertTriggerHandler;
import com.eventara.metrics.config.MetricsProperties;
import com.eventara.metrics.model.MetricsBucket;
import com.eventara.metrics.service.RedisMetricsService;
import com.eventara.rule.entity.AlertRule;
import com.eventara.rule.enums.RuleStatus;
import com.eventara.rule.enums.RuleType;
import com.eventara.rule.evaluation.config.AdaptiveEvaluationProperties;
import com.eventara.rule.repository.RuleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Core service for Adaptive Rate-Based Evaluation.
 * 
 * Instead of evaluating rules on every event (O(N)), this service:
 * 1. Accumulates a "dirty flag" when events arrive (O(1))
 * 2. Ticks at a dynamic interval based on event rate
 * 3. On tick, fetches metrics ONCE per group (EvaluationKey)
 * 4. Evaluates all rules against the shared metrics
 * 
 * This reduces Redis load from O(events * rules) to O(rules / tick).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AdaptiveRuleEvaluator {

    private final AdaptiveEvaluationProperties config;
    private final EventRateMonitor rateMonitor;
    private final RuleRepository ruleRepository;
    private final RedisMetricsService redisMetrics;
    private final AlertTriggerHandler alertHandler;
    private final MetricsProperties metricsProperties;
    private final StringRedisTemplate stringRedisTemplate;

    // --- State ---

    /**
     * Flag indicating if new events have arrived since last evaluation.
     * Use atomic boolean for thread-safety without locking.
     */
    private final AtomicBoolean dirtyFlag = new AtomicBoolean(false);

    /**
     * Guard to ensure only one evaluation runs at a time.
     */
    private final AtomicBoolean evaluating = new AtomicBoolean(false);

    /**
     * Current evaluation interval in milliseconds.
     * Dynamically adjusted based on EPS.
     */
    private volatile long currentIntervalMs = 10_000;

    /**
     * Last time evaluation actually ran.
     */
    private volatile long lastEvaluationTime = System.currentTimeMillis();

    // --- Rule Cache ---

    private volatile List<AlertRule> cachedRules = List.of();
    private volatile long lastRuleRefresh = 0;
    private static final long RULE_CACHE_TTL_MS = 60_000;

    // --- Constants ---

    private static final String COOLDOWN_PREFIX = "eventara:rule:cooldown:";

    // Virtual metrics that shouldn't be evaluated here (handled elsewhere or
    // complex)
    private static final List<String> VIRTUAL_METRICS = List.of(
            "EVENT_RATIO", "ERROR_RATE_CHANGE", "LATENCY_CHANGE");

    @PostConstruct
    public void init() {
        if (config.isEnabled()) {
            refreshRuleCache();
            log.info("AdaptiveRuleEvaluator initialized: {} rules loaded. Initial interval={}ms",
                    cachedRules != null ? cachedRules.size() : 0, currentIntervalMs);
        } else {
            log.info("AdaptiveRuleEvaluator is DISABLED via configuration");
        }
    }

    /**
     * Called from EventConsumer when an event arrives.
     * This is the "Hot Path" - must be extremely fast O(1).
     * 
     * @param isError True if the event was an error
     */
    public void onEventIngested(boolean isError) {
        if (!config.isEnabled())
            return;

        // 1. Record stats
        rateMonitor.recordEvent(isError);

        // 2. Mark as dirty so the next tick knows to evaluate
        dirtyFlag.set(true);
    }

    /**
     * The heartbeat of the system.
     * Runs frequently (every 100ms) but only does work when needed.
     */
    @Scheduled(fixedRate = 100)
    public void checkAndEvaluate() {
        if (!config.isEnabled())
            return;

        long now = System.currentTimeMillis();
        long elapsed = now - lastEvaluationTime;

        // 1. Check if it's time to evaluate
        // 2. Check if there's actually new data (dirty flag)
        if (elapsed < currentIntervalMs || !dirtyFlag.get()) {
            return;
        }

        if (!evaluating.compareAndSet(false, true)) {
            return;
        }

        boolean evaluated = false;

        try {
            if (elapsed < currentIntervalMs || !dirtyFlag.compareAndSet(true, false)) {
                return;
            }

            // --- It's time to evaluate! ---

            lastEvaluationTime = now;
            evaluated = true;
            evaluateAllRulesGrouped();
        } catch (Exception e) {
            log.error("Error during adaptive rule evaluation", e);
        } finally {
            if (evaluated) {
                // Adjust the interval for the NEXT cycle based on current rate
                adjustInterval();
            }
            evaluating.set(false);
        }
    }

    /**
     * The core optimization: Group rules and evaluate efficiently.
     */
    private void evaluateAllRulesGrouped() {
        List<AlertRule> rules = getActiveThresholdRules();
        if (rules.isEmpty())
            return;

        int defaultWindow = metricsProperties.getBucket().getRedisRetentionMinutes();

        // Step 1: Group rules by what metrics they need (EvaluationKey)
        Map<EvaluationKey, List<AlertRule>> groups = rules.stream()
                .collect(Collectors.groupingBy(r -> EvaluationKey.fromRule(r, defaultWindow)));

        log.debug("Evaluating {} rules in {} groups", rules.size(), groups.size());

        // Step 2: Process each group
        for (Map.Entry<EvaluationKey, List<AlertRule>> entry : groups.entrySet()) {
            EvaluationKey key = entry.getKey();
            List<AlertRule> groupRules = entry.getValue();

            try {
                // FETCH METRICS ONCE PER GROUP!
                MetricsBucket bucket = fetchMetricsForKey(key);

                // Evaluate all rules in the group using the same bucket
                for (AlertRule rule : groupRules) {
                    try {
                        evaluateRuleAgainstBucket(rule, bucket, key.getWindowMinutes());
                    } catch (Exception e) {
                        log.error("Failed to evaluate rule {}: {}", rule.getId(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to fetch metrics for key {}: {}", key, e.getMessage());
            }
        }
    }

    /**
     * Adjust the evaluation interval based on the current event rate.
     */
    private void adjustInterval() {
        double eps = rateMonitor.sampleAndGetEps();
        long oldInterval = currentIntervalMs;

        currentIntervalMs = config.getIntervalForRate(eps);

        // Log meaningful changes (e.g., jumping from IDLE to BURST)
        if (Math.abs(currentIntervalMs - oldInterval) > 1000) {
            log.info("Traffic changed (eps={:.1f}). Adjusted interval: {}ms -> {}ms ({})",
                    eps, oldInterval, currentIntervalMs, config.getTierForRate(eps));
        }
    }

    /**
     * Fetch the right metrics from Redis based on the key filters.
     */
    private MetricsBucket fetchMetricsForKey(EvaluationKey key) {
        // Option A: Specific Source(s)
        if (key.hasSourceFilter()) {
            if (key.getSourceFilter().size() == 1) {
                return redisMetrics.getMetricsForSource(
                        key.getSourceFilter().get(0), key.getWindowMinutes());
            }
            return redisMetrics.getMetricsForSources(
                    key.getSourceFilter(), key.getWindowMinutes());
        }

        // Option B: Specific Event Type(s)
        if (key.hasEventTypeFilter()) {
            if (key.getEventTypeFilter().size() == 1) {
                return redisMetrics.getMetricsForEventType(
                        key.getEventTypeFilter().get(0), key.getWindowMinutes());
            }
            // Add multi-type support in RedisMetricsService if needed,
            // for now fallback to separate calls or just first one
            return redisMetrics.getMetricsForEventType(
                    key.getEventTypeFilter().get(0), key.getWindowMinutes());
        }

        // Option C: Global (All Events)
        return redisMetrics.getMetricsLastMinutes(key.getWindowMinutes());
    }

    /**
     * Evaluate a single rule against an already-fetched bucket.
     */
    @SuppressWarnings("unchecked")
    private void evaluateRuleAgainstBucket(AlertRule rule, MetricsBucket bucket, int windowMinutes) {
        Map<String, Object> configMap = rule.getRuleConfig();
        if (configMap == null)
            return;

        // Skip composite/complex rules that aren't simple thresholds
        if (configMap.containsKey("conditions"))
            return;

        String metricType = (String) configMap.get("metricType");

        // Skip handling virtual metrics here (managed by specialized logic if needed)
        if (metricType == null || isVirtualMetric(metricType))
            return;

        String condition = (String) configMap.get("condition");
        Object thresholdObj = configMap.get("thresholdValue");
        int cooldownMinutes = parseIntOrDefault(configMap.get("cooldownMinutes"), 5);

        if (condition == null || thresholdObj == null)
            return;

        double threshold = parseDouble(thresholdObj);
        double currentValue = getMetricValue(metricType, bucket, windowMinutes);

        boolean crossed = isThresholdCrossed(condition, currentValue, threshold);

        if (crossed) {
            if (!isInCooldown(rule.getId(), cooldownMinutes)) {

                log.info("ALARM: Rule '{}' crossed threshold! val={} {} thr={}",
                        rule.getName(), currentValue, condition, threshold);

                fireAlert(rule, currentValue, threshold);
                setCooldown(rule.getId(), cooldownMinutes);
            }
        }
    }

    // --- Metrics Extraction ---

    private double getMetricValue(String metricType, MetricsBucket bucket, int windowMinutes) {
        if (bucket == null)
            return 0.0;

        switch (metricType) {
            case "ERROR_RATE":
                return bucket.getErrorRate();
            case "TOTAL_ERRORS":
                return bucket.getTotalErrors();
            case "AVG_LATENCY":
                return bucket.getAvgLatency();
            case "TOTAL_EVENTS":
                return bucket.getTotalEvents();
            case "EVENTS_PER_MINUTE":
                return bucket.getTotalEvents() / (double) Math.max(1, windowMinutes);
            case "P95_LATENCY":
                return bucket.getLatencyP95() != null ? bucket.getLatencyP95() : 0.0;
            case "P99_LATENCY":
                return bucket.getLatencyP99() != null ? bucket.getLatencyP99() : 0.0;
            default:
                return 0.0;
        }
    }

    private boolean isVirtualMetric(String metricType) {
        return VIRTUAL_METRICS.contains(metricType);
    }

    private boolean isThresholdCrossed(String condition, double current, double threshold) {
        switch (condition) {
            case "GREATER_THAN":
                return current > threshold;
            case "GREATER_THAN_OR_EQUAL":
                return current >= threshold;
            case "LESS_THAN":
                return current < threshold;
            case "LESS_THAN_OR_EQUAL":
                return current <= threshold;
            case "EQUALS":
                return Math.abs(current - threshold) < 0.0001;
            default:
                return false;
        }
    }

    // --- Cooldown Management ---

    private boolean isInCooldown(Long ruleId, int cooldownMinutes) {
        String key = COOLDOWN_PREFIX + ruleId;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    private void setCooldown(Long ruleId, int cooldownMinutes) {
        String key = COOLDOWN_PREFIX + ruleId;
        stringRedisTemplate.opsForValue().set(key,
                String.valueOf(System.currentTimeMillis()),
                java.time.Duration.ofMinutes(cooldownMinutes));
    }

    private void fireAlert(AlertRule rule, double currentValue, double threshold) {
        alertHandler.handleThresholdAlert(
                rule.getId(),
                rule.getName(),
                rule.getSeverity() != null ? rule.getSeverity().name() : "INFO",
                threshold,
                currentValue);
    }

    // --- Helpers ---

    private List<AlertRule> getActiveThresholdRules() {
        long now = System.currentTimeMillis();
        if ((now - lastRuleRefresh) > RULE_CACHE_TTL_MS) {
            refreshRuleCache();
        }
        return cachedRules;
    }

    public synchronized void refreshRuleCache() {
        try {
            List<AlertRule> freshRules = ruleRepository.findByRuleTypeAndStatus(RuleType.THRESHOLD, RuleStatus.ACTIVE);
            cachedRules = freshRules != null ? freshRules : List.of();
            lastRuleRefresh = System.currentTimeMillis();
        } catch (Exception e) {
            log.error("Failed to refresh rule cache", e);
            // Don't clear cache on error, keep using stale rules if possible
        }
    }

    // Helper to expose extraction for testing if needed
    public double getCurrentEps() {
        return rateMonitor.getCurrentEps();
    }

    public long getCurrentIntervalMs() {
        return currentIntervalMs;
    }

    private double parseDouble(Object value) {
        if (value == null)
            return 0.0;
        if (value instanceof Number)
            return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private int parseIntOrDefault(Object value, int defaultValue) {
        if (value == null)
            return defaultValue;
        if (value instanceof Number)
            return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}

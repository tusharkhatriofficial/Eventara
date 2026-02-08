package com.eventara.rule.evaluation;

import com.eventara.alert.service.AlertTriggerHandler;
import com.eventara.metrics.config.MetricsProperties;
import com.eventara.metrics.model.MetricsBucket;
import com.eventara.metrics.service.RedisMetricsService;
import com.eventara.rule.entity.AlertRule;
import com.eventara.rule.enums.RuleStatus;
import com.eventara.rule.enums.RuleType;
import com.eventara.rule.evaluation.config.AdaptiveEvaluationProperties;
import com.eventara.rule.evaluation.handler.HandlerRegistry;
import com.eventara.rule.evaluation.handler.RuleHandler;
import com.eventara.rule.evaluation.model.EvaluationResult;
import com.eventara.rule.repository.RuleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Core service for Adaptive Rate-Based Evaluation.
 * 
 * Instead of evaluating rules on every event (O(N)), this service:
 * 1. Accumulates a "dirty flag" when events arrive (O(1))
 * 2. Ticks at a dynamic interval based on event rate
 * 3. On tick, fetches metrics ONCE per group (EvaluationKey)
 * 4. Evaluates all rules against shared metrics using specialized handlers
 * 
 * This reduces Redis load from O(events * rules) to O(rules / tick).
 * 
 * Supported rule types via Handler Pattern:
 * - Simple Threshold (ERROR_RATE, AVG_LATENCY, etc.)
 * - Composite Rules (AND/OR conditions)
 * - Event Ratio (conversion rates)
 * - Rate of Change (spike detection)
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
    private final HandlerRegistry handlerRegistry;

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

    @PostConstruct
    public void init() {
        if (config.isEnabled()) {
            refreshRuleCache();
            log.info("AdaptiveRuleEvaluator initialized with {} handlers. {} rules loaded. Initial interval={}ms",
                    handlerRegistry.getHandlerCount(),
                    cachedRules.size(),
                    currentIntervalMs);
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
                        evaluateRuleWithHandler(rule, bucket, key.getWindowMinutes());
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
     * Evaluate a rule using the appropriate handler from HandlerRegistry.
     */
    private void evaluateRuleWithHandler(AlertRule rule, MetricsBucket bucket, int windowMinutes) {
        // Find the appropriate handler
        Optional<RuleHandler> handler = handlerRegistry.findHandler(rule);

        if (handler.isEmpty()) {
            log.warn("No handler found for rule {} (type={}, config={})",
                    rule.getId(),
                    rule.getRuleType(),
                    rule.getRuleConfig().get("metricType"));
            return;
        }

        // Get cooldown from rule config
        int cooldownMinutes = parseIntOrDefault(
                rule.getRuleConfig().get("cooldownMinutes"), 5);

        try {
            // Delegate evaluation to the handler
            Optional<EvaluationResult> result = handler.get().evaluate(rule, bucket, windowMinutes);

            if (result.isPresent() && !isInCooldown(rule.getId(), cooldownMinutes)) {
                EvaluationResult r = result.get();

                log.info("🚨 ALARM: Rule '{}' triggered! Details: {}",
                        rule.getName(), r.getDetails());

                fireAlert(rule, r.getCurrentValue(), r.getThreshold());
                setCooldown(rule.getId(), cooldownMinutes);
            }
        } catch (Exception e) {
            log.error("Handler error for rule {}: {}", rule.getId(), e.getMessage(), e);
        }
    }

    /**
     * Adjust the evaluation interval based on the current event rate.
     */
    private void adjustInterval() {
        double eps = rateMonitor.sampleAndGetEps();
        long oldInterval = currentIntervalMs;

        long newInterval = config.getIntervalForRate(eps);
        currentIntervalMs = newInterval;

        // Log interval changes (e.g., jumping from IDLE to BURST)
        if (log.isInfoEnabled() && newInterval != oldInterval) {
            String epsFormatted = String.format(java.util.Locale.US, "%.1f", eps);
            String tier = config.getTierForRate(eps);
            log.info("Traffic changed (eps={}). Adjusted interval: {}ms -> {}ms ({})",
                    epsFormatted, oldInterval, newInterval, tier);
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
            return redisMetrics.getMetricsForEventTypes(
                    key.getEventTypeFilter(), key.getWindowMinutes());
        }

        // Option C: Global (All Events)
        return redisMetrics.getMetricsLastMinutes(key.getWindowMinutes());
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
            log.debug("Rule cache refreshed: {} active rules", cachedRules.size());
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

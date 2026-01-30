package com.eventara.rule.evaluation;

import com.eventara.alert.service.AlertTriggerHandler;
import com.eventara.common.dto.EventDto;
import com.eventara.metrics.config.MetricsProperties;
import com.eventara.metrics.service.RedisMetricsService;
import com.eventara.metrics.model.MetricsBucket;
import com.eventara.rule.entity.AlertRule;
import com.eventara.rule.enums.RuleStatus;
import com.eventara.rule.enums.RuleType;
import com.eventara.rule.repository.RuleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Real-time rule evaluator that fires alerts INSTANTLY when thresholds are
 * crossed.
 * 
 * Key features:
 * - Evaluates on every event (event-driven, not scheduled)
 * - Reads metrics from Redis (respects retention window)
 * - Supports source/type/severity filtering
 * - Implements cooldown to prevent alert spam
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RealTimeRuleEvaluator {

    private final RuleRepository ruleRepository;
    private final AlertTriggerHandler alertHandler;
    private final RedisMetricsService redisMetrics;
    private final MetricsProperties metricsProperties;
    private final StringRedisTemplate stringRedisTemplate;

    // Redis key prefix for distributed cooldown
    private static final String COOLDOWN_PREFIX = "eventara:rule:cooldown:";

    // In-memory state (fallback only)
    private final Map<Long, ThresholdState> thresholdStates = new ConcurrentHashMap<>();

    // Cache of active threshold rules (refreshed periodically)
    private volatile List<AlertRule> cachedRules;
    private volatile long lastRuleRefresh = 0;
    private static final long RULE_CACHE_TTL_MS = 60_000; // Refresh every 60 seconds

    @PostConstruct
    public void init() {
        refreshRuleCache();
        log.info("RealTimeRuleEvaluator initialized with {} threshold rules",
                cachedRules != null ? cachedRules.size() : 0);
    }

    /**
     * Evaluate all instant threshold rules for an incoming event.
     * Called from EventProcessingService on every event.
     */
    public void evaluateEvent(EventDto event) {
        // Get active threshold rules (cached)
        List<AlertRule> rules = getActiveThresholdRules();
        
        log.info("🔍 Evaluating event: source={}, type={}, severity={} | {} active threshold rules found", 
                event.getSource(), event.getEventType(), event.getSeverity(), rules.size());

        for (AlertRule rule : rules) {
            try {
                // Check if rule is configured for instant evaluation
                if (!isInstantEvaluation(rule)) {
                    log.debug("Rule '{}' is not instant evaluation, skipping", rule.getName());
                    continue;
                }

                // Check if event matches rule's filters
                if (!matchesFilters(rule, event)) {
                    log.debug("Event does not match filters for rule '{}'", rule.getName());
                    continue;
                }

                log.info("✅ Evaluating rule '{}' for event {}", rule.getName(), event.getEventId());
                // Evaluate the threshold
                evaluateThreshold(rule, event);

            } catch (Exception e) {
                log.error("Error evaluating rule {} for event: {}", rule.getName(), e.getMessage(), e);
            }
        }
    }

    /**
     * Check if rule is configured for instant (event-driven) evaluation.
     */
    private boolean isInstantEvaluation(AlertRule rule) {
        Map<String, Object> config = rule.getRuleConfig();
        if (config == null)
            return true; // Default to instant

        String mode = (String) config.getOrDefault("evaluationMode", "INSTANT");
        return "INSTANT".equalsIgnoreCase(mode);
    }

    /**
     * Check if event matches rule's filters (source, type, severity).
     */
    private boolean matchesFilters(AlertRule rule, EventDto event) {
        Map<String, Object> config = rule.getRuleConfig();
        if (config == null)
            return true;

        // Check source filter
        if (config.containsKey("sourceFilter")) {
            @SuppressWarnings("unchecked")
            List<String> sources = (List<String>) config.get("sourceFilter");
            if (sources != null && !sources.isEmpty() && !sources.contains(event.getSource())) {
                return false;
            }
        }

        // Check event type filter
        if (config.containsKey("eventTypeFilter")) {
            @SuppressWarnings("unchecked")
            List<String> types = (List<String>) config.get("eventTypeFilter");
            if (types != null && !types.isEmpty() && !types.contains(event.getEventType())) {
                return false;
            }
        }

        // Check severity filter
        if (config.containsKey("severityFilter")) {
            @SuppressWarnings("unchecked")
            List<String> severities = (List<String>) config.get("severityFilter");
            if (severities != null && !severities.isEmpty()
                    && !severities.contains(event.getSeverity())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Evaluate threshold for a rule against current Redis metrics.
     * Handles three formats:
     * 1. Simple threshold: metricType + condition + thresholdValue
     * 2. EVENT_RATIO: numeratorEventType / denominatorEventType comparison
     * 3. Composite: conditions array with operator (AND/OR)
     */
    @SuppressWarnings("unchecked")
    private void evaluateThreshold(AlertRule rule, EventDto event) {
        Map<String, Object> config = rule.getRuleConfig();

        int windowMinutes = parseIntOrDefault(config.get("timeWindowMinutes"),
                metricsProperties.getBucket().getRedisRetentionMinutes());
        int cooldownMinutes = parseIntOrDefault(config.get("cooldownMinutes"), 5);

        // Cap window to Redis retention
        int maxWindow = metricsProperties.getBucket().getRedisRetentionMinutes();
        windowMinutes = Math.min(windowMinutes, maxWindow);

        String metricType = (String) config.get("metricType");
        boolean crossed = false;
        double currentValue = 0;
        double threshold = 0;
        String evaluationDetails = "";

        // Check for composite conditions first
        if (config.containsKey("conditions")) {
            List<Map<String, Object>> conditions = (List<Map<String, Object>>) config.get("conditions");
            String operator = (String) config.getOrDefault("operator", "AND");

            CompositeResult result = evaluateCompositeConditions(conditions, operator, windowMinutes, config);
            crossed = result.crossed;
            currentValue = result.primaryValue;
            threshold = result.primaryThreshold;
            evaluationDetails = result.details;
        }
        // Check for EVENT_RATIO
        else if ("EVENT_RATIO".equals(metricType)) {
            String numeratorType = (String) config.get("numeratorEventType");
            String denominatorType = (String) config.get("denominatorEventType");
            String condition = (String) config.get("condition");
            threshold = parseDouble(config.get("thresholdValue"));
            int minDenominator = parseIntOrDefault(config.get("minDenominatorEvents"), 5);

            RatioResult result = evaluateEventRatio(numeratorType, denominatorType,
                    condition, threshold, windowMinutes, minDenominator);
            crossed = result.crossed;
            currentValue = result.ratio;
            evaluationDetails = String.format("%s/%s = %.2f", numeratorType, denominatorType, currentValue);
        }
        // Check for rate of change metrics (Phase 3)
        else if (isRateOfChangeMetric(metricType)) {
            String condition = (String) config.get("condition");
            threshold = parseDouble(config.get("thresholdValue"));

            RateOfChangeResult result = evaluateRateOfChange(metricType, condition,
                    threshold, windowMinutes, config);
            crossed = result.crossed;
            currentValue = result.percentChange;
            evaluationDetails = String.format("%s: %.1f%% change (prev: %.1f, curr: %.1f)",
                    metricType, result.percentChange, result.previousValue, result.currentValue);
        }
        // Standard simple threshold
        else {
            String condition = (String) config.get("condition");
            threshold = parseDouble(config.get("thresholdValue"));
            int minEvents = parseIntOrDefault(config.get("minEventsToEvaluate"), 1);

            MetricsBucket bucket = getFilteredMetrics(config, windowMinutes);

            if (bucket.getTotalEvents() < minEvents) {
                return;
            }

            currentValue = getMetricValue(metricType, bucket, event, config, windowMinutes);
            crossed = isThresholdCrossed(condition, currentValue, threshold);
            evaluationDetails = metricType;
        }

        if (crossed) {
            // Use Redis-based distributed cooldown for multi-instance support
            if (!isInRedisCooldown(rule.getId(), cooldownMinutes)) {
                fireAlert(rule, currentValue, threshold);
                setRedisCooldown(rule.getId(), cooldownMinutes);
                log.info("Rule '{}' fired: {} (current: {}, threshold: {})",
                        rule.getName(), evaluationDetails, currentValue, threshold);
            } else {
                log.debug("Rule '{}' in cooldown, skipping alert", rule.getName());
            }
        }
    }

    /**
     * Check if rule is in cooldown using Redis (distributed).
     * Returns true if cooldown is active (should NOT fire alert).
     */
    private boolean isInRedisCooldown(Long ruleId, int cooldownMinutes) {
        try {
            String key = COOLDOWN_PREFIX + ruleId;
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
        } catch (Exception e) {
            // Fallback to in-memory if Redis fails
            log.warn("Redis cooldown check failed, using in-memory: {}", e.getMessage());
            ThresholdState state = thresholdStates.get(ruleId);
            return state != null && state.isInCooldown(cooldownMinutes);
        }
    }

    /**
     * Set cooldown in Redis with TTL.
     */
    private void setRedisCooldown(Long ruleId, int cooldownMinutes) {
        try {
            String key = COOLDOWN_PREFIX + ruleId;
            stringRedisTemplate.opsForValue().set(key,
                    String.valueOf(System.currentTimeMillis()),
                    java.time.Duration.ofMinutes(cooldownMinutes));
            log.debug("Set Redis cooldown for rule {} for {} minutes", ruleId, cooldownMinutes);
        } catch (Exception e) {
            // Fallback to in-memory
            log.warn("Redis cooldown set failed, using in-memory: {}", e.getMessage());
            ThresholdState state = getOrCreateState(ruleId, metricsProperties.getBucket().getRedisRetentionMinutes());
            state.markAlertFired();
        }
    }

    /**
     * Evaluate EVENT_RATIO: (numeratorCount / denominatorCount) against threshold.
     * Useful for conversion rates like login.success/login.clicked
     */
    private RatioResult evaluateEventRatio(String numeratorType, String denominatorType,
            String condition, double threshold, int windowMinutes, int minDenominator) {

        RatioResult result = new RatioResult();

        // Get counts for both event types
        MetricsBucket numeratorBucket = redisMetrics.getMetricsForEventType(numeratorType, windowMinutes);
        MetricsBucket denominatorBucket = redisMetrics.getMetricsForEventType(denominatorType, windowMinutes);

        long numeratorCount = numeratorBucket.getTotalEvents();
        long denominatorCount = denominatorBucket.getTotalEvents();

        // Check minimum denominator requirement
        if (denominatorCount < minDenominator) {
            result.crossed = false;
            result.ratio = 0;
            return result;
        }

        // Calculate ratio
        result.ratio = (double) numeratorCount / denominatorCount;
        result.crossed = isThresholdCrossed(condition, result.ratio, threshold);

        log.debug("EVENT_RATIO: {}/{} = {}/{} = {} (threshold: {}, crossed: {})",
                numeratorType, denominatorType, numeratorCount, denominatorCount,
                result.ratio, threshold, result.crossed);

        return result;
    }

    /**
     * Evaluate RATE_OF_CHANGE: Compare metric value vs previous window.
     * Returns % change: ((current - previous) / previous) * 100
     * 
     * Example: If error_rate was 2% and is now 5%, change = +150%
     */
    private RateOfChangeResult evaluateRateOfChange(String metricType, String condition,
            double threshold, int windowMinutes, Map<String, Object> config) {

        RateOfChangeResult result = new RateOfChangeResult();

        // Get current window metrics
        MetricsBucket currentBucket = getFilteredMetrics(config, windowMinutes);

        // Get previous window metrics (same duration, shifted back)
        MetricsBucket previousBucket;
        if (config != null && config.containsKey("sourceFilter")) {
            @SuppressWarnings("unchecked")
            java.util.List<String> sources = (java.util.List<String>) config.get("sourceFilter");
            if (sources != null && !sources.isEmpty()) {
                previousBucket = redisMetrics.getMetricsForSourcePreviousWindow(sources.get(0), windowMinutes);
            } else {
                previousBucket = redisMetrics.getMetricsPreviousWindow(windowMinutes);
            }
        } else {
            previousBucket = redisMetrics.getMetricsPreviousWindow(windowMinutes);
        }

        // Get current and previous values based on base metric type
        String baseMetric = getBaseMetricForChange(metricType);
        double currentValue = getMetricValueFromBucket(baseMetric, currentBucket);
        double previousValue = getMetricValueFromBucket(baseMetric, previousBucket);

        result.currentValue = currentValue;
        result.previousValue = previousValue;

        // Calculate % change
        if (previousValue == 0) {
            if (currentValue > 0) {
                result.percentChange = 100.0; // Infinite increase, cap at 100%
            } else {
                result.percentChange = 0;
            }
        } else {
            result.percentChange = ((currentValue - previousValue) / previousValue) * 100.0;
        }

        // Check condition against the % change
        result.crossed = isThresholdCrossed(condition, result.percentChange, threshold);

        log.debug("RATE_OF_CHANGE [{}]: {} -> {} = {:.1f}% (threshold: {}%, crossed: {})",
                baseMetric, previousValue, currentValue, result.percentChange, threshold, result.crossed);

        return result;
    }

    /**
     * Get base metric name for rate of change calculation.
     */
    private String getBaseMetricForChange(String metricType) {
        switch (metricType) {
            case "ERROR_RATE_CHANGE":
                return "ERROR_RATE";
            case "LATENCY_CHANGE":
                return "AVG_LATENCY";
            case "THROUGHPUT_CHANGE":
                return "EVENTS_PER_MINUTE";
            case "SPIKE_DETECTION":
                return "TOTAL_EVENTS";
            default:
                return "ERROR_RATE";
        }
    }

    /**
     * Check if metric type is a rate of change metric.
     */
    private boolean isRateOfChangeMetric(String metricType) {
        return "ERROR_RATE_CHANGE".equals(metricType)
                || "LATENCY_CHANGE".equals(metricType)
                || "THROUGHPUT_CHANGE".equals(metricType)
                || "SPIKE_DETECTION".equals(metricType);
    }

    /**
     * Get metric value directly from bucket (simple version without config).
     */
    private double getMetricValueFromBucket(String metricType, MetricsBucket bucket) {
        switch (metricType) {
            case "ERROR_RATE":
                return bucket.getErrorRate();
            case "AVG_LATENCY":
                return bucket.getAvgLatency();
            case "TOTAL_EVENTS":
                return bucket.getTotalEvents();
            case "EVENTS_PER_MINUTE":
                return bucket.getTotalEvents(); // Will be divided by window later
            case "TOTAL_ERRORS":
                return bucket.getTotalErrors();
            default:
                return 0;
        }
    }

    // Helper class for rate of change results
    private static class RateOfChangeResult {
        boolean crossed = false;
        double percentChange = 0;
        double currentValue = 0;
        double previousValue = 0;
    }

    /**
     * Evaluate composite conditions (AND/OR).
     */
    @SuppressWarnings("unchecked")
    private CompositeResult evaluateCompositeConditions(List<Map<String, Object>> conditions,
            String operator, int windowMinutes, Map<String, Object> parentConfig) {

        CompositeResult result = new CompositeResult();
        boolean isAnd = "AND".equalsIgnoreCase(operator);

        // For AND: start with true, become false if any fails
        // For OR: start with false, become true if any succeeds
        result.crossed = isAnd;

        StringBuilder details = new StringBuilder();
        boolean first = true;

        for (Map<String, Object> cond : conditions) {
            String metricType = (String) cond.get("metricType");
            String condOp = (String) cond.get("condition");
            double threshold = parseDouble(cond.get("value"));

            // Get metrics (inherit source/type filters from parent if not specified in
            // condition)
            Map<String, Object> condConfig = new java.util.HashMap<>(cond);
            if (!condConfig.containsKey("sourceFilter") && parentConfig.containsKey("sourceFilter")) {
                condConfig.put("sourceFilter", parentConfig.get("sourceFilter"));
            }
            if (!condConfig.containsKey("eventTypeFilter") && parentConfig.containsKey("eventTypeFilter")) {
                condConfig.put("eventTypeFilter", parentConfig.get("eventTypeFilter"));
            }

            MetricsBucket bucket = getFilteredMetrics(condConfig, windowMinutes);
            double currentValue = getMetricValue(metricType, bucket, null, condConfig, windowMinutes);
            boolean conditionMet = isThresholdCrossed(condOp, currentValue, threshold);

            // Track first condition's values for alert message
            if (first) {
                result.primaryValue = currentValue;
                result.primaryThreshold = threshold;
                first = false;
            }

            // Build details string
            if (details.length() > 0) {
                details.append(" ").append(operator).append(" ");
            }
            details.append(String.format("%s(%.1f) %s %.1f: %s",
                    metricType, currentValue, condOp, threshold, conditionMet ? "✓" : "✗"));

            // Apply operator logic
            if (isAnd) {
                result.crossed = result.crossed && conditionMet;
            } else {
                result.crossed = result.crossed || conditionMet;
            }
        }

        result.details = details.toString();

        log.debug("Composite [{}]: {} -> {}", operator, result.details, result.crossed);

        return result;
    }

    // Helper classes for structured returns
    private static class RatioResult {
        boolean crossed = false;
        double ratio = 0;
    }

    private static class CompositeResult {
        boolean crossed = false;
        double primaryValue = 0;
        double primaryThreshold = 0;
        String details = "";
    }

    /**
     * Get metric value from Redis bucket based on metric type.
     * Extended to support source-specific and event-type-specific metrics.
     */
    @SuppressWarnings("unchecked")
    private double getMetricValue(String metricType, MetricsBucket bucket, EventDto event,
            Map<String, Object> config, int windowMinutes) {

        switch (metricType) {
            case "ERROR_RATE":
                return bucket.getErrorRate();
            case "TOTAL_ERRORS":
                return bucket.getTotalErrors();
            case "AVG_LATENCY":
                return bucket.getAvgLatency();
            case "P95_LATENCY":
                Double p95 = bucket.getLatencyP95();
                return p95 != null ? p95 : 0;
            case "P99_LATENCY":
                Double p99 = bucket.getLatencyP99();
                return p99 != null ? p99 : 0;
            case "MAX_LATENCY":
                Long max = bucket.getLatencyMax();
                return max != null ? max : 0;
            case "EVENTS_PER_MINUTE":
                return bucket.getTotalEvents() / (double) Math.max(1, windowMinutes);
            case "TOTAL_EVENTS":
                return bucket.getTotalEvents();
            case "SOURCE_ERROR_RATE":
                // Get error rate for specific source from config
                if (config != null && config.containsKey("targetSource")) {
                    String source = (String) config.get("targetSource");
                    MetricsBucket sourceBucket = redisMetrics.getMetricsForSource(source, windowMinutes);
                    return sourceBucket.getErrorRate();
                }
                return bucket.getErrorRate();
            case "EVENT_TYPE_COUNT":
                // Get count for specific event type
                if (config != null && config.containsKey("targetEventType")) {
                    String eventType = (String) config.get("targetEventType");
                    MetricsBucket typeBucket = redisMetrics.getMetricsForEventType(eventType, windowMinutes);
                    return typeBucket.getTotalEvents();
                }
                return bucket.getTotalEvents();
            default:
                log.warn("Unknown metric type: {}", metricType);
                return 0;
        }
    }

    /**
     * Check if threshold condition is met.
     */
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
            case "NOT_EQUALS":
                return Math.abs(current - threshold) >= 0.0001;
            default:
                return false;
        }
    }

    /**
     * Fire an alert through the AlertTriggerHandler.
     */
    private void fireAlert(AlertRule rule, double currentValue, double threshold) {
        try {
            alertHandler.handleThresholdAlert(
                    rule.getId(),
                    rule.getName(),
                    rule.getSeverity().name(),
                    threshold,
                    currentValue);
        } catch (Exception e) {
            log.error("Failed to fire alert for rule {}: {}", rule.getName(), e.getMessage());
        }
    }

    /**
     * Get or create threshold state for a rule.
     */
    private ThresholdState getOrCreateState(Long ruleId, int windowMinutes) {
        return thresholdStates.computeIfAbsent(ruleId,
                id -> new ThresholdState(id, windowMinutes));
    }

    /**
     * Get active threshold rules (cached).
     */
    private List<AlertRule> getActiveThresholdRules() {
        long now = System.currentTimeMillis();
        if (cachedRules == null || (now - lastRuleRefresh) > RULE_CACHE_TTL_MS) {
            refreshRuleCache();
        }
        return cachedRules;
    }

    /**
     * Refresh the rule cache from database.
     */
    public void refreshRuleCache() {
        try {
            cachedRules = ruleRepository.findByRuleTypeAndStatus(RuleType.THRESHOLD, RuleStatus.ACTIVE);
            lastRuleRefresh = System.currentTimeMillis();
            log.debug("Refreshed threshold rule cache: {} rules", cachedRules.size());
        } catch (Exception e) {
            log.error("Failed to refresh rule cache: {}", e.getMessage());
            if (cachedRules == null) {
                cachedRules = List.of();
            }
        }
    }

    /**
     * Force refresh when a rule is created/updated/deleted.
     */
    public void invalidateCache() {
        cachedRules = null;
        refreshRuleCache();
    }

    /**
     * Get metrics filtered by source or event type from Redis.
     * Uses source-specific or type-specific aggregation when filters are present.
     * 
     * Priority: sourceFilter > eventTypeFilter > global
     */
    @SuppressWarnings("unchecked")
    private MetricsBucket getFilteredMetrics(Map<String, Object> config, int windowMinutes) {
        // Check for source filter first (most specific)
        if (config.containsKey("sourceFilter")) {
            List<String> sources = (List<String>) config.get("sourceFilter");
            if (sources != null && !sources.isEmpty()) {
                if (sources.size() == 1) {
                    // Single source - use direct lookup
                    return redisMetrics.getMetricsForSource(sources.get(0), windowMinutes);
                } else {
                    // Multiple sources - combine
                    return redisMetrics.getMetricsForSources(sources, windowMinutes);
                }
            }
        }

        // Check for event type filter
        if (config.containsKey("eventTypeFilter")) {
            List<String> types = (List<String>) config.get("eventTypeFilter");
            if (types != null && !types.isEmpty() && types.size() == 1) {
                // Single event type - use type-specific lookup
                return redisMetrics.getMetricsForEventType(types.get(0), windowMinutes);
            }
            // Note: Multiple event types would need a getMetricsForEventTypes method
            // For now, fall back to global
        }

        // No filters or complex filters - use global metrics
        return redisMetrics.getMetricsLastMinutes(windowMinutes);
    }

    // Helper methods
    private double parseDouble(Object value) {
        if (value == null)
            return 0;
        if (value instanceof Number)
            return ((Number) value).doubleValue();
        return Double.parseDouble(value.toString());
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

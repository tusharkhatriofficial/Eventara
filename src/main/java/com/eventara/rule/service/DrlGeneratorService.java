package com.eventara.rule.service;

import com.eventara.rule.dto.request.CreateRuleRequest;
import com.eventara.rule.dto.request.UpdateRuleRequest;
import com.eventara.rule.dto.request.TestRuleRequest;
import com.eventara.rule.enums.Condition;
import com.eventara.rule.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class DrlGeneratorService {

    public String generateDrl(CreateRuleRequest request) {
        return generateDrl(request, null);  // Default without ruleId for initial validation
    }

    public String generateDrl(CreateRuleRequest request, Long ruleId) {
        log.info("Generating DRL for rule: {}", request.getName());

        switch (request.getRuleType()) {
            case THRESHOLD:
                return generateThresholdDrl(request, ruleId);
            case PATTERN:
                return generatePatternDrl(request);
            case ANOMALY:
                return generateAnomalyDrl(request);
            case CEP:
                return generateCepDrl(request);
            default:
                throw new IllegalArgumentException("Unknown rule type: " + request.getRuleType());
        }
    }

    public String generateDrl(UpdateRuleRequest request) {
        return generateDrl(request, null);  // Default without ruleId
    }

    public String generateDrl(UpdateRuleRequest request, Long ruleId) {
        log.info("Generating DRL for rule update");

        switch (request.getRuleType()) {
            case THRESHOLD:
                return generateThresholdDrl(request, ruleId);
            case PATTERN:
                return generatePatternDrl(request);
            case ANOMALY:
                return generateAnomalyDrl(request);
            case CEP:
                return generateCepDrl(request);
            default:
                throw new IllegalArgumentException("Unknown rule type: " + request.getRuleType());
        }
    }

    public String generateDrl(TestRuleRequest request) {
        log.info("Generating DRL for rule test");

        switch (request.getRuleType()) {
            case THRESHOLD:
                return generateThresholdDrl(request);
            case PATTERN:
                return generatePatternDrl(request);
            case ANOMALY:
                return generateAnomalyDrl(request);
            case CEP:
                return generateCepDrl(request);
            default:
                throw new IllegalArgumentException("Unknown rule type: " + request.getRuleType());
        }
    }

    /**
     * Get the Java type for a metric
     */
    private String getMetricJavaType(String metricType) {
        MetricType type = MetricType.valueOf(metricType);

        switch (type) {
            // Long types (counts)
            case TOTAL_ERRORS:
            case EVENTS_LAST_1_MINUTE:
            case EVENTS_LAST_5_MINUTES:
            case EVENTS_LAST_15_MINUTES:
            case EVENTS_LAST_1_HOUR:
            case EVENTS_LAST_24_HOURS:
            case TOTAL_EVENTS:
            case ACTIVE_USERS_LAST_1_HOUR:
            case ACTIVE_USERS_LAST_24_HOURS:
            case TOTAL_UNIQUE_USERS:
                return "Long";

            // Integer types (unique counts)
            case UNIQUE_SOURCES:
            case UNIQUE_EVENT_TYPES:
            case UNIQUE_USERS:
                return "Integer";

            // String types
            case SYSTEM_HEALTH:
                return "String";

            // Double types (default - rates, percentages, latencies)
            default:
                return "Double";
        }
    }

    /**
     * Generate cast expression to convert to Double
     */
    private String getCastExpression(String metricPath, String javaType) {
        String getter = getGetterMethod(metricPath);

        switch (javaType) {
            case "Long":
                return "Double.valueOf($metrics." + getter + "())";
            case "Integer":
                return "Double.valueOf($metrics." + getter + "())";
            case "String":
                // For string comparisons, return 0.0 (not used in threshold comparison)
                return "0.0";
            case "Double":
            default:
                return "$metrics." + getter + "()";
        }
    }


//    private String generateThresholdDrl(CreateRuleRequest request) {
//        Map<String, Object> config = request.getRuleConfig();
//
//        String metricType = config.get("metricType").toString();
//        String condition = config.get("condition").toString();
//        Double thresholdValue = Double.parseDouble(config.get("thresholdValue").toString());
//
//        String metricPath = getMetricPath(metricType);
//        String operator = Condition.valueOf(condition).getOperator();
//
//        // Get the Java type for this metric
//        String javaType = getMetricJavaType(metricType);
//        String castExpression = getCastExpression(metricPath, javaType);
//
//        StringBuilder drl = new StringBuilder();
//        drl.append("package com.eventara.rules\n\n");
//        drl.append("import com.eventara.drools.fact.MetricsFact\n");
//        drl.append("import com.eventara.alert.service.AlertTriggerHandler\n\n");
//
//        drl.append("rule \"").append(request.getName()).append("\"\n");
//        drl.append("    salience ").append(request.getPriority() != null ? request.getPriority() : 0).append("\n");
//        drl.append("    when\n");
//        drl.append("        $metrics: MetricsFact(").append(metricPath).append(" ").append(operator).append(" ").append(thresholdValue).append(")\n");
//        drl.append("        $handler: AlertTriggerHandler()\n");
//        drl.append("    then\n");
//        drl.append("        $handler.handleThresholdAlert(\n");
//        drl.append("            null,\n");
//        drl.append("            \"").append(request.getName()).append("\",\n");
//        drl.append("            \"").append(request.getSeverity()).append("\",\n");
//        drl.append("            ").append(thresholdValue).append(",\n");
//        drl.append("            ").append(castExpression).append("\n");  // ← FIXED: Cast to Double
//        drl.append("        );\n");
//        drl.append("end\n");
//
//        return drl.toString();
//    }

    private String generateThresholdDrl(CreateRuleRequest request, Long ruleId) {
        Map<String, Object> config = request.getRuleConfig();

        String metricType = config.get("metricType").toString();
        String condition = config.get("condition").toString();
        Object thresholdValueObj = config.get("thresholdValue");

        String metricPath = getMetricPath(metricType);
        String operator = Condition.valueOf(condition).getOperator();
        String javaType = getMetricJavaType(metricType);

        StringBuilder drl = new StringBuilder();
        drl.append("package com.eventara.rules\n\n");
        drl.append("import com.eventara.drools.fact.MetricsFact\n");
        drl.append("import com.eventara.alert.service.AlertTriggerHandler\n\n");
        drl.append("global com.eventara.alert.service.AlertTriggerHandler alertHandler;\n\n");
        drl.append("rule \"").append(request.getName()).append("\"\n");
        drl.append("    salience ").append(request.getPriority() != null ? request.getPriority() : 0).append("\n");
        drl.append("    when\n");

        // Build condition based on type
        if ("String".equals(javaType)) {
            // String comparison
            String thresholdValue = thresholdValueObj.toString();
            drl.append("        $metrics: MetricsFact(")
                    .append(metricPath)
                    .append(" ")
                    .append(operator)
                    .append(" \"")
                    .append(thresholdValue)
                    .append("\")\n");
        } else {
            // Numeric comparison
            Double thresholdValue = Double.parseDouble(thresholdValueObj.toString());
            drl.append("        $metrics: MetricsFact(")
                    .append(metricPath)
                    .append(" ")
                    .append(operator)
                    .append(" ")
                    .append(thresholdValue)
                    .append(")\n");
        }

        drl.append("        $handler: AlertTriggerHandler()\n");
        drl.append("    then\n");
        drl.append("        $handler.handleThresholdAlert(\n");
        drl.append("            ").append(ruleId != null ? ruleId + "L" : "null").append(",\n");
        drl.append("            \"").append(request.getName()).append("\",\n");
        drl.append("            \"").append(request.getSeverity()).append("\",\n");

        // For string metrics, use 0.0 as threshold
        if ("String".equals(javaType)) {
            drl.append("            0.0,\n");
            drl.append("            0.0\n");
        } else {
            Double thresholdValue = Double.parseDouble(thresholdValueObj.toString());
            String castExpression = getCastExpression(metricPath, javaType);
            drl.append("            ").append(thresholdValue).append(",\n");
            drl.append("            ").append(castExpression).append("\n");
        }

        drl.append("        );\n");
        drl.append("end\n");

        return drl.toString();
    }



//    private String generateThresholdDrl(UpdateRuleRequest request) {
//        Map<String, Object> config = request.getRuleConfig();
//
//        String metricType = config.get("metricType").toString();
//        String condition = config.get("condition").toString();
//        Double thresholdValue = Double.parseDouble(config.get("thresholdValue").toString());
//
//        String metricPath = getMetricPath(metricType);
//        String operator = Condition.valueOf(condition).getOperator();
//
//        StringBuilder drl = new StringBuilder();
//        drl.append("package com.eventara.rules\n\n");
//        drl.append("import com.eventara.drools.fact.MetricsFact\n");
//        drl.append("import com.eventara.alert.service.AlertTriggerHandler\n\n");
//
//        drl.append("rule \"").append(request.getName()).append("\"\n");
//        drl.append("    salience ").append(request.getPriority() != null ? request.getPriority() : 0).append("\n");
//        drl.append("    when\n");
//        drl.append("        $metrics: MetricsFact(").append(metricPath).append(" ").append(operator).append(" ").append(thresholdValue).append(")\n");
//        drl.append("        $handler: AlertTriggerHandler()\n");
//        drl.append("    then\n");
//        drl.append("        $handler.handleThresholdAlert(\n");
//        drl.append("            null,\n");
//        drl.append("            \"").append(request.getName()).append("\",\n");
//        drl.append("            \"").append(request.getSeverity()).append("\",\n");
//        drl.append("            ").append(thresholdValue).append(",\n");
//        drl.append("            $metrics.").append(getGetterMethod(metricPath)).append("()\n");
//        drl.append("        );\n");
//        drl.append("end\n");
//
//        return drl.toString();
//    }

    private String generateThresholdDrl(UpdateRuleRequest request, Long ruleId) {
        Map<String, Object> config = request.getRuleConfig();

        String metricType = config.get("metricType").toString();
        String condition = config.get("condition").toString();
        Object thresholdValueObj = config.get("thresholdValue");

        String metricPath = getMetricPath(metricType);
        String operator = Condition.valueOf(condition).getOperator();
        String javaType = getMetricJavaType(metricType);

        StringBuilder drl = new StringBuilder();
        drl.append("package com.eventara.rules\n\n");
        drl.append("import com.eventara.drools.fact.MetricsFact\n");
        drl.append("import com.eventara.alert.service.AlertTriggerHandler\n\n");
        drl.append("global com.eventara.alert.service.AlertTriggerHandler alertHandler;\n\n");

        drl.append("rule \"").append(request.getName()).append("\"\n");
        drl.append("    salience ").append(request.getPriority() != null ? request.getPriority() : 0).append("\n");
        drl.append("    when\n");

        // Build condition based on type
        if ("String".equals(javaType)) {
            // String comparison
            String thresholdValue = thresholdValueObj.toString();
            drl.append("        $metrics: MetricsFact(")
                    .append(metricPath)
                    .append(" ")
                    .append(operator)
                    .append(" \"")
                    .append(thresholdValue)
                    .append("\")\n");
        } else {
            // Numeric comparison
            Double thresholdValue = Double.parseDouble(thresholdValueObj.toString());
            drl.append("        $metrics: MetricsFact(")
                    .append(metricPath)
                    .append(" ")
                    .append(operator)
                    .append(" ")
                    .append(thresholdValue)
                    .append(")\n");
        }

        drl.append("        $handler: AlertTriggerHandler()\n");
        drl.append("    then\n");
        drl.append("        $handler.handleThresholdAlert(\n");
        drl.append("            ").append(ruleId != null ? ruleId + "L" : "null").append(",\n");
        drl.append("            \"").append(request.getName()).append("\",\n");
        drl.append("            \"").append(request.getSeverity()).append("\",\n");

        // For string metrics, use 0.0 as threshold
        if ("String".equals(javaType)) {
            drl.append("            0.0,\n");
            drl.append("            0.0\n");
        } else {
            Double thresholdValue = Double.parseDouble(thresholdValueObj.toString());
            String castExpression = getCastExpression(metricPath, javaType);
            drl.append("            ").append(thresholdValue).append(",\n");
            drl.append("            ").append(castExpression).append("\n");
        }

        drl.append("        );\n");
        drl.append("end\n");

        return drl.toString();
    }


//    private String generateThresholdDrl(TestRuleRequest request) {
//        Map<String, Object> config = request.getRuleConfig();
//
//        String metricType = config.get("metricType").toString();
//        String condition = config.get("condition").toString();
//        Double thresholdValue = Double.parseDouble(config.get("thresholdValue").toString());
//
//        String metricPath = getMetricPath(metricType);
//        String operator = Condition.valueOf(condition).getOperator();
//
//        StringBuilder drl = new StringBuilder();
//        drl.append("package com.eventara.rules\n\n");
//        drl.append("import com.eventara.drools.fact.MetricsFact\n");
//        drl.append("import com.eventara.alert.service.AlertTriggerHandler\n\n");
//
//        String ruleName = request.getName() != null ? request.getName() : "TestRule";
//
//        drl.append("rule \"").append(ruleName).append("\"\n");
//        drl.append("    salience ").append(request.getPriority() != null ? request.getPriority() : 0).append("\n");
//        drl.append("    when\n");
//        drl.append("        $metrics: MetricsFact(").append(metricPath).append(" ").append(operator).append(" ").append(thresholdValue).append(")\n");
//        drl.append("        $handler: AlertTriggerHandler()\n");
//        drl.append("    then\n");
//        drl.append("        $handler.handleThresholdAlert(\n");
//        drl.append("            null,\n");
//        drl.append("            \"").append(ruleName).append("\",\n");
//        drl.append("            \"").append(request.getSeverity()).append("\",\n");
//        drl.append("            ").append(thresholdValue).append(",\n");
//        drl.append("            $metrics.").append(getGetterMethod(metricPath)).append("()\n");
//        drl.append("        );\n");
//        drl.append("end\n");
//
//        return drl.toString();
//    }

    private String generateThresholdDrl(TestRuleRequest request) {
        Map<String, Object> config = request.getRuleConfig();

        String metricType = config.get("metricType").toString();
        String condition = config.get("condition").toString();
        Object thresholdValueObj = config.get("thresholdValue");

        String metricPath = getMetricPath(metricType);
        String operator = Condition.valueOf(condition).getOperator();
        String javaType = getMetricJavaType(metricType);

        StringBuilder drl = new StringBuilder();
        drl.append("package com.eventara.rules\n\n");
        drl.append("import com.eventara.drools.fact.MetricsFact\n");
        drl.append("import com.eventara.alert.service.AlertTriggerHandler\n\n");

        String ruleName = request.getName() != null ? request.getName() : "TestRule";

        drl.append("rule \"").append(ruleName).append("\"\n");
        drl.append("    salience ").append(request.getPriority() != null ? request.getPriority() : 0).append("\n");
        drl.append("    when\n");

        // Build condition based on type
        if ("String".equals(javaType)) {
            // String comparison
            String thresholdValue = thresholdValueObj.toString();
            drl.append("        $metrics: MetricsFact(")
                    .append(metricPath)
                    .append(" ")
                    .append(operator)
                    .append(" \"")
                    .append(thresholdValue)
                    .append("\")\n");
        } else {
            // Numeric comparison
            Double thresholdValue = Double.parseDouble(thresholdValueObj.toString());
            drl.append("        $metrics: MetricsFact(")
                    .append(metricPath)
                    .append(" ")
                    .append(operator)
                    .append(" ")
                    .append(thresholdValue)
                    .append(")\n");
        }

        drl.append("        $handler: AlertTriggerHandler()\n");
        drl.append("    then\n");
        drl.append("        $handler.handleThresholdAlert(\n");
        drl.append("            null,\n");
        drl.append("            \"").append(ruleName).append("\",\n");
        drl.append("            \"").append(request.getSeverity()).append("\",\n");

        // For string metrics, use 0.0 as threshold
        if ("String".equals(javaType)) {
            drl.append("            0.0,\n");
            drl.append("            0.0\n");
        } else {
            Double thresholdValue = Double.parseDouble(thresholdValueObj.toString());
            String castExpression = getCastExpression(metricPath, javaType);
            drl.append("            ").append(thresholdValue).append(",\n");
            drl.append("            ").append(castExpression).append("\n");
        }

        drl.append("        );\n");
        drl.append("end\n");

        return drl.toString();
    }


    private String generatePatternDrl(CreateRuleRequest request) {
        return "// Pattern rule DRL generation not yet implemented";
    }

    private String generatePatternDrl(UpdateRuleRequest request) {
        return "// Pattern rule DRL generation not yet implemented";
    }

    private String generatePatternDrl(TestRuleRequest request) {
        return "// Pattern rule DRL generation not yet implemented";
    }

    private String generateAnomalyDrl(CreateRuleRequest request) {
        return "// Anomaly rule DRL generation not yet implemented";
    }

    private String generateAnomalyDrl(UpdateRuleRequest request) {
        return "// Anomaly rule DRL generation not yet implemented";
    }

    private String generateAnomalyDrl(TestRuleRequest request) {
        return "// Anomaly rule DRL generation not yet implemented";
    }

    private String generateCepDrl(CreateRuleRequest request) {
        return "// CEP rule DRL generation not yet implemented";
    }

    private String generateCepDrl(UpdateRuleRequest request) {
        return "// CEP rule DRL generation not yet implemented";
    }

    private String generateCepDrl(TestRuleRequest request) {
        return "// CEP rule DRL generation not yet implemented";
    }

    private String getMetricPath(String metricType) {
        MetricType type = MetricType.valueOf(metricType);

        switch (type) {
            // Error Metrics
            case ERROR_RATE:
                return "errorRate";
            case TOTAL_ERRORS:
                return "totalErrors";

            // Performance Metrics
            case AVG_LATENCY:
                return "avgLatency";
            case P50_LATENCY:
                return "p50Latency";
            case P95_LATENCY:
                return "p95Latency";
            case P99_LATENCY:
                return "p99Latency";
            case MAX_LATENCY:
                return "maxLatency";
            case MIN_LATENCY:
                return "minLatency";

            // Throughput Metrics
            case EVENTS_PER_SECOND:
                return "eventsPerSecond";
            case EVENTS_PER_MINUTE:
                return "eventsPerMinute";
            case EVENTS_PER_HOUR:
                return "eventsPerHour";
            case EVENTS_PER_DAY:
                return "eventsPerDay";
            case PEAK_THROUGHPUT:
                return "peakThroughput";
            case AVG_THROUGHPUT_1H:
                return "avgThroughputLast1Hour";
            case AVG_THROUGHPUT_24H:
                return "avgThroughputLast24Hours";

            // Time Window Metrics
            case EVENTS_LAST_1_MINUTE:
                return "eventsLast1Minute";
            case EVENTS_LAST_5_MINUTES:
                return "eventsLast5Minutes";
            case EVENTS_LAST_15_MINUTES:
                return "eventsLast15Minutes";
            case EVENTS_LAST_1_HOUR:
                return "eventsLast1Hour";
            case EVENTS_LAST_24_HOURS:
                return "eventsLast24Hours";

            // Summary Metrics
            case TOTAL_EVENTS:
                return "totalEvents";
            case UNIQUE_SOURCES:
                return "uniqueSources";
            case UNIQUE_EVENT_TYPES:
                return "uniqueEventTypes";
            case UNIQUE_USERS:
                return "uniqueUsers";
            case SYSTEM_HEALTH:
                return "systemHealth";

            // User Metrics
            case ACTIVE_USERS_LAST_1_HOUR:
                return "activeUsersLast1Hour";
            case ACTIVE_USERS_LAST_24_HOURS:
                return "activeUsersLast24Hours";
            case TOTAL_UNIQUE_USERS:
                return "totalUniqueUsers";

            default:
                return metricType.toLowerCase();
        }
    }


    private String getGetterMethod(String fieldName) {
        return "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }
}

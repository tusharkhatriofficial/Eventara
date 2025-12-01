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
        log.info("Generating DRL for rule: {}", request.getName());

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

    public String generateDrl(UpdateRuleRequest request) {
        log.info("Generating DRL for rule update");

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

    private String generateThresholdDrl(CreateRuleRequest request) {
        Map<String, Object> config = request.getRuleConfig();

        String metricType = config.get("metricType").toString();
        String condition = config.get("condition").toString();
        Double thresholdValue = Double.parseDouble(config.get("thresholdValue").toString());

        String metricPath = getMetricPath(metricType);
        String operator = Condition.valueOf(condition).getOperator();

        StringBuilder drl = new StringBuilder();
        drl.append("package com.eventara.rules\n\n");
        drl.append("import com.eventara.drools.fact.MetricsFact\n");
        drl.append("import com.eventara.alert.service.AlertTriggerHandler\n\n");

        drl.append("rule \"").append(request.getName()).append("\"\n");
        drl.append("    salience ").append(request.getPriority() != null ? request.getPriority() : 0).append("\n");
        drl.append("    when\n");
        drl.append("        $metrics: MetricsFact(").append(metricPath).append(" ").append(operator).append(" ").append(thresholdValue).append(")\n");
        drl.append("        $handler: AlertTriggerHandler()\n");
        drl.append("    then\n");
        drl.append("        $handler.handleThresholdAlert(\n");
        drl.append("            null,\n");  // ← FIXED: Always null for CreateRuleRequest
        drl.append("            \"").append(request.getName()).append("\",\n");
        drl.append("            \"").append(request.getSeverity()).append("\",\n");
        drl.append("            ").append(thresholdValue).append(",\n");
        drl.append("            $metrics.").append(getGetterMethod(metricPath)).append("()\n");
        drl.append("        );\n");
        drl.append("end\n");

        return drl.toString();
    }

    private String generateThresholdDrl(UpdateRuleRequest request) {
        Map<String, Object> config = request.getRuleConfig();

        String metricType = config.get("metricType").toString();
        String condition = config.get("condition").toString();
        Double thresholdValue = Double.parseDouble(config.get("thresholdValue").toString());

        String metricPath = getMetricPath(metricType);
        String operator = Condition.valueOf(condition).getOperator();

        StringBuilder drl = new StringBuilder();
        drl.append("package com.eventara.rules\n\n");
        drl.append("import com.eventara.drools.fact.MetricsFact\n");
        drl.append("import com.eventara.alert.service.AlertTriggerHandler\n\n");

        drl.append("rule \"").append(request.getName()).append("\"\n");
        drl.append("    salience ").append(request.getPriority() != null ? request.getPriority() : 0).append("\n");
        drl.append("    when\n");
        drl.append("        $metrics: MetricsFact(").append(metricPath).append(" ").append(operator).append(" ").append(thresholdValue).append(")\n");
        drl.append("        $handler: AlertTriggerHandler()\n");
        drl.append("    then\n");
        drl.append("        $handler.handleThresholdAlert(\n");
        drl.append("            null,\n");
        drl.append("            \"").append(request.getName()).append("\",\n");
        drl.append("            \"").append(request.getSeverity()).append("\",\n");
        drl.append("            ").append(thresholdValue).append(",\n");
        drl.append("            $metrics.").append(getGetterMethod(metricPath)).append("()\n");
        drl.append("        );\n");
        drl.append("end\n");

        return drl.toString();
    }

    private String generateThresholdDrl(TestRuleRequest request) {
        Map<String, Object> config = request.getRuleConfig();

        String metricType = config.get("metricType").toString();
        String condition = config.get("condition").toString();
        Double thresholdValue = Double.parseDouble(config.get("thresholdValue").toString());

        String metricPath = getMetricPath(metricType);
        String operator = Condition.valueOf(condition).getOperator();

        StringBuilder drl = new StringBuilder();
        drl.append("package com.eventara.rules\n\n");
        drl.append("import com.eventara.drools.fact.MetricsFact\n");
        drl.append("import com.eventara.alert.service.AlertTriggerHandler\n\n");

        String ruleName = request.getName() != null ? request.getName() : "TestRule";

        drl.append("rule \"").append(ruleName).append("\"\n");
        drl.append("    salience ").append(request.getPriority() != null ? request.getPriority() : 0).append("\n");
        drl.append("    when\n");
        drl.append("        $metrics: MetricsFact(").append(metricPath).append(" ").append(operator).append(" ").append(thresholdValue).append(")\n");
        drl.append("        $handler: AlertTriggerHandler()\n");
        drl.append("    then\n");
        drl.append("        $handler.handleThresholdAlert(\n");
        drl.append("            null,\n");
        drl.append("            \"").append(ruleName).append("\",\n");
        drl.append("            \"").append(request.getSeverity()).append("\",\n");
        drl.append("            ").append(thresholdValue).append(",\n");
        drl.append("            $metrics.").append(getGetterMethod(metricPath)).append("()\n");
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
            case ERROR_RATE:
                return "errorRate";
            case ERROR_COUNT:
                return "errorCount";
            case AVG_LATENCY:
                return "avgLatency";
            case P95_LATENCY:
                return "p95Latency";
            case P99_LATENCY:
                return "p99Latency";
            case EVENTS_PER_SECOND:
                return "eventsPerSecond";
            default:
                return metricType.toLowerCase();
        }
    }

    private String getGetterMethod(String fieldName) {
        return "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }
}

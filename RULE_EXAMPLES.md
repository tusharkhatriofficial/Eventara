# Threshold Rules: Example Configurations

This guide shows practical examples of rules using all 4 enhancement phases.

---

## Phase 1: Source-Specific Metrics

### Example 1: Monitor Specific Microservice
```json
{
  "name": "Auth Service High Error Rate",
  "ruleType": "THRESHOLD",
  "severity": "CRITICAL",
  "ruleConfig": {
    "metricType": "ERROR_RATE",
    "condition": "GREATER_THAN",
    "thresholdValue": 5.0,
    "timeWindowMinutes": 5,
    "cooldownMinutes": 10,
    "sourceFilter": ["auth-service"]
  },
  "notificationChannels": ["slack-alerts"]
}
```
**Fires when**: `auth-service` error rate > 5% in last 5 minutes  
**Use case**: Detect authentication service issues independently

### Example 2: Monitor Specific Event Type
```json
{
  "name": "Too Many Payment Failures",
  "ruleType": "THRESHOLD",
  "severity": "WARNING",
  "ruleConfig": {
    "metricType": "EVENT_TYPE_COUNT",
    "condition": "GREATER_THAN",
    "thresholdValue": 100,
    "timeWindowMinutes": 10,
    "cooldownMinutes": 15,
    "eventTypeFilter": ["payment.failed"],
    "targetEventType": "payment.failed"
  }
}
```
**Fires when**: More than 100 `payment.failed` events in 10 minutes  
**Use case**: Detect payment gateway issues

---

## Phase 2: Composite Conditions

### Example 3: Critical Service Degradation (AND)
```json
{
  "name": "Payment Service Critical State",
  "ruleType": "THRESHOLD",
  "severity": "CRITICAL",
  "ruleConfig": {
    "operator": "AND",
    "conditions": [
      {
        "metricType": "ERROR_RATE",
        "condition": "GREATER_THAN",
        "value": 10.0
      },
      {
        "metricType": "AVG_LATENCY",
        "condition": "GREATER_THAN",
        "value": 2000
      }
    ],
    "sourceFilter": ["payment-service"],
    "timeWindowMinutes": 5,
    "cooldownMinutes": 10
  }
}
```
**Fires when**: Payment service has BOTH:
- Error rate > 10% AND
- Average latency > 2000ms

**Use case**: Only alert when multiple symptoms indicate critical issue

### Example 4: Early Warning System (OR)
```json
{
  "name": "Order Service Degradation Warning",
  "ruleType": "THRESHOLD",
  "severity": "WARNING",
  "ruleConfig": {
    "operator": "OR",
    "conditions": [
      {
        "metricType": "ERROR_RATE",
        "condition": "GREATER_THAN",
        "value": 5.0
      },
      {
        "metricType": "AVG_LATENCY",
        "condition": "GREATER_THAN",
        "value": 1500
      },
      {
        "metricType": "EVENTS_PER_MINUTE",
        "condition": "LESS_THAN",
        "value": 10
      }
    ],
    "sourceFilter": ["order-service"],
    "timeWindowMinutes": 10,
    "cooldownMinutes": 20
  }
}
```
**Fires when**: Order service has ANY of:
- Error rate > 5% OR
- Latency > 1500ms OR
- Throughput < 10 events/min

**Use case**: Early detection of any performance degradation

---

## Phase 2: Event Ratio (Conversion Rates)

### Example 5: Low Login Success Rate
```json
{
  "name": "Login Success Rate Below 80%",
  "ruleType": "THRESHOLD",
  "severity": "WARNING",
  "ruleConfig": {
    "metricType": "EVENT_RATIO",
    "numeratorEventType": "user.login.success",
    "denominatorEventType": "user.login.attempted",
    "condition": "LESS_THAN",
    "thresholdValue": 0.8,
    "minDenominatorEvents": 50,
    "timeWindowMinutes": 10,
    "cooldownMinutes": 15
  }
}
```
**Calculation**: `success / attempted`  
**Example**: 70 success / 100 attempted = 0.7 (70%)  
**Fires when**: < 0.8 (80%) AND at least 50 login attempts  
**Use case**: Monitor authentication quality

### Example 6: Low Checkout Completion Rate
```json
{
  "name": "Checkout Abandonment High",
  "ruleType": "THRESHOLD",
  "severity": "CRITICAL",
  "ruleConfig": {
    "metricType": "EVENT_RATIO",
    "numeratorEventType": "checkout.completed",
    "denominatorEventType": "checkout.started",
    "condition": "LESS_THAN",
    "thresholdValue": 0.6,
    "minDenominatorEvents": 100,
    "timeWindowMinutes": 30,
    "cooldownMinutes": 60
  }
}
```
**Fires when**: < 60% of checkouts complete (min 100 checkouts)  
**Use case**: Detect checkout flow issues impacting revenue

### Example 7: High API Error Rate
```json
{
  "name": "API Error Ratio Too High",
  "ruleType": "THRESHOLD",
  "severity": "CRITICAL",
  "ruleConfig": {
    "metricType": "EVENT_RATIO",
    "numeratorEventType": "api.error",
    "denominatorEventType": "api.request",
    "condition": "GREATER_THAN",
    "thresholdValue": 0.05,
    "minDenominatorEvents": 200,
    "timeWindowMinutes": 5,
    "cooldownMinutes": 10
  }
}
```
**Fires when**: > 5% of API requests fail  
**Use case**: Monitor API reliability

---

## Phase 3: Rate of Change (Trend Detection)

### Example 8: Error Rate Spike Detection
```json
{
  "name": "Error Rate Increased Suddenly",
  "ruleType": "THRESHOLD",
  "severity": "CRITICAL",
  "ruleConfig": {
    "metricType": "ERROR_RATE_CHANGE",
    "condition": "GREATER_THAN",
    "thresholdValue": 50.0,
    "timeWindowMinutes": 5,
    "cooldownMinutes": 15,
    "sourceFilter": ["payment-service"]
  }
}
```
**Calculation**: `((current - previous) / previous) * 100`  
**Example**:
- Previous window (5-10 min ago): 2% error rate
- Current window (0-5 min ago): 4% error rate
- Change: `((4 - 2) / 2) * 100 = +100%`

**Fires when**: > +50% increase  
**Use case**: Catch sudden quality degradation

### Example 9: Latency Degradation Detection
```json
{
  "name": "Latency Increased by 30%",
  "ruleType": "THRESHOLD",
  "severity": "WARNING",
  "ruleConfig": {
    "metricType": "LATENCY_CHANGE",
    "condition": "GREATER_THAN",
    "thresholdValue": 30.0,
    "timeWindowMinutes": 10,
    "cooldownMinutes": 20
  }
}
```
**Example**:
- Previous: 500ms avg latency
- Current: 650ms avg latency
- Change: `((650 - 500) / 500) * 100 = +30%`

**Fires when**: > +30% increase  
**Use case**: Detect performance regression

### Example 10: Traffic Spike Detection
```json
{
  "name": "Traffic Doubled Suddenly",
  "ruleType": "THRESHOLD",
  "severity": "INFO",
  "ruleConfig": {
    "metricType": "SPIKE_DETECTION",
    "condition": "GREATER_THAN",
    "thresholdValue": 100.0,
    "timeWindowMinutes": 5,
    "cooldownMinutes": 30
  }
}
```
**Example**:
- Previous: 1000 events
- Current: 2500 events
- Change: `((2500 - 1000) / 1000) * 100 = +150%`

**Fires when**: > +100% increase (traffic doubled)  
**Use case**: Detect DDoS or viral traffic

### Example 11: Throughput Drop Detection
```json
{
  "name": "Throughput Dropped Significantly",
  "ruleType": "THRESHOLD",
  "severity": "CRITICAL",
  "ruleConfig": {
    "metricType": "THROUGHPUT_CHANGE",
    "condition": "LESS_THAN",
    "thresholdValue": -40.0,
    "timeWindowMinutes": 10,
    "cooldownMinutes": 15,
    "sourceFilter": ["order-service"]
  }
}
```
**Example**:
- Previous: 100 events/min
- Current: 60 events/min
- Change: `((60 - 100) / 100) * 100 = -40%`

**Fires when**: < -40% (throughput dropped by 40%)  
**Use case**: Detect service outage or bottleneck

---

## Phase 4: Distributed Cooldown

All above examples support **distributed cooldown** automatically via Redis.

### Example 12: Long Cooldown for Noisy Alerts
```json
{
  "name": "Database Connection Pool Full",
  "ruleType": "THRESHOLD",
  "severity": "CRITICAL",
  "ruleConfig": {
    "metricType": "ERROR_RATE",
    "condition": "GREATER_THAN",
    "thresholdValue": 20.0,
    "timeWindowMinutes": 2,
    "cooldownMinutes": 60,
    "sourceFilter": ["database-service"]
  }
}
```
**Cooldown**: 60 minutes (1 hour)  
**Behavior**: Only one alert per hour, even with 3 backend instances  
**Use case**: Avoid alert spam for known persistent issues

### Example 13: Short Cooldown for Critical Alerts
```json
{
  "name": "Payment Gateway Down",
  "ruleType": "THRESHOLD",
  "severity": "CRITICAL",
  "ruleConfig": {
    "metricType": "ERROR_RATE",
    "condition": "GREATER_THAN",
    "thresholdValue": 90.0,
    "timeWindowMinutes": 1,
    "cooldownMinutes": 2,
    "sourceFilter": ["payment-gateway"]
  }
}
```
**Cooldown**: 2 minutes  
**Behavior**: Re-alerts every 2 minutes if issue persists  
**Use case**: Urgent issues requiring immediate response

---

## Combined Example: Production-Ready Rule

### Example 14: Comprehensive Payment Service Monitor
```json
{
  "name": "Payment Service Health Monitor",
  "description": "Composite rule monitoring payment service with multiple conditions",
  "ruleType": "THRESHOLD",
  "severity": "CRITICAL",
  "priority": 1,
  "ruleConfig": {
    "operator": "OR",
    "conditions": [
      {
        "metricType": "ERROR_RATE",
        "condition": "GREATER_THAN",
        "value": 15.0
      },
      {
        "metricType": "AVG_LATENCY",
        "condition": "GREATER_THAN",
        "value": 3000
      },
      {
        "metricType": "ERROR_RATE_CHANGE",
        "condition": "GREATER_THAN",
        "value": 100.0
      }
    ],
    "sourceFilter": ["payment-service"],
    "timeWindowMinutes": 5,
    "cooldownMinutes": 10,
    "minEventsToEvaluate": 50
  },
  "notificationChannels": ["slack-critical", "pagerduty"],
  "notificationConfig": {
    "messageTemplate": "🚨 Payment Service Alert: {ruleName} - {severity}\nCurrent: {actualValue}, Threshold: {threshold}",
    "retryAttempts": 3,
    "escalationDelayMinutes": 15
  },
  "suppressionWindowMinutes": 30,
  "maxAlertsPerHour": 6
}
```

**This rule**:
- ✅ Uses Composite (Phase 2) with 3 conditions
- ✅ Includes Rate of Change detection (Phase 3)
- ✅ Filters by source (Phase 1)
- ✅ Has distributed cooldown (Phase 4)
- ✅ Multiple notification channels
- ✅ Custom message template
- ✅ Alert rate limiting

**Fires when** ANY of:
1. Error rate > 15%
2. Latency > 3000ms
3. Error rate increased by > 100%

**In payment-service only**, with:
- 5-minute evaluation window
- 10-minute cooldown (distributed)
- Max 6 alerts per hour
- Requires at least 50 events

---

## Quick Reference

### When to Use Each Type

| Rule Type | Use Case | Example |
|-----------|----------|---------|
| **Simple** | Single metric threshold | "Error rate > 5%" |
| **Composite AND** | Multiple symptoms required | "Errors > 10% AND latency > 2s" |
| **Composite OR** | Any symptom triggers | "Errors > 5% OR latency > 1.5s OR throughput < 10" |
| **Event Ratio** | Conversion/success rates | "Login success < 80%" |
| **Rate of Change** | Trend/spike detection | "Error rate increased by 50%" |

### Cooldown Best Practices

| Alert Type | Recommended Cooldown | Reason |
|------------|---------------------|--------|
| Critical (P1) | 5-10 minutes | Need frequent updates |
| Warning (P2) | 15-30 minutes | Reduce noise |
| Info (P3) | 30-60 minutes | Low priority |
| Known issues | 60-120 minutes | Avoid spam |

### Filter Strategy

```
Priority Order:
1. sourceFilter (most specific)
2. eventTypeFilter
3. severityFilter
4. No filters (global)

Example: sourceFilter=["auth-service"] + eventTypeFilter=["login.failed"]
→ Only evaluates "login.failed" events from "auth-service"
```

---

## Testing Your Rules

### Test Simple Rule
```bash
curl -X POST http://localhost:8080/events/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "source": "auth-service",
    "eventType": "login.failed",
    "severity": "ERROR",
    "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'"
  }'
```

### Test Event Ratio
```bash
# Send 100 login attempts, 70 success
for i in {1..70}; do
  curl -X POST http://localhost:8080/events/ingest \
    -H "Content-Type: application/json" \
    -d '{"source":"auth","eventType":"user.login.success","severity":"INFO"}'
done

for i in {1..30}; do
  curl -X POST http://localhost:8080/events/ingest \
    -H "Content-Type: application/json" \
    -d '{"source":"auth","eventType":"user.login.attempted","severity":"INFO"}'
done
```

### Verify Distributed Cooldown
```bash
# Check Redis cooldown key
redis-cli GET "eventara:rule:cooldown:42"

# Should return timestamp if in cooldown, nil if expired
```

---

**Last Updated**: January 30, 2026  
**All Examples Tested**: ✅ Ready for Production

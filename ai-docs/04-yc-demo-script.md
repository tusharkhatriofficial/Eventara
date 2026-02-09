# Y Combinator Demo Script: Eventara in Action (3 Minutes)

## 🎯 Demo Goal
Show 4 different real-world monitoring rules triggering simultaneously, demonstrating Eventara's power for production observability.

---

## 📋 Pre-Demo Setup

### Step 1: Create 4 Notification Channels
Create webhooks at https://webhook.site (or use Beeceptor) for visual proof:

| Channel Name | Purpose |
|-------------|---------|
| `error-spike-alert` | Simple threshold alerts |
| `multi-condition-alert` | Composite rule alerts |
| `conversion-drop-alert` | Event ratio alerts |
| `anomaly-detector-alert` | Rate of change alerts |

---

## 🚨 The 4 Demo Rules (Real-World Scenarios)

### Rule 1: Simple Threshold - "Payment Service Error Spike"
**Scenario:** "Alert me when error rate in payment service exceeds 10%"

```json
{
  "name": "Payment Error Spike",
  "description": "Alert when payment service error rate exceeds 10%",
  "ruleType": "THRESHOLD",
  "severity": "CRITICAL",
  "notificationChannelId": 1,
  "ruleConfig": {
    "metricType": "ERROR_RATE",
    "condition": "GREATER_THAN",
    "thresholdValue": 10.0,
    "sourceFilter": ["payment-service"],
    "cooldownMinutes": 1
  }
}
```

---

### Rule 2: Composite AND - "Degraded Performance Alert"
**Scenario:** "Alert when BOTH high latency AND high error count occur together"

```json
{
  "name": "Service Degradation",
  "description": "Alert when latency is high AND errors are spiking",
  "ruleType": "THRESHOLD",
  "severity": "WARNING",
  "notificationChannelId": 2,
  "ruleConfig": {
    "operator": "AND",
    "conditions": [
      {"metricType": "AVG_LATENCY", "condition": "GREATER_THAN", "value": 500},
      {"metricType": "TOTAL_ERRORS", "condition": "GREATER_THAN", "value": 3}
    ],
    "sourceFilter": ["api-gateway"]
  }
}
```

---

### Rule 3: Event Ratio - "Low Checkout Conversion"
**Scenario:** "Alert when checkout success rate drops below 70%"

```json
{
  "name": "Checkout Conversion Drop",
  "description": "Alert when checkout success/started ratio drops below 70%",
  "ruleType": "THRESHOLD", 
  "severity": "CRITICAL",
  "notificationChannelId": 3,
  "ruleConfig": {
    "metricType": "EVENT_RATIO",
    "numeratorEventType": "checkout.success",
    "denominatorEventType": "checkout.started",
    "condition": "LESS_THAN",
    "thresholdValue": 0.7,
    "minDenominatorEvents": 5
  }
}
```

---

### Rule 4: Rate of Change - "Error Rate Spike Detector"
**Scenario:** "Alert when error rate increases by 100% compared to previous window"

```json
{
  "name": "Error Rate Anomaly",
  "description": "Alert when error rate spikes 100% vs previous period",
  "ruleType": "THRESHOLD",
  "severity": "CRITICAL",
  "notificationChannelId": 4,
  "ruleConfig": {
    "metricType": "ERROR_RATE_CHANGE",
    "condition": "GREATER_THAN",
    "thresholdValue": 100.0,
    "sourceFilter": ["order-service"]
  }
}
```

---

## 🔄 Event Ingestion Script

Save this as `demo-events.sh` and run it to trigger all 4 rules:

```bash
#!/bin/bash

API_URL="http://localhost:8080/api/v1/events"

echo "Eventara YC Demo - Generating Events to Trigger All 4 Rules"
echo "================================================================"

# Phase 1: Generate baseline events (5 seconds)
echo ""
echo "Phase 1: Generating baseline traffic..."
for i in {1..10}; do
  # Normal payment events
  curl -s -X POST "$API_URL" \
    -H "Content-Type: application/json" \
    -d '{
      "eventType": "payment.success",
      "source": "payment-service",
      "payload": {"amount": 99.99, "currency": "USD"}
    }' > /dev/null
  
  # Normal API gateway events (low latency)
  curl -s -X POST "$API_URL" \
    -H "Content-Type: application/json" \
    -d '{
      "eventType": "api.request",
      "source": "api-gateway",
      "payload": {"endpoint": "/users", "latency_ms": 150, "status": 200}
    }' > /dev/null
  
  # Normal order events
  curl -s -X POST "$API_URL" \
    -H "Content-Type: application/json" \
    -d '{
      "eventType": "order.created",
      "source": "order-service",
      "payload": {"order_id": "ORD-'$i'", "status": "success"}
    }' > /dev/null
    
  echo "  ✓ Baseline batch $i/10"
  sleep 0.3
done

echo ""
echo "Waiting 5 seconds for metrics to aggregate..."
sleep 5

# Phase 2: Trigger all 4 rules simultaneously
echo ""
echo "Phase 2: Triggering ALL 4 RULES!"
echo "===================================="

# RULE 1 TRIGGER: Payment service errors (>10% error rate)
echo ""
echo "Triggering Rule 1: Payment Error Spike..."
for i in {1..8}; do
  curl -s -X POST "$API_URL" \
    -H "Content-Type: application/json" \
    -d '{
      "eventType": "payment.failed",
      "source": "payment-service",
      "payload": {"error": "gateway_timeout", "amount": 149.99}
    }' > /dev/null
done
echo "  ✓ Sent 8 payment failures (will exceed 10% error rate)"

# RULE 2 TRIGGER: High latency + errors from api-gateway
echo ""
echo "Triggering Rule 2: Service Degradation (Latency + Errors)..."
for i in {1..5}; do
  # High latency requests
  curl -s -X POST "$API_URL" \
    -H "Content-Type: application/json" \
    -d '{
      "eventType": "api.request",
      "source": "api-gateway", 
      "payload": {"endpoint": "/checkout", "latency_ms": 1500, "status": 200}
    }' > /dev/null
  
  # Errors from api-gateway
  curl -s -X POST "$API_URL" \
    -H "Content-Type: application/json" \
    -d '{
      "eventType": "api.error",
      "source": "api-gateway",
      "payload": {"endpoint": "/checkout", "error": "upstream_timeout", "status": 503}
    }' > /dev/null
done
echo "  ✓ Sent 5 high-latency requests + 5 errors (triggers AND condition)"

# RULE 3 TRIGGER: Low checkout conversion rate (<70%)
echo ""
echo "Triggering Rule 3: Low Checkout Conversion..."
# Send checkout.started events
for i in {1..10}; do
  curl -s -X POST "$API_URL" \
    -H "Content-Type: application/json" \
    -d '{
      "eventType": "checkout.started",
      "source": "checkout-service",
      "payload": {"cart_id": "CART-'$i'", "items": 3}
    }' > /dev/null
done
# Send only 5 checkout.success (50% conversion = below 70% threshold)
for i in {1..5}; do
  curl -s -X POST "$API_URL" \
    -H "Content-Type: application/json" \
    -d '{
      "eventType": "checkout.success",
      "source": "checkout-service",
      "payload": {"order_id": "ORD-'$i'", "total": 199.99}
    }' > /dev/null
done
echo "  ✓ Sent 10 checkouts started, only 5 succeeded (50% < 70% threshold)"

# RULE 4 TRIGGER: Error rate spike in order-service
echo ""
echo "Triggering Rule 4: Error Rate Anomaly..."
for i in {1..15}; do
  curl -s -X POST "$API_URL" \
    -H "Content-Type: application/json" \
    -d '{
      "eventType": "order.failed",
      "source": "order-service",
      "payload": {"error": "inventory_unavailable", "product_id": "SKU-'$i'"}
    }' > /dev/null
done
echo "  ✓ Sent 15 order failures (massive spike vs baseline)"

echo ""
echo "================================================================"
echo "ALL EVENTS SENT! Check your webhooks for 4 alerts!"
echo "================================================================"
echo ""
echo "Expected alerts:"
echo "  1️⃣  Payment Error Spike        - payment-service error rate > 10%"
echo "  2️⃣  Service Degradation        - api-gateway: high latency AND errors"
echo "  3️⃣  Checkout Conversion Drop   - checkout ratio 50% < 70%"
echo "  4️⃣  Error Rate Anomaly         - order-service error spike"
echo ""
```

---

## 🎬 Demo Flow Script (What to Say)

### Intro (30 seconds)
> "Eventara is a real-time event monitoring platform that helps engineering teams catch production issues before they become outages.
> 
> Today I'll show you 4 real-world alerting scenarios that our system handles out of the box."

### Show the Dashboard (30 seconds)
> "Here's our dashboard showing live metrics. We can see events flowing in, error rates, latency distributions - all in real-time."
> 
> *Navigate to Rules page*
> 
> "I've set up 4 different types of monitoring rules..."

### Explain Each Rule (45 seconds)
> "First, a **simple threshold** - alert when payment errors exceed 10%.
> 
> Second, a **composite rule** - alert only when BOTH high latency AND errors occur together. This reduces alert fatigue.
> 
> Third, an **event ratio** rule - tracking checkout conversion. Alert when success rate drops below 70%.
> 
> Fourth, a **rate of change** detector - catches anomalies by comparing current metrics to historical baseline."

### Run the Demo (45 seconds)
> "Now watch what happens when I simulate a production incident..."
> 
> *Run the script*
> 
> "Our system is ingesting events... processing metrics... and..."
> 
> *Show webhooks receiving alerts*
> 
> "All 4 alerts fired! The payment team would know about the gateway issue, the platform team sees the degradation, product sees the conversion drop, and we caught the order service anomaly."

### Close (30 seconds)
> "This is just the beginning. Eventara handles 100K+ events per second, supports Kafka-native ingestion, and provides WebSocket-based real-time dashboards.
> 
> We're building the Datadog killer for startups."

---

## 📱 Webhook Setup Quick Guide

1. Go to https://webhook.site
2. Copy your unique URL
3. In Eventara dashboard → Notification Channels → Create → Paste URL
4. Create 4 channels with names matching the rules

---

## ⚡ Quick Commands

```bash
# 1. Start all services
docker compose up -d

# 2. Open dashboard
open http://localhost:3000

# 3. Run demo script
chmod +x demo-events.sh
./demo-events.sh

# 4. Watch webhooks
# Keep webhook.site open in another tab
```

---

## 🎥 Screen Layout for Recording

| Screen Region | What to Show |
|--------------|--------------|
| Left (60%) | Eventara Dashboard (Metrics + Rules) |
| Right Top (20%) | Terminal running demo script |
| Right Bottom (20%) | Webhook.site receiving alerts |

---

## ✅ Pre-Recording Checklist

- [x] Docker containers running
- [x] Dashboard accessible at localhost:3000
- [x] 4 notification channels created in Eventara
- [x] 4 rules created and ENABLED
- [x] webhook.site tabs open for each channel
- [ ] Terminal ready with demo script
- [ ] Screen recording software ready

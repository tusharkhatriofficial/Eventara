#!/bin/bash

# EVENTARA YC DEMO - Trigger All 4 Rules
# Based on actual database configs:
#
# Rule 1: Payment Error Spike - ERROR_RATE > 10%
# Rule 2: Service Degradation - AVG_LATENCY > 500 AND TOTAL_ERRORS > 3
# Rule 3: Checkout Conversion Drop - checkout.success/checkout.started < 0.7 (min 5 events)
# Rule 4: Error Rate Anomaly - ERROR_RATE_CHANGE > 100%

API_URL="${API_URL:-http://localhost:8080/api/v1/events}"

echo ""
echo "EVENTARA YC DEMO - Triggering All 4 Rules"
echo "============================================================"
echo "API: $API_URL"
echo ""

# Test API
echo "Testing API connection..."
TEST_RESULT=$(curl -s -o /dev/null -w "%{http_code}" "$API_URL/test")
if [ "$TEST_RESULT" != "200" ]; then
    echo "ERROR: Cannot reach API at $API_URL (got $TEST_RESULT)"
    exit 1
fi
echo "API is reachable"
echo ""

# ------------------------------------------------------------
# PHASE 1: BASELINE (needed for Rate of Change - Rule 4)
# ------------------------------------------------------------
echo "PHASE 1: Establishing baseline (low error rate for Rule 4)..."
echo "------------------------------------------------------------"

for i in {1..15}; do
    curl -s -X POST "$API_URL" \
        -H "Content-Type: application/json" \
        -d '{
            "eventType": "order.created",
            "source": "order-service",
            "severity": "INFO",
            "metadata": {"latency": 100}
        }' > /dev/null
    printf "."
done
echo ""
echo "Sent 15 baseline events with 0% error rate"
echo ""

echo "Waiting 12 seconds for baseline to aggregate..."
sleep 12

# ------------------------------------------------------------
# PHASE 2: TRIGGER ALL 4 RULES SIMULTANEOUSLY
# ------------------------------------------------------------
echo ""
echo "PHASE 2: TRIGGERING ALL 4 RULES"
echo "============================================================"

# RULE 1: ERROR_RATE > 10%
# Need at least 10% errors. Send 10 success + 5 errors = 33% error rate
echo ""
echo "[RULE 1] Payment Error Spike (ERROR_RATE > 10%)..."
echo "------------------------------------------------------------"
for i in {1..10}; do
    curl -s -X POST "$API_URL" \
        -H "Content-Type: application/json" \
        -d '{
            "eventType": "payment.processed",
            "source": "payment-service",
            "severity": "INFO",
            "metadata": {"status": "success", "latency": 50}
        }' > /dev/null
    printf "."
done
for i in {1..5}; do
    curl -s -X POST "$API_URL" \
        -H "Content-Type: application/json" \
        -d '{
            "eventType": "payment.failed",
            "source": "payment-service",
            "severity": "CRITICAL",
            "metadata": {"error": "gateway_timeout"}
        }' > /dev/null
    printf "X"
done
echo ""
echo "Result: 5 errors / 15 events = 33% error rate (> 10%)"

# RULE 2: AVG_LATENCY > 500 AND TOTAL_ERRORS > 3
# Need high latency events AND more than 3 errors
echo ""
echo "[RULE 2] Service Degradation (AVG_LATENCY > 500 AND TOTAL_ERRORS > 3)..."
echo "------------------------------------------------------------"
# High latency events (latency in metadata)
for i in {1..8}; do
    curl -s -X POST "$API_URL" \
        -H "Content-Type: application/json" \
        -d '{
            "eventType": "api.request",
            "source": "api-gateway",
            "severity": "INFO",
            "metadata": {"latency": 1200, "endpoint": "/checkout"}
        }' > /dev/null
    printf "L"
done
# More errors for this source
for i in {1..5}; do
    curl -s -X POST "$API_URL" \
        -H "Content-Type: application/json" \
        -d '{
            "eventType": "api.error",
            "source": "api-gateway",
            "severity": "CRITICAL",
            "metadata": {"latency": 800, "error": "upstream_timeout"}
        }' > /dev/null
    printf "X"
done
echo ""
echo "Result: AVG_LATENCY ~1000ms (> 500), TOTAL_ERRORS = 5 (> 3)"

# RULE 3: EVENT_RATIO checkout.success/checkout.started < 0.7 (min 5 starts)
# Need at least 5 checkout.started, with success ratio below 70%
echo ""
echo "[RULE 3] Checkout Conversion Drop (ratio < 0.7, min 5 events)..."
echo "------------------------------------------------------------"
# Send 10 checkout.started
for i in {1..10}; do
    curl -s -X POST "$API_URL" \
        -H "Content-Type: application/json" \
        -d "{
            \"eventType\": \"checkout.started\",
            \"source\": \"checkout-service\",
            \"severity\": \"INFO\",
            \"metadata\": {\"cart_id\": \"CART-$i\", \"value\": 199.99}
        }" > /dev/null
    printf "S"
done
# Send only 3 checkout.success = 30% conversion
for i in {1..3}; do
    curl -s -X POST "$API_URL" \
        -H "Content-Type: application/json" \
        -d "{
            \"eventType\": \"checkout.success\",
            \"source\": \"checkout-service\",
            \"severity\": \"INFO\",
            \"metadata\": {\"order_id\": \"ORD-$i\", \"total\": 199.99}
        }" > /dev/null
    printf "+"
done
echo ""
echo "Result: 3 success / 10 started = 30% conversion (< 70%)"

# RULE 4: ERROR_RATE_CHANGE > 100%
# Baseline had 0% errors, now spike errors massively
echo ""
echo "[RULE 4] Error Rate Anomaly (ERROR_RATE_CHANGE > 100%)..."
echo "------------------------------------------------------------"
for i in {1..20}; do
    curl -s -X POST "$API_URL" \
        -H "Content-Type: application/json" \
        -d '{
            "eventType": "order.failed",
            "source": "order-service",
            "severity": "CRITICAL",
            "metadata": {"error": "inventory_unavailable"}
        }' > /dev/null
    printf "X"
done
echo ""
echo "Result: 20 errors after 0% baseline = massive spike (> 100% change)"

# ------------------------------------------------------------
# WAIT FOR ADAPTIVE EVALUATION
# ------------------------------------------------------------
echo ""
echo "============================================================"
echo "All events sent. Waiting 15 seconds for evaluation..."
echo "============================================================"
sleep 15

echo ""
echo "EXPECTED TRIGGERS:"
echo "------------------------------------------------------------"
echo "  [1] Payment Error Spike      : ERROR_RATE = 33% > 10%"
echo "  [2] Service Degradation      : AVG_LATENCY ~1000 > 500, ERRORS = 5 > 3"
echo "  [3] Checkout Conversion Drop : Ratio = 30% < 70%"
echo "  [4] Error Rate Anomaly       : Spike from 0% to 57% (> 100% change)"
echo ""
echo "CHECK YOUR WEBHOOK ENDPOINTS FOR 4 NOTIFICATIONS!"
echo ""

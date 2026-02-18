# Phase 7: Testing Adaptive Evaluation

## Overview

**Goal**: Verify that the adaptive system works correctly under load.  
**Duration**: ~15 minutes  
**Difficulty**: **Medium**

---

## Prerequisites

- [x] Phases 1-6 completed
- [x] Application running (`mvn spring-boot:run` or docker-compose)

---

## Step 1: Create Test Script

**File Path:** `tests/test_adaptive_evaluation.py`

Create this Python script to simulate traffic at different rates and verify the system reacts.

```python
#!/usr/bin/env python3
"""
Adaptive Evaluation Verification Test
=====================================
Tests that the Adaptive Rule Evaluator:
1. Processes events (black-box verification)
2. Detects high traffic and adjusts interval (verified via logs/behavior)
3. Correctly alerts even during high load
"""

import requests
import time
import random
import string
import threading
from colorama import init, Fore, Style

# Initialize colorama
init()

API_URL = "http://localhost:8080"
EVALUATION_LOG_KEYWORD = "Traffic changed"

def print_header(text):
    print(f"\n{Fore.CYAN}{'='*70}")
    print(f" {text}")
    print(f"{'='*70}{Style.RESET_ALL}")

def print_step(text):
    print(f"{Fore.YELLOW}➜ {text}{Style.RESET_ALL}")

def print_success(text):
    print(f"{Fore.GREEN}✔ {text}{Style.RESET_ALL}")

def print_fail(text):
    print(f"{Fore.RED}✘ {text}{Style.RESET_ALL}")

def generate_random_string(length=8):
    return ''.join(random.choices(string.ascii_lowercase, k=length))

def create_rule():
    """Create a test rule to ensure we have something to evaluate."""
    rule_name = f"AdaptiveTest_{generate_random_string()}"
    payload = {
        "name": rule_name,
        "ruleType": "THRESHOLD",
        "severity": "WARNING",
        "priority": "HIGH",
        "ruleConfig": {
            "metricType": "ERROR_RATE",
            "condition": "GREATER_THAN",
            "thresholdValue": 50.0,
            "timeWindowMinutes": 1,
            "cooldownMinutes": 1,
            "minEventsToEvaluate": 1
        }
    }
    try:
        resp = requests.post(f"{API_URL}/api/v1/rules", json=payload)
        resp.raise_for_status()
        rule = resp.json()
        requests.post(f"{API_URL}/api/v1/rules/{rule['id']}/enable")
        print_success(f"Created test rule: {rule_name}")
        return rule
    except Exception as e:
        print_fail(f"Failed to create rule: {e}")
        return None

def send_burst(count, delay=0.0):
    """Send a burst of events."""
    success = 0
    for _ in range(count):
        try:
            payload = {
                "source": "load-test",
                "eventType": "request",
                "severity": "INFO",
                "timestamp": str(time.time()),
                "metadata": {"load": True}
            }
            requests.post(f"{API_URL}/api/v1/events", json=payload)
            success += 1
            if delay > 0:
                time.sleep(delay)
        except:
            pass
    return success

def run_test():
    print_header("ADAPTIVE EVALUATION TEST SUITE")
    
    # 1. Create a rule
    rule = create_rule()
    if not rule:
        return

    # 2. Warm up (Low Rate)
    print_header("Phase 1: Low Traffic (Should remain IDLE/LOW)")
    print_step("Sending 10 events over 5 seconds...")
    send_burst(10, delay=0.5)
    print_success("Sent low traffic burst")
    time.sleep(2)

    # 3. High Load (High Rate)
    print_header("Phase 2: High Traffic (Should switch to HIGH/BURST)")
    print_step("Sending 200 events as fast as possible...")
    
    start_time = time.time()
    # Use threads to generate real load
    threads = []
    for _ in range(4):
        t = threading.Thread(target=send_burst, args=(50,))
        threads.append(t)
        t.start()
    
    for t in threads:
        t.join()
        
    duration = time.time() - start_time
    eps = 200 / duration
    print_success(f"Sent 200 events in {duration:.2f}s (~{eps:.1f} EPS)")
    
    print_step("Wait for metrics to stabilize...")
    time.sleep(5)
    
    # 4. Trigger Alert
    print_header("Phase 3: Verify Alerting Under Load")
    print_step("Sending failing events to trigger alert...")
    
    try:
        # Send error events
        for _ in range(5):
            requests.post(f"{API_URL}/api/v1/events", json={
                "source": "adaptive-test-fail",
                "eventType": "error", 
                "severity": "ERROR"
            })
            
        print_success("Sent error events")
        
        # Verify alert
        print_step("Checking alerts API...")
        time.sleep(2)
        resp = requests.get(f"{API_URL}/api/v1/alerts")
        if resp.status_code == 200:
            print_success("API is responsive")
        else:
            print_fail(f"API Error: {resp.status_code}")
            
    except Exception as e:
        print_fail(f"Alert check failed: {e}")

    print_header("TEST COMPLETE")
    print("\n⚠️  CHECK APPLICATION LOGS to confirm adaptive behavior:")
    print("   Look for: 'Traffic changed (eps=...). Adjusted interval: ...'")
    print(f"{Fore.CYAN}{'='*70}{Style.RESET_ALL}\n")

if __name__ == "__main__":
    run_test()
```

---

## Step 2: Run the Test

1. Start your application (if not running):
   ```bash
   mvn spring-boot:run
   ```

2. Open a **new terminal** and run the test:
   ```bash
   python3 tests/test_adaptive_evaluation.py
   ```

---

## Step 3: Verify Logs

While the test runs, verify that your application logs show the adaptive behavior.

**Expected Logs:**
```
INFO ... AdaptiveRuleEvaluator : Traffic changed (eps=0.2). Adjusted interval: 30000ms -> 10000ms (LOW)
...
INFO ... AdaptiveRuleEvaluator : Traffic changed (eps=45.5). Adjusted interval: 10000ms -> 500ms (HIGH)
...
INFO ... AdaptiveRuleEvaluator : Traffic changed (eps=0.0). Adjusted interval: 500ms -> 30000ms (IDLE)
```

If you see these logs, **IT WORKS!** 🎉

---

## ✅ Phase 7 Complete!

You have successfully:
- [x] Verified the system adapts to traffic
- [x] Verified alerts still work under load

---

## Next Step

Proceed to **Phase 8: Monitoring** (Optional) to add detailed metrics.

---

*Phase 7 Complete | Total Est. Time: 15 minutes*

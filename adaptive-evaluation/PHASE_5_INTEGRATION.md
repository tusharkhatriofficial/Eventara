# Phase 5: Integration with EventConsumer

## Overview

**Goal**: Connect the new Adaptive Rule Evaluator to the event ingestion stream.  
**Duration**: ~30 minutes  
**Difficulty**: **Medium**

---

## Prerequisites

- [x] Phase 4 completed (`AdaptiveRuleEvaluator`)
- [x] Project compiles successfully

---

## Step 1: Modify EventConsumer.java

**File Path:** `src/main/java/com/eventara/ingestion/kafka/EventConsumer.java`

We need to inject the new components (`AdaptiveRuleEvaluator` and its config) and switch between the old and new evaluation logic based on the feature flag.

### 1.1 Add Imports

Add these imports to the top of the file:

```java
import com.eventara.rule.evaluation.AdaptiveRuleEvaluator;
import com.eventara.rule.evaluation.config.AdaptiveEvaluationProperties;
```

### 1.2 Inject Dependencies

Add these fields to the `EventConsumer` class:

```java
    @Autowired
    private AdaptiveRuleEvaluator adaptiveRuleEvaluator;

    @Autowired
    private AdaptiveEvaluationProperties adaptiveConfig;
```

### 1.3 Update Implementation

Locate the `consumeEvent` method. Find the line where `realTimeRuleEvaluator.evaluateEvent(eventDto)` is called (around line 99).

Replace that *single line* with this logic:

```java
            // -----------------------------------------------------------
            // Rule Evaluation Strategy
            // -----------------------------------------------------------
            if (adaptiveConfig.isEnabled()) {
                // NEW: Adaptive Rate-Based Evaluation
                // This is O(1) - just increments counters and sets a dirty flag
                // The actual evaluation happens asynchronously in AdaptiveRuleEvaluator
                adaptiveRuleEvaluator.onEventIngested(eventDto.isError());
            } else {
                // OLD: Per-Event Evaluation
                // This is O(N*M) blocking call - evaluates all rules inline
                realTimeRuleEvaluator.evaluateEvent(eventDto);
            }
            // -----------------------------------------------------------
```

---

## Step 2: Full Code Reference

If you prefer to see the full method context, here is how the `consumeEvent` method should look:

```java
    @KafkaListener(topics = "${eventara.kafka.topics.events-raw}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeEvent(String message, Acknowledgment acknowledgment) {
        long startTime = System.nanoTime();
        try {
            log.debug("Received event: {}", message);

            // 1. Parse Event
            EventDto eventDto = objectMapper.readValue(message, EventDto.class);

            // 2. Validate Event
            if (eventDto.getEventId() == null || eventDto.getTimestamp() == null) {
                log.warn("Invalid event received: missing required fields");
                acknowledgment.acknowledge();
                return;
            }

            // 3. Enrich Event (e.g. add received timestamp)
            eventDto.setReceivedAt(Instant.now());

            // 4. Save to Database (PostgreSQL)
            EventEntity entity = mapToEntity(eventDto);
            eventRepository.save(entity);

            // 5. Update Metrics (Redis/TimescaleDB)
            metricsService.recordEvent(eventDto);

            // 6. Evaluate Rules (Legacy vs Adaptive)
            if (adaptiveConfig.isEnabled()) {
                adaptiveRuleEvaluator.onEventIngested(eventDto.isError());
            } else {
                realTimeRuleEvaluator.evaluateEvent(eventDto);
            }

            // 7. Acknowledge
            acknowledgment.acknowledge();

            long duration = (System.nanoTime() - startTime) / 1000000;
            log.debug("Event processed in {} ms", duration);

        } catch (Exception e) {
            log.error("Error processing event: {}", message, e);
            // In a real system, might want to send to Dead Letter Queue (DLQ)
            // For now, we acknowledge to avoid infinite loops on bad data
            acknowledgment.acknowledge();
        }
    }
```

---

## Step 3: Verify the Build

Run Maven to verify everything compiles:

```bash
mvn compile
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
```

---

## Step 4: Verification (Mental Check)

**Why is this better?**

Before:
- Kafka consumer thread does: `Parse` -> `Save DB` -> `Save Metrics` -> **`Wait for Rule Eval`** -> `Ack`
- "Wait for Rule Eval" takes 10-50ms PER EVENT if you have many rules/Redis calls.
- If you get 1000 events/sec, your consumer lags.

After (Adaptive Enabled):
- Kafka consumer thread does: `Parse` -> `Save DB` -> `Save Metrics` -> **`Set Flag (0.001ms)`** -> `Ack`
- Consumer is blazing fast ⚡️
- Actual evaluation happens in a background thread in `AdaptiveRuleEvaluator`.

---

## ✅ Phase 5 Complete!

You have successfully:
- [x] Connected the Adaptive Evaluator to the event stream
- [x] Maintained backward compatibility via feature flag

---

## Next Step

Proceed to **Phase 6: Enable Scheduling** to ensure the background "tick" actually runs!

---

*Phase 5 Complete | Total Est. Time: 30 minutes*

# Phase 6: Enable Scheduling

## Overview

**Goal**: Ensure Spring's task scheduling is enabled so the `@Scheduled` tick works.  
**Duration**: ~10 minutes  
**Difficulty**: Easy

---

## Prerequisites

- [x] Phase 5 completed (Integration)
- [ ] Project compiles successfully

---

## Step 1: Verify EventaraApplication.java

**File Path:** `src/main/java/com/eventara/EventaraApplication.java`

Open the main application class and check if `@EnableScheduling` is present.

If it is **NOT** present, add it:

```java
package com.eventara;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // 1. Add Import

@SpringBootApplication
@EnableScheduling  // 2. Add Annotation
public class EventaraApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventaraApplication.class, args);
    }
}
```

**Why?**
Without `@EnableScheduling`, the `@Scheduled(fixedRate = 100)` annotation in `AdaptiveRuleEvaluator` will be ignored, and rules will **NEVER** be evaluated!

---

## Step 2: Verify the Build

Run Maven to verify everything compiles:

```bash
mvn compile
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
```

---

## ✅ Phase 6 Complete!

You have successfully:
- [x] Enabled Spring Scheduling
- [x] Verified the build

---

## Next Step

Proceed to **Phase 7: Testing** to verify everything works as expected!

---

*Phase 6 Complete | Total Est. Time: 10 minutes*

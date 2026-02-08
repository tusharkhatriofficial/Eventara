# Adaptive Rate-Based Evaluation

## 📖 Documentation Index

This folder contains the complete documentation and implementation plan for transforming Eventara's rule evaluation from per-event to adaptive tick-based.

---

## Files

| File | Description |
|------|-------------|
| [ADAPTIVE_RATE_BASED_EVALUATION.md](./ADAPTIVE_RATE_BASED_EVALUATION.md) | Concept document explaining the algorithm and architecture |
| [ADAPTIVE_RATE_BASED_EVALUATION_PLAN.md](./ADAPTIVE_RATE_BASED_EVALUATION_PLAN.md) | Complete implementation plan with all phases |
| [PHASE_1_CONFIGURATION.md](./PHASE_1_CONFIGURATION.md) | Step-by-step guide for Phase 1 (Configuration) |

---

## Quick Overview

### Problem
Current evaluation: **O(events × rules × redis)** = unsustainable at high throughput

### Solution
Adaptive tick-based: **O(rules/tick)** with dynamic intervals based on event rate

### Traffic Tiers

| Events/sec | Tier | Evaluation Interval |
|------------|------|---------------------|
| < 0.1 | IDLE | 30 seconds |
| 0.1 - 1.0 | LOW | 10 seconds |
| 1.0 - 10.0 | MEDIUM | 2 seconds |
| 10.0 - 100.0 | HIGH | 500ms |
| > 100.0 | BURST | 100ms |

---

## Implementation Phases

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | Configuration Infrastructure | 📋 Ready |
| 2 | Event Rate Monitor | 📋 Planned |
| 3 | Evaluation Key for Rule Grouping | 📋 Planned |
| 4 | Adaptive Rule Evaluator | 📋 Planned |
| 5 | Integration with EventConsumer | 📋 Planned |
| 6 | Enable Scheduling | 📋 Planned |
| 7 | Testing | 📋 Planned |
| 8 | Monitoring (Optional) | 📋 Planned |

---

## Getting Started

1. Read the [concept document](./ADAPTIVE_RATE_BASED_EVALUATION.md) to understand the algorithm
2. Review the [full plan](./ADAPTIVE_RATE_BASED_EVALUATION_PLAN.md) to see all phases
3. Start with [Phase 1](./PHASE_1_CONFIGURATION.md) for step-by-step implementation

---

*Last Updated: 2026-02-04*

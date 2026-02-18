# Eventara v0.1.1

**See it before your users do.**

Real-time event analytics and intelligent alerting you can self-host. Kafka-powered ingestion, live dashboards, and rule-based alerts for engineers who want control.

---

## What is Eventara

Eventara is an open-source platform for real-time event monitoring, analytics, and alerting. You stream events from your services, visualize live behavior, detect anomalies, and route alerts -- all on your own infrastructure. No vendor. No surprise bills.

---

## Highlights in v0.1.1

### Threshold Alert Rules (4 Types)

This release introduces a complete rule evaluation engine powered by Drools 8.44, with four production-ready rule types:

- **Simple Threshold** -- fire when a metric crosses a static boundary (e.g., error rate > 10%)
- **Composite (AND/OR)** -- combine multiple conditions with boolean logic (e.g., error rate > 10% AND latency > 500ms)
- **Event Ratio** -- track ratios between event types (e.g., payment.failed / payment.total > 0.05)
- **Rate of Change** -- detect spikes and trend shifts (e.g., throughput dropped 30% in the last 5 minutes)

Rules evaluate against real metrics from the streaming pipeline. The evaluation engine adapts its polling interval based on traffic volume (idle through burst).

### Redis-Backed Distributed Metrics

- Real-time sliding-window aggregation stored in Redis 7
- Automatic rollup from Redis to TimescaleDB every 60 seconds
- Redis retention: 30 minutes. TimescaleDB retention: 30 days (User can change these in properties)
- Distributed cooldowns for alert deduplication (`eventara:rule:cooldown:{ruleId}` with TTL)

### TimescaleDB Time-Series Storage

- PostgreSQL 14 with the TimescaleDB extension for hypertable-backed metrics
- Automatic compression policies on metrics buckets
- Continuous aggregates for historical queries
- 7 managed schema migrations via Flyway

### Adaptive Rule Evaluation

- Evaluation interval adapts to traffic: 30s at idle, 100ms at burst
- Five traffic tiers: idle (<0.1 ev/s), low (<1), medium (<10), high (<100), burst (100+)
- Reduces unnecessary computation during quiet periods, increases sensitivity during spikes

### Notification Channels
- You can now add Webhooks as notification channel.

### Enhanced Dashboard Rule Editor

- Mode selector for all four rule types
- Source and event type filters
- Visual composite condition builder (AND/OR)
- Ratio configuration (numerator/denominator event types)
- Rate of change direction and window settings

---

## Full Feature Set

### Event Ingestion
- REST API (`POST /api/v1/events`) accepts events from any language or framework
- Kafka-backed streaming pipeline (`eventara.events.raw` / `eventara.events.processed`)
- Producer config: acks=all, retries=3, linger.ms=10, batch size 16KB
- Consumer group: `eventara-consumer-group`, auto-offset-reset=earliest

### Real-Time Dashboard
- React 19 + TypeScript + Vite 7
- WebSocket push updates via STOMP/SockJS (1-second intervals)
- Chart.js visualizations for metrics, throughput, and event streams
- Tailwind CSS styling
- Pages: Overview, Real-time monitoring, Event analytics

### Data Layer
- PostgreSQL 14 with TimescaleDB extension (hypertables, compression, continuous aggregates)
- Redis 7 Alpine (sliding-window metrics, distributed cooldowns, LRU eviction at 256MB)
- JSONB metadata columns for flexible event shapes
- Flyway-managed schema (7 migrations): events, alert_rules, alert_history, notification_channels, rule_execution_log, notification_log, metrics_buckets

### Metrics
- Metrics bucket granularity: 10 seconds
- 7 metric types: `SOURCE_ERROR_RATE`, `EVENT_TYPE_COUNT`, `EVENT_RATIO`, `ERROR_RATE_CHANGE`, `LATENCY_CHANGE`, `THROUGHPUT_CHANGE`, `SPIKE_DETECTION`
- System health and throughput endpoints (`GET /api/v1/metrics`)

### Infrastructure
- Single `docker compose up --build -d` brings up the full stack
- 6 containers: Spring Boot API, React dashboard, Kafka, Zookeeper, TimescaleDB, Redis
- Kafka UI available on port 8090

---

## Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Runtime | Java | 21 |
| Framework | Spring Boot | 3.5.7 |
| Messaging | Apache Kafka (Confluent) | 7.5.0 |
| Time-series DB | TimescaleDB (PostgreSQL 14) | latest-pg14 |
| Cache | Redis | 7 Alpine |
| Rule engine | Drools | 8.44.0 |
| Migrations | Flyway | managed |
| API docs | SpringDoc OpenAPI | 2.7.0 |
| Frontend | React | 19.2 |
| Build tool | Vite | 7.2 |
| Styling | Tailwind CSS | 3.4 |
| Charts | Chart.js | 4.5 |
| WebSocket | SockJS + STOMP.js | 1.6 / 7.2 |
| Container | Docker Compose | - |

---

## Quick Start

```bash
git clone https://github.com/tusharkhatriofficial/eventara.git
cd eventara
docker compose up --build -d
```

| Service | URL |
|---|---|
| Dashboard | http://localhost:5173 |
| API | http://localhost:8080 |
| Kafka UI | http://localhost:8090 |
| Swagger | http://localhost:8080/swagger-ui.html |

### Send your first event

```bash
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "payment.failed",
    "source": "payment-service",
    "userId": "user_123",
    "severity": "ERROR"
  }'
```

---

## What is Next

- Multi-channel alert routing (Slack, PagerDuty, email)
- gRPC ingestion endpoint
- More rule types (anomaly detection, forecasting)
- Kubernetes Helm chart

---

## License

Apache 2.0

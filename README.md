<div align="center">
  <!-- <picture> -->
    <!-- Dark Mode Image -->
    <!-- <source media="(prefers-color-scheme: dark)" srcset="https://github.com/user-attachments/assets/99d3120a-4f56-4898-ba51-d6b5c0f14d02"> -->
    <!-- Light Mode Image -->
    <!-- <source media="(prefers-color-scheme: light)" srcset="https://github.com/user-attachments/assets/addfaf5d-5bbe-45b2-a929-0ca7a9d48fd6"> -->
    <!-- Fallback / Default Image (REQUIRED) -->
    <!-- <img alt="Eventara Logo" src="https://github.com/user-attachments/assets/addfaf5d-5bbe-45b2-a929-0ca7a9d48fd6" width="500"> -->
  <!-- </picture> -->
  <img width="334" height="201" alt="ChatGPT Image Feb 11, 2026, 03_08_11 PM copy" src="https://github.com/user-attachments/assets/22289a9e-b4bd-4c69-a94a-0bf738dda21a" />

  
  # See it before your users do
</div>

<div align="center">
  <h3>Open-source, self-hosted event monitoring with real-time analytics and intelligent alerting</h3>
  <p>Kafka-powered ingestion. TimescaleDB + Redis metrics. Intelligent alerting. Live dashboards. One <code>docker compose up</code>.</p>

  <p>
    <a href="./LICENSE">
      <img alt="License" src="https://img.shields.io/badge/license-Apache%202.0-blue.svg">
    </a>
    <a href="#quick-start">
      <img alt="Docker Ready" src="https://img.shields.io/badge/docker-ready-blue">
    </a>
    <a href="https://kafka.apache.org/">
      <img alt="Kafka Powered" src="https://img.shields.io/badge/powered%20by-Kafka-black">
    </a>
    <a href="#contributing">
      <img alt="PRs Welcome" src="https://img.shields.io/badge/PRs-welcome-brightgreen">
    </a>
    <a href="https://github.com/tusharkhatriofficial/eventara/stargazers">
      <img alt="GitHub stars" src="https://img.shields.io/github/stars/tusharkhatriofficial/eventara?style=social">
    </a>
  </p>

  <p>
    <a href="https://eventara-docs.vercel.app">Docs</a>
    ·
    <a href="https://github.com/tusharkhatriofficial/eventara/issues">Issues</a>
    ·
    <a href="https://github.com/tusharkhatriofficial/eventara/discussions">Discussions</a>
    ·
    <a href="#quick-start">Quick Start</a>
  </p>

  <img width="1646" height="890" alt="Screenshot 2026-01-28 at 2 04 28 PM" src="https://github.com/user-attachments/assets/030e2526-0e85-474c-94f2-940920de438b" />
  <img width="1646" height="890" alt="Screenshot 2026-01-28 at 2 06 25 PM" src="https://github.com/user-attachments/assets/5c4641bb-4838-40c4-8629-ba0caabb3632" />
  <img width="1646" height="890" alt="Screenshot 2026-01-28 at 2 06 52 PM" src="https://github.com/user-attachments/assets/15989697-e6cd-4c07-a2d8-5cdb81e3f221" />

</div>

---

## Why Eventara

Every company that scales past a handful of services needs event monitoring. The options today are either expensive SaaS platforms where you hand over your data, or stitching together 5+ open-source tools yourself.

Eventara is a single, self-hosted platform that handles ingestion, streaming, real-time analytics, threshold alerting, and live dashboards out of the box. You keep your data. You control the pipeline. You deploy with one command.

---

## What it does today (v0.1.1)

**Ingestion** -- REST API accepts events from any service, language, or platform. Events flow into Kafka for ordered, durable, high-throughput delivery.

**Real-time analytics** -- Redis sliding-window aggregation for sub-second metrics. TimescaleDB hypertables for time-series storage with automatic compression and continuous aggregates. Metrics roll up from Redis to TimescaleDB every 60 seconds.

**4 threshold rule types** -- evaluated in real time by a Java handler engine:

| Rule Type | What it does |
|---|---|
| **Simple Threshold** | Fire when a metric crosses a boundary (error rate > 10%) |
| **Composite** | AND/OR logic across multiple conditions (error rate > 10% AND latency > 500ms) |
| **Event Ratio** | Track ratios between event types (payment.failed / payment.total > 0.05) |
| **Rate of Change** | Detect spikes and drops (throughput fell 30% in 5 minutes) |

Drools is used under the hood for DRL generation, syntax validation, and the rule-testing API -- but the hot-path evaluation is pure Java for maximum throughput.

**Adaptive evaluation** -- the engine adjusts its polling interval based on traffic volume. 30s at idle, 100ms during bursts. No wasted compute when things are quiet, instant sensitivity when they are not.

**Live dashboard** -- React 19 + Vite + Chart.js with WebSocket push updates every second. Rule editor with visual composite builder, ratio config, and source/event filters.

**Webhook notifications** -- alert channels with webhook delivery when rules fire.

---

## Quick Start

```bash
git clone https://github.com/tusharkhatriofficial/eventara.git
cd eventara
docker compose up --build -d
```

For a VM-style deploy using a Docker image, see [`DEPLOYMENT.md`](./DEPLOYMENT.md).

| Service | URL |
|---|---|
| Dashboard | http://localhost:5173 |
| API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| Kafka UI | http://localhost:8090 |

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

Open the dashboard. Events appear in real time.

---

## Architecture

```
Your Services --> REST API (Spring Boot) --> Kafka --> Redis (real-time metrics)
                                              |                |
                                              v                v
                                        TimescaleDB      Drools Engine
                                              |                |
                                              v                v
                                        Flyway Schema    WebSocket (STOMP)
                                                               |
                                                               v
                                                     Dashboard (React + Vite)
```

Core stack in one Docker Compose file. Full stack in 30 seconds.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21, Spring Boot 3.5.7 |
| Messaging | Apache Kafka 3.7 (KRaft mode; no Zookeeper) |
| Time-series | TimescaleDB (PostgreSQL 14) |
| Cache / Metrics | Redis 7 |
| Rule Engine | Java handler pattern (Drools for DRL validation) |
| Migrations | Flyway (7 migrations) |
| Frontend | React 19, Vite 7, Tailwind CSS, Chart.js |
| Real-time | WebSocket via STOMP + SockJS |
| API Docs | SpringDoc OpenAPI 2.7 |
| Deploy | Docker Compose |

---

## API

```bash
# Ingest event
POST /api/v1/events

# Get metrics
GET /api/v1/metrics

# Query events by type
GET /api/v1/events/type/{type}?page=0&size=10

# Alert rules CRUD
POST /api/v1/rules
GET /api/v1/rules
PUT /api/v1/rules/{id}
DELETE /api/v1/rules/{id}

# Test rule evaluation
POST /api/v1/rules/{id}/test
```

Full API docs at `https://www.eventara.co/introduction`.

---

## Roadmap

- [x] Event ingestion + Kafka pipeline
- [x] Real-time dashboard with WebSocket
- [x] TimescaleDB time-series storage
- [x] Redis distributed metrics
- [x] 4 threshold rule types (Simple, Composite, Ratio, Rate of Change)
- [x] Adaptive evaluation engine
- [x] Webhook notification channel
- [ ] Multi-channel alerts (Slack, PagerDuty, email)
- [ ] gRPC ingestion endpoint
- [ ] Anomaly detection and forecasting rules
- [ ] Kubernetes Helm chart
- [ ] Authentication and RBAC

---

## Contributing

Contributions welcome. Whether you are learning Spring Boot and Kafka, or you want a self-hosted monitoring platform for your team.

```bash
# Backend
mvn spring-boot:run

# Frontend
cd eventara-dashboard && npm install && npm run dev
```

1. Fork and create a branch
2. Make your changes
3. Open a Pull Request

Look for issues tagged `good first issue` to get started.

---

## License

Apache 2.0. See [LICENSE](./LICENSE).

---

<div align="center">
  <p>
    <a href="https://eventara-docs.vercel.app">Documentation</a>
    ·
    <a href="https://github.com/tusharkhatriofficial/eventara/issues">Issues</a>
    ·
    <a href="https://github.com/tusharkhatriofficial/eventara/discussions">Discussions</a>
  </p>

  <sub>Built by <a href="https://github.com/tusharkhatriofficial">Tushar Khatri</a></sub>
</div>

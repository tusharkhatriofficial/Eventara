Eventara
<div align="center">
  Real-Time Event Analytics & Alearting Platform
  <br> <br> 
  <!-- <img width="1024" height="470" alt="Eventara Banner" src="https://github.com/user-attachments/assets/106068a7-0527-498f-8b5e-02a2f9f4810c" />  -->
<img width="1440" height="732" alt="Screenshot 2025-11-29 at 12 03 49 AM" src="https://github.com/user-attachments/assets/423d92aa-7bc0-4f6a-90cf-8f6d5c22c71a" />

</div>

---

## What is Eventara?

Eventara is an **open-source, self-hosted platform** that helps you monitor and understand what's happening in your applications in real-time. Send events from your services, define alert rules, and get notified instantly when something goes wrong.

**Built for:**
- Startups that can't afford expensive monitoring tools
- Mid-sized companies needing customizable observability
- Enterprises wanting full control over their data

---

## Core Concept

Your Application → Eventara → Real-Time Analytics + Intelligent Alerts

### Example Use Cases

- Monitor payment failures and alert your team before users complain
- Track signup rates and detect sudden drops instantly
- Analyze user behavior patterns in real-time
- Get notified when API error rates spike
- Monitor IoT sensor data and trigger actions on anomalies

---

## Quick Start

### Prerequisites

- Docker & Docker Compose
- Git

### Run Eventara (All Services)

1. Clone the repository

   `git clone https://github.com/tusharkhatriofficial/eventara.git`
   
   `cd eventara`

3. Start all services

   `docker compose up --build -d`

4. Wait ~30 seconds for all services to be ready

   `docker compose logs -f`

5. Access the services:

| Service | URL | Description |
|--------|-----|-------------|
| Dashboard | http://localhost:5173 | Real-time analytics UI |
| API | http://localhost:8080 | REST API endpoints |
| Kafka UI | http://localhost:8090 | Kafka topic visualization |

### Test the System

Send a test event:

```shell
curl -X POST http://localhost:8080/api/v1/events
-H "Content-Type: application/json"
-d '{
"eventType": "user.login",
"source": "auth-service",
"userId": "test_user_123",
"severity": "INFO"
}'
```

Watch the dashboard update in real-time at http://localhost:5173 🎉

### Run Demo Script (20-second showcase)

macOS zsh / Linux bash

```shell
for i in {1..60}; do
  curl -s -X POST http://localhost:8080/api/v1/events \
    -H "Content-Type: application/json" \
    -d '{"eventType":"user.login","source":"auth-service","userId":"user_1","severity":"INFO"}' &&
  curl -s -X POST http://localhost:8080/api/v1/events \
    -H "Content-Type: application/json" \
    -d '{"eventType":"payment.success","source":"payment-service","userId":"user_2","severity":"INFO"}' &&
  curl -s -X POST http://localhost:8080/api/v1/events \
    -H "Content-Type: application/json" \
    -d '{"eventType":"order.created","source":"order-service","userId":"user_3","severity":"WARNING"}' &&
  curl -s -X POST http://localhost:8080/api/v1/events \
    -H "Content-Type: application/json" \
    -d '{"eventType":"payment.failed","source":"payment-service","userId":"user_4","severity":"ERROR"}'
  sleep 0.3
done
```

Windows PowerShell

```shell
for ($i = 1; $i -le 60; $i++) {
    curl -s -Method POST -Uri "http://localhost:8080/api/v1/events" `
        -Headers @{ "Content-Type"="application/json" } `
        -Body '{"eventType":"user.login","source":"auth-service","userId":"user_1","severity":"INFO"}'

    curl -s -Method POST -Uri "http://localhost:8080/api/v1/events" `
        -Headers @{ "Content-Type"="application/json" } `
        -Body '{"eventType":"payment.success","source":"payment-service","userId":"user_2","severity":"INFO"}'

    curl -s -Method POST -Uri "http://localhost:8080/api/v1/events" `
        -Headers @{ "Content-Type"="application/json" } `
        -Body '{"eventType":"order.created","source":"order-service","userId":"user_3","severity":"WARNING"}'

    curl -s -Method POST -Uri "http://localhost:8080/api/v1/events" `
        -Headers @{ "Content-Type"="application/json" } `
        -Body '{"eventType":"payment.failed","source":"payment-service","userId":"user_4","severity":"ERROR"}'

    Start-Sleep -Milliseconds 300
}
```

---

## Dashboard Features

The real-time dashboard includes:

- ✅ **Overview** - System health, key metrics, and live charts
- ✅ **Real-Time Monitoring** - Live event streams and throughput gauges
- ✅ **Event Analytics** - Detailed event type analysis and patterns
- ✅ **Source Analytics** - Service health and performance monitoring *(coming soon)*
- ✅ **Error Analysis** - Error tracking and debugging tools *(coming soon)*
- ✅ **Performance Metrics** - Latency analysis and optimization insights *(coming soon)*

**All metrics update live via WebSocket** - no page refresh needed!

---

## Architecture

┌─────────────────────────────────────────────────────────────┐
│ Your Applications │
│ (Microservices, APIs, IoT Devices, Mobile Apps, etc.) │
└────────────┬────────────────────────────────────────────────┘
│ HTTP/gRPC Events
▼
┌─────────────────────────────────────────────────────────────┐
│ Eventara Platform │
│ │
│ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ │
│ │ Ingestion │───▶│ Kafka │──▶│ Analytics │ │
│ │ Service │ │ (Streaming) │ │ Engine │ │
│ └──────────────┘ └──────────────┘ └──────────────┘ │
│ │ │ │
│ ▼ ▼ │
│ ┌──────────────┐ ┌──────────────┐ │
│ │ PostgreSQL │ │ Dashboard │ │
│ │ (Storage) │ │ (React) │ │
│ └──────────────┘ └──────────────┘ │
└─────────────────────────────────────────────────────────────┘

---

## 🛠️ Built With Modern Tech Stack

| Category | Technology |
|----------|------------|
| **Backend** | Spring Boot (Java 21) |
| **Messaging** | Apache Kafka |
| **Storage** | PostgreSQL + TimescaleDB *(planned)* |
| **Real-Time** | WebSocket |
| **Frontend** | React + TypeScript + Vite |
| **Styling** | Tailwind CSS |
| **Charts** | Chart.js |
| **Cache** | Redis *(planned)* |
| **Analytics** | Kafka Streams |
| **APIs** | REST + gRPC *(planned)* |
| **Monitoring** | Prometheus + Grafana *(planned)* |
| **Deployment** | Docker + Kubernetes ready |

---

## API Documentation

### Endpoints

**Create Event:**
POST http://localhost:8080/api/v1/events
Content-Type: application/json

```json
{
"eventType": "user.signup",
"source": "web-app",
"userId": "user_456",
"severity": "INFO",
"tags": {
"campaign": "summer_sale"
},
"metadata": {
"plan": "premium"
}
}
```

**Get Metrics:**
GET http://localhost:8080/api/v1/metrics

**Query Events:**
GET http://localhost:8080/api/v1/events?eventType=user.login&limit=100

---

## 🐳 Docker Commands

Start all services

`docker compose up --build -d`

View logs

`docker compose logs -f springboot`

`docker compose logs -f dashboard`

`docker compose logs -f kafka`

Check service status

`docker compose ps`

Stop all services

`docker compose down`

Wipe all data (DB + Kafka)

`docker compose down -v`

Restart specific service

`docker compose restart springboot`

`docker compose restart dashboard`

Access PostgreSQL

`docker exec -it postgres14 psql -U postgres -d eventara`

List Kafka topics

`docker exec -it eventara-kafka kafka-topics --bootstrap-server localhost:9092 --list`

---

## Project Roadmap

### Phase 1: Foundation ✅ (Completed)
- [x] Project architecture design
- [x] Docker setup with PostgreSQL, Kafka, Zookeeper
- [x] Spring Boot event ingestion service
- [x] REST API for event submission
- [x] Kafka producer/consumer integration
- [x] PostgreSQL event storage

### Phase 2: Real-Time Analytics ✅ (Completed)
- [x] WebSocket real-time data streaming
- [x] Comprehensive metrics service
- [x] React dashboard with TypeScript
- [x] Overview page with live metrics
- [x] Real-time monitoring page
- [x] Event analytics page
- [x] Source analytics page
- [x] Error analysis page
- [x] Performance metrics page

### Phase 3: Rule Engine & Alerting 🚧 (In Progress)
- [ ] Rule definition engine
- [ ] Alert threshold configuration
- [ ] Multi-channel notifications (Email, Slack, Webhook)
- [ ] Alert acknowledgment workflow

### Phase 4: Advanced Features ⏳ (Next)
- [ ] Time-series database (TimescaleDB)
- [ ] Historical data queries
- [ ] Custom dashboards
- [ ] Data export (CSV, JSON)
- [ ] User authentication & authorization

### Phase 5: Production Ready ⏳
- [ ] Redis caching layer
- [ ] Horizontal scaling support
- [ ] Kubernetes deployment configs
- [ ] Prometheus + Grafana monitoring
- [ ] Production deployment guides

---

## Why Eventara?

| Problem | Solution |
|---------|----------|
| **High Cost** - Tools like Datadog, Splunk cost $200-2000/month | **Free & Open Source** - No licensing fees ever |
| **Vendor Lock-in** - Data trapped in proprietary systems | **Full Control** - Your data stays with you |
| **Generic Solutions** - One-size-fits-all approach | **Customizable** - Adapt to your exact needs |
| **Cloud Only** - Forced to use specific providers | **Self-Hosted** - Deploy anywhere you want |

Perfect for:
- **Startups** — Get enterprise-level monitoring without the enterprise price tag
- **Mid-Sized Companies** — Customize everything to match your exact needs
- **Enterprises** — Full data sovereignty and compliance control

---

## Troubleshooting

<details>
<summary><b>Dashboard shows "Connecting..." forever</b></summary>

- Check if Spring Boot is running: `docker compose logs springboot`
- Verify WebSocket endpoint: `curl http://localhost:8080/ws`
- Restart services: `docker compose restart springboot dashboard`
</details>

<details>
<summary><b>No events appearing</b></summary>

- Check Kafka topics exist:
  docker exec -it eventara-kafka kafka-topics --list --bootstrap-server localhost:9092

- View consumer logs: `docker compose logs -f springboot | grep Consumer`
- Verify database connection: `docker compose logs springboot | grep datasource`
</details>

<details>
<summary><b>Port already in use</b></summary>

Change ports in `docker-compose.yml` if 5173, 8080, or 5432 are occupied:
ports:

"5174:5173" # Changed from 5173

</details>

---

## Contributing

This project is in early stages, and we welcome contributors! Whether you're learning Spring Boot, Kafka, React, or DevOps, this is a great project to grow your skills.

### Ways to Contribute

- Code contributions (check issues labeled `good-first-issue`)
- Documentation improvements
- Testing and bug reports
- Feature suggestions
- Spread the word!

### Development Setup (Local)

Backend
cd eventara
mvn spring-boot:run

Frontend
cd eventara-dashboard
npm install
npm run dev

### Contribution Guidelines

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## License

Licensed under the [Apache License 2.0](./LICENSE).

You are free to use, modify, and distribute this software with proper attribution.

---

## Contact & Community

- 🐛 **Issues:** [GitHub Issues](https://github.com/tusharkhatriofficial/eventara/issues)
- 💬 **Discussions:** [GitHub Discussions](https://github.com/tusharkhatriofficial/eventara/discussions)
- 📧 **Email:** hello@tusharkhatri.in

---

<div align="center">

### ⭐ Star this repo if you're interested in the project!

**Built with ❤️ for the developer community**

[![GitHub stars](https://img.shields.io/github/stars/tusharkhatriofficial/eventara?style=social)](https://github.com/tusharkhatriofficial/eventara)
[![GitHub forks](https://img.shields.io/github/forks/tusharkhatriofficial/eventara?style=social)](https://github.com/tusharkhatriofficial/eventara)
[![GitHub watchers](https://img.shields.io/github/watchers/tusharkhatriofficial/eventara?style=social)](https://github.com/tusharkhatriofficial/eventara)

</div>

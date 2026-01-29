# Eventara - Technical Codebase Report
## Comprehensive Technical Documentation for Y Combinator Application

---

## Executive Summary

**Eventara** is an open-source, self-hosted real-time event monitoring and alerting platform designed to provide enterprise-grade observability at zero licensing cost. The platform enables organizations to ingest events from any application, define intelligent alerting rules, and receive instant notifications when anomalies occur.

### Key Value Propositions
- **Zero Cost**: Completely open-source, eliminating $200-2000+/month monitoring costs
- **Self-Hosted**: Full data sovereignty and privacy compliance
- **Real-Time**: Sub-second event processing with live WebSocket dashboard
- **Intelligent Alerting**: Rule-based alerting using the Drools rule engine with threshold-based rules (Pattern, Anomaly, and CEP rules planned)
- **Multi-Channel Notifications**: Webhook support (Email, Slack, SMS, PagerDuty planned)

### Current Status
- **Stage**: v0.1.0 - First Public Release
- **Readiness**: Production-ready for threshold-based monitoring use cases
- **Architecture**: Fully containerized, Docker Compose deployment ready

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Your Applications                                │
│           (Microservices, APIs, IoT Devices, Mobile Apps)               │
└────────────────────────────┬────────────────────────────────────────────┘
                             │ HTTP REST Events
                             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         EVENTARA PLATFORM                               │
│                                                                         │
│  ┌──────────────────┐     ┌──────────────────┐    ┌─────────────────┐  │
│  │ Event Ingestion  │────▶│  Apache Kafka    │───▶│ Event Consumer  │  │
│  │   Controller     │     │  (Message Queue) │    │   + Metrics     │  │
│  │   /api/v1/events │     │  events-raw topic│    │   Recording     │  │
│  └──────────────────┘     └──────────────────┘    └────────┬────────┘  │
│                                                              │          │
│                                   ┌──────────────────────────┤          │
│                                   │                          │          │
│                                   ▼                          ▼          │
│  ┌──────────────────┐     ┌──────────────────┐    ┌─────────────────┐  │
│  │    PostgreSQL    │◀────│ Comprehensive    │    │ Drools Rule     │  │
│  │  (Event Storage) │     │ Metrics Service  │    │   Engine        │  │
│  │                  │     │ (Real-time Stats)│    │ (Alert Trigger) │  │
│  └──────────────────┘     └────────┬─────────┘    └────────┬────────┘  │
│                                    │                        │          │
│                                    ▼                        ▼          │
│                          ┌──────────────────┐    ┌─────────────────┐  │
│                          │    WebSocket     │    │ Notification    │  │
│                          │   Push (1s)      │    │ Service         │  │
│                          │  /topic/metrics  │    │ (Webhook, etc.) │  │
│                          └────────┬─────────┘    └─────────────────┘  │
│                                   │                                   │
└───────────────────────────────────┼───────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      React Dashboard (Vite + TypeScript)                │
│                                                                         │
│  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐ │
│  │ Overview  │ │ Real-Time │ │ Event     │ │ Rules     │ │ Settings  │ │
│  │           │ │ Monitoring│ │ Analytics │ │ Editor    │ │ Notif.    │ │
│  └───────────┘ └───────────┘ └───────────┘ └───────────┘ └───────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Technology Stack

| Layer | Technology | Version | Purpose |
|-------|------------|---------|---------|
| **Backend** | Spring Boot | 3.5.7 | REST API, Dependency Injection, WebSocket |
| **Language** | Java | 21 | LTS, Virtual Threads, Modern Features |
| **Message Queue** | Apache Kafka | 7.5.0 | Event streaming, decoupling |
| **Database** | PostgreSQL | 14 | Event storage, Rule persistence |
| **Rule Engine** | Drools | 8.44.0 | Dynamic DRL rule execution |
| **Frontend** | React | 19.2.0 | Modern UI framework |
| **Build Tool** | Vite | 7.2.4 | Fast HMR, ES Modules |
| **Styling** | Tailwind CSS | 3.4.18 | Utility-first CSS |
| **Charts** | Chart.js | 4.5.1 | Data visualization |
| **Real-Time** | STOMP/SockJS | Latest | WebSocket communication |
| **DB Migration** | Flyway | Latest | Schema versioning |
| **Templating** | Thymeleaf | Latest | Email HTML templates |
| **Scheduling** | Quartz | Latest | Rule evaluation scheduling |
| **Container** | Docker + Compose | Latest | Deployment orchestration |

---

## Backend Architecture (Spring Boot)

### Module Structure

```
src/main/java/com/eventara/
├── EventaraApplication.java     # Main entry point
├── ingestion/                   # Event ingestion module
│   ├── controller/              # REST endpoints
│   │   └── EventController.java
│   ├── service/
│   │   └── EventService.java
│   ├── kafka/
│   │   ├── EventProducer.java   # Kafka publisher
│   │   └── EventConsumer.java   # Kafka subscriber
│   ├── mapper/
│   │   └── EventMapper.java
│   └── model/entity/
│       └── Event.java           # JPA entity
│
├── analytics/                   # Real-time analytics module
│   ├── controller/
│   │   ├── MetricsController.java
│   │   └── MetricsWebSocketController.java
│   └── service/
│       ├── ComprehensiveMetricsService.java  # Core metrics
│       └── MetricsAggregationService.java
│
├── rule/                        # Alert rule management
│   ├── controller/
│   │   ├── RuleController.java
│   │   └── RuleTestController.java
│   ├── service/
│   │   ├── RuleService.java          # Interface
│   │   ├── RuleServiceImpl.java      # Implementation
│   │   ├── DrlGeneratorService.java  # Drools DRL generation
│   │   ├── RuleCompilerService.java
│   │   └── RuleValidationService.java
│   ├── entity/
│   │   └── AlertRule.java
│   ├── dto/
│   │   ├── request/
│   │   │   ├── CreateRuleRequest.java
│   │   │   ├── UpdateRuleRequest.java
│   │   │   └── TestRuleRequest.java
│   │   └── response/
│   │       ├── RuleResponse.java
│   │       └── RuleTestResult.java
│   ├── enums/
│   │   ├── MetricType.java       # 28 metric types
│   │   ├── Condition.java        # 8 comparison operators
│   │   ├── RuleType.java         # THRESHOLD, PATTERN, ANOMALY, CEP
│   │   └── RuleStatus.java       # ACTIVE, INACTIVE, ARCHIVED, DRAFT
│   └── repository/
│       └── RuleRepository.java
│
├── alert/                       # Alert handling
│   ├── entity/
│   │   └── AlertHistory.java
│   ├── service/
│   │   └── AlertTriggerHandler.java  # Called by Drools
│   ├── enums/
│   │   ├── AlertSeverity.java    # CRITICAL, WARNING, INFO
│   │   └── AlertStatus.java
│   └── repository/
│       └── AlertHistoryRepository.java
│
├── notification/                # Multi-channel notifications
│   ├── controller/
│   │   ├── NotificationChannelController.java
│   │   └── NotificationLogController.java
│   ├── service/
│   │   ├── NotificationService.java        # Interface
│   │   ├── NotificationServiceImpl.java    # Implementation
│   │   └── WebhookNotificationHandler.java # Webhook delivery
│   ├── entity/
│   │   ├── NotificationChannel.java
│   │   └── NotificationLog.java
│   ├── enums/
│   │   ├── ChannelType.java      # WEBHOOK, EMAIL, SLACK, SMS, PAGERDUTY
│   │   └── NotificationStatus.java
│   └── dto/
│       └── NotificationMessage.java
│
├── drools/                      # Rule execution engine
│   ├── service/
│   │   └── RuleExecutionService.java  # KIE session management
│   └── fact/
│       └── MetricsFact.java           # Facts for rule evaluation
│
└── common/                      # Shared utilities
    ├── dto/
    │   ├── EventDto.java
    │   ├── EventRequest.java
    │   ├── EventResponse.java
    │   └── ComprehensiveMetricsDto.java
    └── repository/
        └── EventRepository.java
```

---

## Data Flow: Event Ingestion to Alert

### Step-by-Step Flow

```
1. CLIENT → POST /api/v1/events
   {
     "eventType": "payment.failed",
     "source": "payment-service",
     "userId": "user_123",
     "severity": "ERROR"
   }

2. EventController.ingestEvent()
   └── Validates request
   └── Returns HTTP 202 Accepted immediately

3. EventService.processEvent()
   └── Maps request to Event entity
   └── Generates unique eventId (UUID)
   └── Passes to EventProducer

4. EventProducer.sendEvent()
   └── Publishes to Kafka topic "events-raw"
   └── Uses eventId as partition key
   └── Async callback logs success/failure

5. [Kafka stores event in topic partition]

6. EventConsumer.ConsumeEvent()
   └── @KafkaListener receives event
   └── Checks for duplicate (deduplication)
   └── Saves to PostgreSQL via EventRepository
   └── Converts to EventDto
   └── Calls ComprehensiveMetricsService.recordEvent()
   └── Manually acknowledges Kafka offset

7. ComprehensiveMetricsService.recordEvent()
   └── Increments total event counter
   └── Updates eventsByType, eventsBySource maps
   └── Tracks latency metrics
   └── Maintains 24-hour sliding window
   └── Detects anomalies (>5% error rate, P95 >1s, 3x traffic spike)

8. MetricsWebSocketController (@Scheduled every 1s)
   └── Calls getComprehensiveMetrics()
   └── Pushes to WebSocket /topic/metrics
   └── Dashboard receives real-time update

9. RuleExecutionService (Scheduled by timeWindowMinutes)
   └── Groups active rules by evaluation interval
   └── Creates KieSession for each group
   └── Injects current MetricsFact
   └── Fires all matching rules

10. [If Threshold Rule Matches]
    └── DRL rule condition evaluates true
    └── Calls AlertTriggerHandler.handleThresholdAlert()

11. AlertTriggerHandler.handleThresholdAlert()
    └── Checks suppression window (default 30 min)
    └── Creates AlertHistory entity
    └── Saves to database
    └── Updates rule trigger count
    └── Calls NotificationService.sendNotification()

12. NotificationServiceImpl.sendNotification()
    └── Fetches configured channels from rule
    └── Checks rate limits
    └── Routes to appropriate handler (WebhookNotificationHandler)
    └── Logs notification attempt
    └── Updates AlertHistory with notification results
```

---

## Rule Engine: Threshold Rules Deep Dive

### Currently Implemented: THRESHOLD Rules

Threshold rules compare a metric value against a threshold using a condition. When the condition is met, an alert fires.

#### Available Metric Types (28 metrics)

```java
// Error Metrics
ERROR_RATE,          // Percentage of events that are errors
TOTAL_ERRORS,        // Absolute error count

// Performance Metrics  
AVG_LATENCY,         // Average processing latency (ms)
P50_LATENCY,         // 50th percentile latency
P95_LATENCY,         // 95th percentile latency
P99_LATENCY,         // 99th percentile latency
MAX_LATENCY,         // Maximum observed latency
MIN_LATENCY,         // Minimum observed latency

// Throughput Metrics
EVENTS_PER_SECOND,   // Current events/second
EVENTS_PER_MINUTE,   // Current events/minute
EVENTS_PER_HOUR,     // Hourly projection
EVENTS_PER_DAY,      // Daily projection
PEAK_THROUGHPUT,     // Highest observed throughput
AVG_THROUGHPUT_1H,   // Average over last hour
AVG_THROUGHPUT_24H,  // Average over last 24 hours

// Time Window Metrics
EVENTS_LAST_1_MINUTE,
EVENTS_LAST_5_MINUTES,
EVENTS_LAST_15_MINUTES,
EVENTS_LAST_1_HOUR,
EVENTS_LAST_24_HOURS,

// Summary Metrics
TOTAL_EVENTS,        // All-time event count
UNIQUE_SOURCES,      // Number of distinct sources
UNIQUE_EVENT_TYPES,  // Number of distinct event types
UNIQUE_USERS,        // Number of distinct users
SYSTEM_HEALTH,       // "healthy", "degraded", "critical"

// User Metrics
ACTIVE_USERS_LAST_1_HOUR,
ACTIVE_USERS_LAST_24_HOURS,
TOTAL_UNIQUE_USERS
```

#### Available Conditions

```java
GREATER_THAN(">"),
LESS_THAN("<"),
EQUALS("=="),
GREATER_THAN_OR_EQUAL(">="),
LESS_THAN_OR_EQUAL("<="),
NOT_EQUALS("!="),
BETWEEN("between"),      // Not yet implemented
NOT_BETWEEN("not between") // Not yet implemented
```

### Rule Creation Flow

```
1. User creates rule via dashboard:
   POST /api/v1/rules
   {
     "name": "High Error Rate Alert",
     "ruleType": "THRESHOLD",
     "severity": "CRITICAL",
     "priority": 10,
     "ruleConfig": {
       "metricType": "ERROR_RATE",
       "condition": "GREATER_THAN",
       "thresholdValue": 5.0,
       "timeWindowMinutes": 5
     },
     "notificationChannels": ["slack-alerts"],
     "suppressionWindowMinutes": 30,
     "maxAlertsPerHour": 10
   }

2. RuleServiceImpl.createRule():
   - Validates request
   - Calls DrlGeneratorService.generateDrl()
   - Generates DRL hash for change detection
   - Saves AlertRule to PostgreSQL
   - Triggers RuleExecutionService.reloadRule()

3. DrlGeneratorService.generateThresholdDrl():
   - Generates Drools Rule Language code:

   package com.eventara.rules
   
   import com.eventara.drools.fact.MetricsFact
   import com.eventara.alert.service.AlertTriggerHandler
   
   global com.eventara.alert.service.AlertTriggerHandler alertHandler;
   
   rule "High Error Rate Alert"
       salience 10
       when
           $metrics: MetricsFact(
               (currentTimeSeconds / 60) % 5 == 0,
               errorRate > 5.0
           )
           $handler: AlertTriggerHandler()
       then
           $handler.handleThresholdAlert(
               1L,
               "High Error Rate Alert",
               "CRITICAL",
               5.0,
               $metrics.getErrorRate()
           );
   end

4. RuleExecutionService:
   - Groups rules by timeWindowMinutes
   - Creates separate KieContainer per group
   - Schedules evaluation at fixed intervals
   - Aligns to clock (5-min rules fire at :00, :05, :10...)
```

### Rule Lifecycle

| Status | Description |
|--------|-------------|
| `ACTIVE` | Rule is loaded in Drools, being evaluated |
| `INACTIVE` | Rule exists but not evaluated |
| `ARCHIVED` | Rule is soft-deleted, hidden from UI |
| `DRAFT` | Rule under construction, not yet enabled |

### Alert Suppression & Rate Limiting

```java
// On AlertRule entity:
suppressionWindowMinutes = 30  // Default: suppress duplicates for 30 min
maxAlertsPerHour = 10          // Default: max 10 alerts per hour per rule
```

This prevents alert fatigue by:
1. Checking if same rule triggered within suppression window
2. Enforcing max alerts per hour limit

---

## Notification System

### Implemented: Webhook Notifications

```java
// NotificationChannel entity
{
  "channelType": "WEBHOOK",
  "name": "slack-alerts",
  "config": {
    "url": "https://hooks.slack.com/services/...",
    "method": "POST",
    "headers": { "Content-Type": "application/json" }
  },
  "rateLimitPerMinute": 60,
  "rateLimitPerHour": 1000
}
```

### Notification Flow

```
AlertTriggerHandler
    └── sendNotifications(alert, ruleId)
            └── Builds NotificationMessage
            └── Fetches channel names from rule
            └── Calls NotificationService.sendNotification() async

NotificationServiceImpl
    └── For each channel:
        └── Validate channel enabled
        └── Check rate limits
        └── Route to WebhookNotificationHandler
        └── Log attempt
        └── Update channel stats

WebhookNotificationHandler
    └── Build HTTP request from config
    └── Send POST with alert payload
    └── Record response code & body
    └── Return NotificationResult
```

### Notification Payload Example

```json
{
  "alertId": 123,
  "ruleName": "High Error Rate Alert",
  "severity": "CRITICAL",
  "subject": "Alert: High Error Rate Alert",
  "message": "Alert: High Error Rate Alert - Threshold: 5.00, Actual: 7.23",
  "thresholdValue": 5.0,
  "actualValue": 7.23,
  "triggeredAt": "2026-01-28T15:30:00",
  "context": {
    "ruleName": "High Error Rate Alert",
    "thresholdValue": 5.0,
    "actualValue": 7.23,
    "timestamp": "2026-01-28T15:30:00"
  }
}
```

### Planned Notification Channels

| Channel | Implementation Status |
|---------|----------------------|
| Webhook | ✅ Implemented |
| Email | 🚧 Template ready (Thymeleaf), handler TODO |
| Slack | 📋 Planned |
| SMS | 📋 Planned |
| PagerDuty | 📋 Planned |

---

## Real-Time Dashboard (React)

### Frontend Architecture

```
eventara-dashboard/src/
├── App.tsx                     # Router setup
├── main.tsx                    # Entry point
├── index.css                   # Design system (TailwindCSS)
│
├── hooks/
│   └── useWebSocket.ts         # STOMP/SockJS connection
│
├── pages/
│   ├── Overview.tsx            # Hero metrics, charts, alerts
│   ├── RealTimeMonitoring.tsx  # Live gauges, throughput
│   ├── EventAnalytics.tsx      # Event type breakdown
│   ├── RulesList.tsx           # Rule management table
│   ├── RuleEditor.tsx          # Create/edit rules form
│   ├── RuleDetails.tsx         # Single rule view
│   ├── SettingsNotifications.tsx # Notification channels
│   ├── AlertsAndAnomalies.tsx  # Alert history
│   ├── PerformanceMetrics.tsx  # Latency analysis
│   ├── ErrorAnalysis.tsx       # Error tracking
│   ├── SourceAnalytics.tsx     # Coming soon
│   └── UserAnalytics.tsx       # Coming soon
│
├── components/
│   ├── layout/
│   │   ├── DashboardLayout.tsx # Sidebar + Header wrapper
│   │   ├── Sidebar.tsx         # Navigation
│   │   ├── Header.tsx          # Top bar
│   │   └── ConnectionStatus.tsx # WebSocket indicator
│   ├── cards/
│   │   ├── HeroMetricCard.tsx
│   │   ├── SystemHealthBadge.tsx
│   │   ├── QuickStatsGrid.tsx
│   │   └── ...
│   ├── charts/
│   │   ├── ThroughputChart.tsx
│   │   ├── EventsOverTimeChart.tsx
│   │   ├── CircularGauge.tsx
│   │   └── ...
│   ├── alerts/
│   │   ├── RecentAlertsPanel.tsx
│   │   └── ActiveAnomaliesAlert.tsx
│   └── rules/
│       └── RuleTestModal.tsx
│
├── types/
│   ├── metrics.ts              # ComprehensiveMetrics type
│   ├── rules.ts                # Rule DTOs
│   ├── notifications.ts        # Channel DTOs
│   └── websocket.ts            # Connection state
│
└── utils/
    └── api/
        ├── rules.ts            # Rule API calls
        └── notifications.ts    # Notification API calls
```

### WebSocket Connection

```typescript
// useWebSocket.ts
const stompClient = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
  heartbeatIncoming: 4000,
  heartbeatOutgoing: 4000,
  reconnectDelay: 3000
});

stompClient.onConnect = () => {
  // Subscribe to metrics topic
  stompClient.subscribe('/topic/metrics', (message) => {
    const metrics: ComprehensiveMetrics = JSON.parse(message.body);
    setMetrics(metrics);
  });
  
  // Request initial metrics
  stompClient.publish({
    destination: '/app/subscribe',
    body: JSON.stringify({ action: 'subscribe' })
  });
};
```

### Metrics Update Frequency

- **WebSocket push**: Every 1 second
- **Metrics calculation**: Real-time (on each event)
- **Dashboard rendering**: React state update on new message

---

## API Reference

### Event Ingestion APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/events` | Ingest single event |
| POST | `/api/v1/events/batch` | Batch ingestion (planned) |
| GET | `/api/v1/events` | List events (paginated) |
| GET | `/api/v1/events/{eventId}` | Get event by ID |
| GET | `/api/v1/events/type/{type}` | Get events by type |
| GET | `/api/v1/events/test` | API health check |

### Rule Management APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/rules` | Create new rule |
| GET | `/api/v1/rules` | List all rules |
| GET | `/api/v1/rules/{id}` | Get rule by ID |
| PUT | `/api/v1/rules/{id}` | Update rule |
| DELETE | `/api/v1/rules/{id}` | Delete rule |
| POST | `/api/v1/rules/{id}/enable` | Enable rule |
| POST | `/api/v1/rules/{id}/disable` | Disable rule |
| POST | `/api/v1/rules/{id}/archive` | Archive rule |
| GET | `/api/v1/rules/active` | List active rules |
| GET | `/api/v1/rules/type/{type}` | Filter by rule type |
| GET | `/api/v1/rules/status/{status}` | Filter by status |
| GET | `/api/v1/rules/search?q=` | Search rules |
| GET | `/api/v1/rules/statistics` | Get counts |
| POST | `/api/v1/rules/test` | Test rule configuration |
| POST | `/api/v1/rules/test/{id}` | Test existing rule |

### Notification APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/notifications/channels` | Create channel |
| GET | `/api/v1/notifications/channels` | List channels |
| PUT | `/api/v1/notifications/channels/{id}` | Update channel |
| DELETE | `/api/v1/notifications/channels/{id}` | Delete channel |
| POST | `/api/v1/notifications/channels/{id}/test` | Test channel |
| GET | `/api/v1/notifications/logs` | List notification logs |

### Metrics APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/metrics` | Get current metrics |
| WebSocket | `/topic/metrics` | Real-time metrics stream |
| WebSocket | `/app/subscribe` | Subscribe to metrics |

---

## Database Schema

### Core Tables

```sql
-- Events table
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) UNIQUE NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    source VARCHAR(255) NOT NULL,
    user_id VARCHAR(255),
    severity VARCHAR(50),
    timestamp TIMESTAMP NOT NULL,
    processing_latency_ms BIGINT,
    tags JSONB,
    metadata JSONB,
    is_error BOOLEAN DEFAULT FALSE
);

-- Alert rules table
CREATE TABLE alert_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    rule_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    rule_config JSONB NOT NULL,
    generated_drl TEXT NOT NULL,
    drl_hash VARCHAR(255),
    severity VARCHAR(50) NOT NULL,
    priority INTEGER DEFAULT 0,
    notification_channels TEXT[],
    notification_config JSONB,
    suppression_window_minutes INTEGER DEFAULT 30,
    max_alerts_per_hour INTEGER DEFAULT 10,
    created_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    last_triggered_at TIMESTAMP,
    trigger_count INTEGER DEFAULT 0,
    version INTEGER DEFAULT 1
);

-- Alert history table
CREATE TABLE alert_history (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT REFERENCES alert_rules(id),
    rule_name VARCHAR(255) NOT NULL,
    rule_version INTEGER,
    severity VARCHAR(50) NOT NULL,
    priority INTEGER,
    message TEXT NOT NULL,
    description TEXT,
    threshold_value DOUBLE PRECISION,
    actual_value DOUBLE PRECISION,
    status VARCHAR(50) NOT NULL,
    triggered_at TIMESTAMP NOT NULL,
    acknowledged_at TIMESTAMP,
    acknowledged_by VARCHAR(255),
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(255),
    context JSONB,
    notifications_sent JSONB
);

-- Notification channels table
CREATE TABLE notification_channels (
    id BIGSERIAL PRIMARY KEY,
    channel_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    config JSONB NOT NULL,
    rate_limit_per_minute INTEGER,
    rate_limit_per_hour INTEGER,
    last_used_at TIMESTAMP,
    total_sent INTEGER DEFAULT 0,
    total_failed INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255)
);

-- Notification logs table
CREATE TABLE notification_logs (
    id BIGSERIAL PRIMARY KEY,
    alert_id BIGINT,
    channel_id BIGINT REFERENCES notification_channels(id),
    channel_type VARCHAR(50) NOT NULL,
    recipient VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    sent_at TIMESTAMP,
    subject TEXT,
    message TEXT,
    response_code INTEGER,
    response_body TEXT,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    delivery_time_ms BIGINT,
    created_at TIMESTAMP NOT NULL
);
```

---

## Deployment

### Docker Compose Setup

```yaml
services:
  postgres:        # PostgreSQL 14
  zookeeper:       # Kafka coordination
  kafka:           # Message queue
  kafka-ui:        # Kafka visualization (optional)
  springboot:      # Java backend
  dashboard:       # React frontend (dev mode)
```

### Quick Start

```bash
# Clone and start
git clone https://github.com/tusharkhatriofficial/eventara.git
cd eventara
docker compose up --build -d

# Wait ~30 seconds, then access:
# Dashboard: http://localhost:5173
# API:       http://localhost:8080
# Kafka UI:  http://localhost:8090

# Send test event
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{"eventType":"user.login","source":"auth-service","userId":"user_1","severity":"INFO"}'
```

---

## What's Working Now

### ✅ Fully Implemented

1. **Event Ingestion Pipeline**
   - REST API event submission
   - Kafka message queue (async processing)
   - PostgreSQL persistence
   - Event deduplication

2. **Real-Time Analytics**
   - 28 metric types tracked
   - Throughput calculations (per second/minute/hour/day)
   - Latency percentiles (P50, P95, P99)
   - Error rate tracking
   - User activity tracking
   - Time window metrics (1min, 5min, 15min, 1h, 24h)

3. **WebSocket Dashboard**
   - 1-second push updates
   - Connection state management
   - Auto-reconnection (up to 10 attempts)
   - All charts and metrics live-updating

4. **Threshold Rules**
   - Full CRUD API
   - Dynamic DRL generation
   - Time-windowed evaluation
   - Rule testing before save
   - Enable/disable/archive lifecycle

5. **Alert Handling**
   - Alert creation on rule trigger
   - Suppression window (prevent duplicates)
   - Max alerts per hour limit
   - Alert history persistence

6. **Webhook Notifications**
   - Channel configuration
   - Rate limiting (per minute/hour)
   - Delivery logging
   - Channel testing

7. **Premium Dashboard UI**
   - Modern glass morphism design
   - Responsive layout (mobile/desktop)
   - Animated components
   - Dark theme sidebar

---

## Roadmap: What's Coming

### Phase 3: Advanced Rule Types
- [ ] Pattern rules (event sequences)
- [ ] Anomaly detection (ML-based)
- [ ] CEP rules (complex event processing)

### Phase 4: Additional Notifications
- [ ] Email notifications (HTML templates)
- [ ] Slack integration
- [ ] SMS via Twilio
- [ ] PagerDuty integration

### Phase 5: Enterprise Features
- [ ] User authentication (OAuth2/OIDC)
- [ ] Role-based access control
- [ ] Multi-tenancy
- [ ] SSO support

### Phase 6: Scaling & Performance
- [ ] Redis caching layer
- [ ] TimescaleDB for time-series
- [ ] Horizontal scaling (Kubernetes)
- [ ] Prometheus + Grafana monitoring

---

## Market Opportunity

### The Problem
Managed observability platforms like Datadog, Splunk, and New Relic charge $200-2000+/month for monitoring. Startups and mid-sized companies either:
1. Pay excessive amounts
2. Build hacky internal solutions
3. Go without real-time monitoring

### Our Solution
Eventara provides enterprise-grade observability at zero cost:
- Self-hosted = full data control
- Open source = no vendor lock-in
- Modern stack = easy to extend
- Real-time = sub-second insights

### Target Customers
1. **Startups**: Need monitoring but can't afford $500+/month
2. **Mid-sized tech companies**: Want customizable solutions
3. **Enterprises**: Require data sovereignty compliance
4. **IoT companies**: Need real-time device monitoring

---

## Technical Differentiators

1. **Drools Rule Engine**: Industrial-strength rule execution, not just simple threshold comparisons
2. **Kafka-First Architecture**: True event streaming, not request-response polling
3. **WebSocket Dashboard**: Real-time without refresh, not periodic API calls
4. **Extensible Design**: Clean module boundaries, easy to add new rule types/channels
5. **Developer Experience**: Docker Compose up and running in 30 seconds

---

## Team & Contact

**Developer**: Tushar Khatri
- Email: hello@tusharkhatri.in
- GitHub: [tusharkhatriofficial/eventara](https://github.com/tusharkhatriofficial/eventara)

---

*Report Generated: January 28, 2026*
*Eventara Version: v0.1.0*

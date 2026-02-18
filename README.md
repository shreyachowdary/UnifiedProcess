# Unified Notification Platform

Production-style notification platform built with **Spring Boot 3.x (Java 17)** supporting **EMAIL via SMTP** and **SMS via SMSC/SMPP**. Uses **Kafka** for async processing, **MongoDB + PostgreSQL** for persistence, and **Azure DevOps** for CI/CD.

## Architecture

```
                    ┌─────────────────────────────────────────────────────────┐
                    │                    REST API (8080)                       │
                    │  POST /api/v1/notifications  →  202 Accepted + requestId │
                    │  GET  /api/v1/notifications/{id}                         │
                    │  GET  /api/v1/metrics/summary                            │
                    └──────────────────────────┬──────────────────────────────┘
                                               │
                    ┌──────────────────────────▼──────────────────────────────┐
                    │  Validate → Persist (MongoDB) → Publish Kafka → Return     │
                    │  Idempotency check (clientId + idempotencyKey)             │
                    └──────────────────────────┬──────────────────────────────┘
                                               │
                    ┌──────────────────────────▼──────────────────────────────┐
                    │              Kafka: notifications.requested             │
                    └──────────────────────────┬──────────────────────────────┘
                                               │
                    ┌──────────────────────────▼──────────────────────────────┐
                    │              Worker (8081) - Kafka Consumer               │
                    │  Route → SMTP (email) / SMPP (sms) → Update status       │
                    │  Retry (notifications.retry) → DLQ (notifications.dlq)    │
                    └─────────────────────────────────────────────────────────┘
                                               │
                    ┌──────────────────────────▼──────────────────────────────┐
                    │  MongoDB: notifications, delivery_logs, idempotency     │
                    │  PostgreSQL: templates, clients, routing_rules            │
                    └─────────────────────────────────────────────────────────┘
```

## Key Features

| Feature | Implementation |
|---------|----------------|
| **Async processing** | API publishes to Kafka and returns 202 immediately; Worker consumes and sends |
| **SMTP (Email)** | Spring Mail `JavaMailSender` |
| **SMS (SMSC/SMPP)** | `SmsProvider` interface + `MockSmppSimulator` (swap to real jSMPP/OpenSMPP) |
| **Retries** | Exponential backoff via retry topic; max 3 attempts |
| **DLQ** | Failed messages after max retries → `notifications.dlq` |
| **Idempotency** | Same `clientId` + `idempotencyKey` returns existing `requestId` |
| **Delivery tracking** | Status: QUEUED → PROCESSING → SENT → DELIVERED / FAILED |

## How to Run Locally

### 1. Start infrastructure (Kafka, MongoDB, PostgreSQL)

```bash
cd infra
docker-compose up -d
```

Wait ~30 seconds for Kafka to be ready. PostgreSQL runs `init.sql` on first start (creates tables, indexes, seed data).

### 2. Run full stack with Docker (API + Worker)

```bash
cd infra
docker-compose up --build
```

This builds and runs both `notification-api` and `notification-worker`.

### 3. Or run API/Worker from IDE

Start infra only:

```bash
docker-compose -f infra/docker-compose.infra-only.yml up -d
```

Then run `NotificationApiApplication` and `NotificationWorkerApplication` from your IDE. Ensure env vars point to `localhost` (default in `application.yml`).

### 4. Configure SMTP (for real email)

Set environment variables:

```bash
export SMTP_HOST=smtp.gmail.com
export SMTP_PORT=587
export SMTP_USERNAME=your-email@gmail.com
export SMTP_PASSWORD=your-app-password
```

For local dev without real SMTP, the API still accepts requests; the worker will fail on send unless you use a test SMTP (e.g. MailHog) or mock.

## Example cURL Requests

### Create notification (returns 202 + requestId)

```bash
curl -X POST http://localhost:8080/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "client1",
    "channel": "EMAIL",
    "to": "user@example.com",
    "templateId": "welcome-email",
    "variables": {"name": "John"},
    "idempotencyKey": "req-001"
  }'
```

Response: `{"requestId":"550e8400-e29b-41d4-a716-446655440000"}`

### Get status

```bash
curl http://localhost:8080/api/v1/notifications/{requestId}
```

### Metrics summary

```bash
curl http://localhost:8080/api/v1/metrics/summary
```

Response: `{"total":100,"successRate":95.5,"avgLatencyMs":120.3,"lastHourVolume":42}`

### Idempotent request (same key returns same requestId)

```bash
curl -X POST http://localhost:8080/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "client1",
    "channel": "EMAIL",
    "to": "user@example.com",
    "templateId": "welcome-email",
    "variables": {"name": "John"},
    "idempotencyKey": "req-001"
  }'
```

Returns the same `requestId` as the first request with `idempotencyKey: req-001`.

## Kafka Async – Why It Improves API Response Time

**Before (synchronous):** API receives request → validates → sends email/SMS via provider → waits for provider response (often 100–500ms) → returns 200. Total latency = validation + network to provider.

**After (async with Kafka):** API receives request → validates → persists to MongoDB → publishes to Kafka → returns 202. Total latency = validation + persist + Kafka publish (~10–50ms). The actual send happens in the Worker asynchronously.

**Result:** API responds in ~20–50ms instead of 100–500ms, improving perceived performance and allowing the API to handle more concurrent requests without blocking on external providers.

## Delivery Success Rate

Success rate is computed as:

```
successRate = (count of notifications with status SENT or DELIVERED) / total notifications * 100
```

- **Total:** All notifications in MongoDB.
- **Success:** Notifications whose final status is `SENT` or `DELIVERED`.
- **avgLatencyMs:** Average of `latencyMs` from `delivery_logs` collection.

## Query Optimization & Indexes

### MongoDB

| Collection | Index | Purpose |
|------------|-------|---------|
| `notifications` | `requestId` (unique) | Fast lookup by requestId for status checks |
| `notifications` | `(clientId, createdAt)` compound | List notifications by client with paging |
| `notifications` | `(status, createdAt)` compound | Filter by status, time-range queries |
| `idempotency_keys` | `(clientId, idempotencyKey)` unique | Idempotency check |
| `delivery_logs` | `requestId` | Fetch logs for a notification |

**Projection example (avoid fetching large `variables` when listing):**

```java
@Query(value = "{ 'requestId': ?0 }", fields = "{ 'requestId': 1, 'status': 1, 'clientId': 1, 'channel': 1, 'to': 1, 'templateId': 1, 'createdAt': 1, 'updatedAt': 1 }")
Optional<NotificationDocument> findByRequestIdProjected(String requestId);
```

### PostgreSQL

| Table | Index | Purpose |
|-------|-------|---------|
| `templates` | `idx_template_id` | Lookup by templateId |
| `clients` | `idx_client_id` | Lookup by clientId |
| `routing_rules` | `idx_routing_client_channel` | Route by client + channel |

### Before/After Example (Estimated)

**Before:** Query `notifications` by `clientId` without index → full collection scan. For 100k docs: ~200–500ms.

**After:** Compound index `(clientId, createdAt)` → index scan. Same query: ~5–20ms.

## Swapping to Real SMSC (SMPP)

The `SmsProvider` interface in `sms-simulator` is designed for easy replacement:

1. Add jSMPP or OpenSMPP dependency to `notification-worker`.
2. Create `JsmppSmsProvider implements SmsProvider` that connects to your SMSC and sends via SMPP.
3. Register it as a `@Bean` in `WorkerConfig` (or remove `@ConditionalOnMissingBean` and provide your bean).
4. The `MockSmppSimulator` will not load when your bean is present.

## CI/CD (Azure DevOps)

`azure-pipelines.yml`:

1. **Build:** Maven `clean verify` (compile + tests).
2. **Docker:** Builds `notification-api` and `notification-worker` images.
3. **Optional:** Push to Azure Container Registry (configure `DOCKER_REGISTRY`, `DOCKER_SERVICE_CONNECTION`).

**Variables to configure:**

- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` – PostgreSQL.
- `MONGODB_URI` – MongoDB.
- `KAFKA_BOOTSTRAP_SERVERS` – Kafka broker.
- `SMTP_HOST`, `SMTP_USERNAME`, `SMTP_PASSWORD` – SMTP credentials (use Azure DevOps variable groups / secrets).

## Project Structure

```
.
├── notification-api/          # REST API, Kafka producer
├── notification-worker/       # Kafka consumer, SMTP/SMPP adapters
├── sms-simulator/             # SmsProvider interface + MockSmppSimulator
├── infra/
│   ├── docker-compose.yml     # Full stack (API, Worker, Kafka, Mongo, Postgres)
│   ├── docker-compose.infra-only.yml
│   └── init.sql
├── azure-pipelines.yml
└── README.md
```

## Observability

- **Structured logs:** JSON format in `application.yml`.
- **Actuator:** `/actuator/health`, `/actuator/info`, `/actuator/metrics`.
- **Metrics:** `/api/v1/metrics/summary` for total, successRate, avgLatency, lastHourVolume.

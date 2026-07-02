# Governance Policy Management System

A microservices-based system for managing the full lifecycle of governance policies — from creation through approval — with a complete audit trail driven by Kafka events.

---

## Architecture Overview

```
┌─────────────────────┐        Kafka         ┌─────────────────────┐
│  governance-service │ ──────────────────▶  │    audit-service    │
│     (port 8080)     │  governance-events   │     (port 8081)     │
│                     │       topic          │                     │
│  - Policy CRUD      │                      │  - Consumes events  │
│  - Status workflow  │                      │  - Stores audit log │
│  - Publishes events │                      │  - Query audit logs │
│                     │                      │                     │
│  PostgreSQL         │                      │  PostgreSQL         │
│  governance_db      │                      │  audit_db           │
│  (port 5432)        │                      │  (port 5433)        │
└─────────────────────┘                      └─────────────────────┘
```

---

## Services

| Service | Port | Database | Role |
|---|---|---|---|
| governance-service | 8080 | governance_db (5432) | Manages policies and publishes events |
| audit-service | 8081 | audit_db (5433) | Consumes events and records audit logs |
| Kafka | 9092 | — | Message broker between services |
| Zookeeper | 2181 | — | Kafka coordinator |

---

## Policy Lifecycle (Status Flow)

```
  POST /policies
       │
       ▼
    DRAFT ──────────────────────────────────────▶ (only state at creation)
       │
       │  POST /policies/{id}/submit
       ▼
  PENDING_APPROVAL
       │
       ├──── POST /policies/{id}/approve ────▶ APPROVED
       │
       └──── POST /policies/{id}/reject  ────▶ REJECTED
```

Every transition publishes a Kafka event to the `governance-events` topic, which the audit-service consumes and stores.

---

## Getting Started

### Prerequisites

- Java 21
- Maven
- Docker Desktop

### 1. Clone the repository

```bash
git clone <repository-url>
cd governance-policy-management-system
```

### 2. Start infrastructure (PostgreSQL + Kafka)

```bash
docker-compose up -d
```

This starts:
- `postgres-governance` on port `5432` → `governance_db`
- `postgres-audit` on port `5433` → `audit_db`
- `zookeeper` on port `2181`
- `kafka` on port `9092`

Verify containers are running:
```bash
docker ps
```

### 3. Start governance-service

```bash
cd governance-service
mvnw spring-boot:run
```

Runs on `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui.html`

### 4. Start audit-service

Open a new terminal:
```bash
cd audit-service
mvnw spring-boot:run
```

Runs on `http://localhost:8081`  
Swagger UI: `http://localhost:8081/swagger-ui.html`

---

## API Reference

### governance-service — `http://localhost:8080`

#### Create a policy
```http
POST /policies
Content-Type: application/json

{
  "title": "Data Retention Policy",
  "description": "All user data must be retained for 7 years.",
  "createdBy": "alice"
}
```
Response: `201 Created` — policy created in `DRAFT` status.

#### Get all policies
```http
GET /policies
```

#### Get policy by ID
```http
GET /policies/{id}
```

#### Submit for approval
```http
POST /policies/{id}/submit
```
Transitions: `DRAFT → PENDING_APPROVAL`

#### Approve a policy
```http
POST /policies/{id}/approve?actor=manager
```
Transitions: `PENDING_APPROVAL → APPROVED`

#### Reject a policy
```http
POST /policies/{id}/reject?actor=manager
```
Transitions: `PENDING_APPROVAL → REJECTED`

---

### audit-service — `http://localhost:8081`

#### Get all audit logs
```http
GET /audit-logs
```

#### Get audit logs for a specific policy
```http
GET /audit-logs/policy/{policyId}
```
Returns records ordered by timestamp descending (newest first).

---

## Full Workflow Example

```bash
# 1. Create a policy
curl -X POST http://localhost:8080/policies \
  -H "Content-Type: application/json" \
  -d '{"title":"Security Policy","description":"MFA required for all users.","createdBy":"alice"}'

# 2. Submit for approval (use the id returned above, e.g. 1)
curl -X POST http://localhost:8080/policies/1/submit

# 3. Approve the policy
curl -X POST "http://localhost:8080/policies/1/approve?actor=manager"

# 4. Check the audit trail
curl http://localhost:8081/audit-logs/policy/1
```

Expected audit log response:
```json
[
  { "id": 3, "eventType": "policy-approved", "policyId": 1, "actor": "manager", "timestamp": "..." },
  { "id": 2, "eventType": "policy-submitted", "policyId": 1, "actor": "alice",   "timestamp": "..." },
  { "id": 1, "eventType": "policy-created",   "policyId": 1, "actor": "alice",   "timestamp": "..." }
]
```

---

## Project Structure

```
governance-policy-management-system/
├── docker-compose.yml               # Infrastructure setup
├── governance-service/              # Policy management service
│   └── src/main/java/.../
│       ├── config/                  # Kafka producer configuration
│       ├── controller/              # REST endpoints (PolicyController)
│       ├── dto/                     # CreatePolicyRequest, PolicyResponse
│       ├── entity/                  # Policy, PolicyStatus (enum)
│       ├── event/                   # GovernanceEvent (Kafka payload)
│       ├── exception/               # PolicyNotFoundException, InvalidStatusTransitionException, GlobalExceptionHandler
│       ├── kafka/                   # GovernanceEventProducer
│       ├── repository/              # PolicyRepository
│       └── service/                 # PolicyService (business logic)
│
└── audit-service/                   # Audit trail service
    └── src/main/java/.../
        ├── config/                  # Kafka consumer configuration
        ├── controller/              # REST endpoints (AuditLogController)
        ├── dto/                     # AuditLogResponse
        ├── entity/                  # AuditLog
        ├── event/                   # GovernanceEvent (Kafka payload mirror)
        ├── kafka/                   # GovernanceEventConsumer
        ├── repository/              # AuditLogRepository
        └── service/                 # AuditLogService (save + query)
```

---

## Key Design Decisions

**Event-driven audit trail** — The audit-service never calls governance-service directly. It only listens to Kafka. This means the two services are fully decoupled: governance-service doesn't know or care whether audit-service is running.

**Fire-and-forget Kafka publishing** — In governance-service, Kafka publish failures are logged but do not roll back the policy transaction. Policy state is the source of truth; audit records are best-effort.

**Status guard on transitions** — Every state change validates the current status before proceeding. Attempting an invalid transition (e.g. approving a `DRAFT`) returns `409 Conflict` with a clear message.

**Separate databases** — Each service owns its own PostgreSQL database. governance-service uses `governance_db`; audit-service uses `audit_db`. No shared schema.

---

## Error Responses

| Scenario | HTTP Status | Example message |
|---|---|---|
| Policy not found | `404 Not Found` | `Policy not found with id: 5` |
| Invalid status transition | `409 Conflict` | `Cannot transition policy from DRAFT to APPROVED` |
| Validation failure | `400 Bad Request` | `{ "title": "Title is required" }` |
| Unexpected error | `500 Internal Server Error` | Error message + server log |

---

## Stopping the System

```bash
# Stop all infrastructure containers
docker-compose down

# Stop and remove volumes (wipes all database data)
docker-compose down -v
```

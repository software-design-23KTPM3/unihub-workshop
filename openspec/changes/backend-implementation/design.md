## Context

The UniHub Workshop system needs to replace a manual, error-prone Google Form-based registration process. The primary challenge is handling high concurrent traffic (12k users in 10 minutes) while ensuring zero seat over-booking and reliable payments.

## Goals / Non-Goals

**Goals:**
- Implement a scalable, event-driven microservices architecture.
- Ensure strong consistency for seat reservations using Redis distributed locks/counters.
- Decouple heavy tasks (notifications, payments, AI processing, CSV sync) via RabbitMQ.
- Provide offline-first check-in capabilities for mobile users.
- Secure the system using RBAC with Keycloak and API Gateway.

**Non-Goals:**
- Integration with real payment gateways (mock/sandbox only).
- Deployment to production cloud infrastructure (Docker-only).
- Native mobile apps (PWA/Responsive Web only).

## Decisions

### 1. Architectural Pattern: Event-Driven Microservices
- **Decision**: Use Spring Boot for services and RabbitMQ for inter-service communication.
- **Rationale**: Decouples the fast "Sync" luồng (registration availability) from the slow "Async" luồng (payment processing, notifications).
- **Alternatives**: Monolithic architecture (risk of hanging during spikes), Kafka (overly complex for this scale).

### 2. Concurrency Control: Redis Atomic Counter
- **Decision**: Use `DECR` on a Redis key to reserve seats before persisting to DB.
- **Rationale**: Redis handles thousands of atomic operations per second on RAM, avoiding DB connection pool exhaustion.
- **Alternatives**: DB Pessimistic Locking (too slow/blocking), Optimistic Locking (too many retries/failures under load).

### 3. Data Persistence: PostgreSQL + Redis
- **Decision**: Use PostgreSQL for persistent, ACID-compliant data and Redis for transient, high-speed data.
- **Rationale**: Ensures financial and registration data integrity while maintaining high response speeds.

### 4. Background Processing: Worker Pattern
- **Decision**: Registration Worker, Payment Worker, Notification Worker, Integration Worker.
- **Rationale**: Sanitize spike loads and prevent external system failures from affecting user experience.

## Risks / Trade-offs

- **[Risk] Seat Over-booking** → **Mitigation**: Redis atomic `DECR` and distributed locks.
- **[Risk] Gateway Failure (SPOF)** → **Mitigation**: Dual Nginx setup with Keepalived.
- **[Risk] Double Charging** → **Mitigation**: Client-side session storage + Backend `idempotency_key` (Unique constraint).
- **[Risk] Offline Data Scanned Multiple Times** → **Mitigation**: Conflict resolution logic in Batch Sync process.
- **[Risk] DB Load Spike** → **Mitigation**: Workers perform Bulk Writes instead of individual inserts.

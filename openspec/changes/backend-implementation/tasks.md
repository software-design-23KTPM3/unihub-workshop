## 1. Infrastructure Setup

- [x] 1.1 Create `docker-compose.yml` for PostgreSQL, Redis (rate & lock), RabbitMQ, and Keycloak
- [x] 1.2 Initialize PostgreSQL with schema (Workshops, Registrations, Transactions, Students, SyncLogs, Notifications)
- [x] 1.3 Configure Keycloak with UniHub realm, clients, and roles (STUDENT, ORGANIZER, CHECKIN_STAFF)
- [x] 1.4 Setup Nginx API Gateway with Lua scripts for JWT verification and Redis-based Rate Limiting

## 2. Core Backend Development - Foundation

- [x] 2.1 Initialize Spring Boot project with required dependencies (Web, Data JPA, Redis, RabbitMQ, Security, Resilience4j)
- [x] 2.2 Implement JPA Entities and Repositories for the entire system
- [x] 2.3 Configure Security with OAuth2 Resource Server and JWT decoder (RS256)
- [x] 2.4 Setup base Controller/Service structure with Global Exception Handling

## 3. Workshop Management Capability

- [x] 3.1 Implement CRUD APIs for Workshops with role-based security (@PreAuthorize)
- [x] 3.2 Implement Workshop Service logic: Database persistence + immediate Redis cache invalidation
- [x] 3.3 Implement Redis initialization logic for `workshop_slots` upon creation/update

## 4. High-Concurrency Booking Capability

- [x] 4.1 Implement `POST /api/registrations` with Idempotency-Key check and Redis `DECR` logic
- [x] 4.2 Implement Registration Service: Create PENDING record and publish event to RabbitMQ
- [x] 4.3 Configure RabbitMQ Exchange and Queues for registration and cancellation events

## 5. Background Workers Implementation

- [x] 5.1 Implement Registration Worker: Consume registration events and perform bulk inserts/updates to PostgreSQL
- [x] 5.2 Implement Notification Worker: Consume events, generate QR codes, and simulate Email/Push delivery (Strategy Pattern)
- [x] 5.3 Implement Payment Worker: Mock external gateway calls with Circuit Breaker and handle idempotency
- [x] 5.4 Implement Integration Worker: Scheduled CSV processing with streaming/chunking and AI summary PDF processing

## 6. Offline Check-in Capability

- [x] 6.1 Implement `POST /api/v1/sync` for bulk check-in synchronization from mobile devices
- [x] 6.2 Implement sync conflict resolution and original timestamp preservation logic

## 7. AI Summary Capability

- [x] 7.1 Implement PDF upload and storage logic
- [x] 7.2 Implement worker logic for PDF text extraction (PDFBox) and AI summary generation (Mock/Google AI Studio)

## 8. Verification and Testing

- [x] 8.1 Verify project compilation and basic service functionality
- [ ] 8.2 Verify Idempotency and Rate Limiting at the API Gateway level
- [ ] 8.3 Conduct end-to-end tests for a free workshop booking flow
- [ ] 8.4 Conduct end-to-end tests for a paid workshop booking flow including webhook simulation
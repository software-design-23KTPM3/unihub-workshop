## Why

To transform the current manual workshop registration process (Google Forms, manual emails, physical check-ins) into a robust, high-performance, and automated digital system (UniHub Workshop). The system must handle high concurrent traffic (12k users in 10 mins) and ensure data consistency, particularly for limited seat availability.

## What Changes

- Implementation of a distributed backend architecture using Spring Boot.
- Real-time seat reservation system with strong consistency using Redis.
- Asynchronous processing for payments, notifications, and AI analysis using RabbitMQ.
- Role-Based Access Control (RBAC) integration with Keycloak.
- Offline-first check-in capability for mobile users.
- Automated student data synchronization from CSV files.
- AI-powered workshop summarization from PDF uploads.

## Capabilities

### New Capabilities
- `auth`: Centralized identity management and RBAC using Keycloak and JWT.
- `workshop-management`: Management of workshops, rooms, and schedules.
- `booking`: High-concurrency seat reservation and registration handling.
- `payment`: Transaction processing with idempotency and circuit breaker protection.
- `checkin`: Mobile-based QR code check-in with offline synchronization.
- `notification`: Asynchronous email and app notifications.
- `csv-sync`: Scheduled student data import pipeline with validation.
- `ai-summary`: Automatic PDF-to-summary processing using AI models.

### Modified Capabilities
- None

## Impact

- Architecture: Event-driven microservices with API Gateway.
- Backend: Spring Boot application with multiple workers.
- Infrastructure: PostgreSQL, Redis, RabbitMQ, Keycloak, Nginx.
- Client: New Web and Mobile applications for registration and check-in.

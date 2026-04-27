## ADDED Requirements

### Requirement: Workshop Lifecycle Management
The system SHALL allow users with role ORGANIZER or ADMIN to create, update, and cancel workshops.

#### Scenario: Successful Workshop Creation
- **WHEN** an authorized user sends valid workshop details (name, speaker, room, max_seats, time)
- **THEN** system SHALL create a record in PostgreSQL, initialize a Redis slot counter, and invalidate relevant caches

#### Scenario: Validating Capacity Update
- **WHEN** an organizer attempts to reduce `max_seats` below the current number of successful registrations
- **THEN** system MUST return 409 Conflict and reject the change

### Requirement: Workshop Cancellation and Notification
The system SHALL allow authorized users to cancel a workshop, which triggers automated notifications to registered students.

#### Scenario: Successful Cancellation
- **WHEN** an authorized user cancels a workshop
- **THEN** system SHALL update the status to CANCELLED in PostgreSQL, delete the Redis slot counter, and publish a `workshop.cancelled` event to RabbitMQ

### Requirement: Cache Consistency for Workshops
The system SHALL ensure that any data modification (create/update/cancel) triggers immediate cache invalidation.

#### Scenario: Immediate Cache Invalidation
- **WHEN** a workshop is updated in the database
- **THEN** the system SHALL delete `workshop_details:{id}` and `workshop_list` keys from Redis

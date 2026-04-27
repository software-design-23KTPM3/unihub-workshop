## ADDED Requirements

### Requirement: Concurrent Seat Reservation
The system SHALL use an atomic Redis counter (`DECR`) to manage seat availability under high concurrent load.

#### Scenario: Contested Last Seat
- **WHEN** multiple users attempt to book the last available seat simultaneously
- **THEN** only one user SHALL succeed (Redis returns >= 0) and others MUST receive a "Workshop sold out" error (Redis returns < 0)

### Requirement: Request Idempotency
The system SHALL ensure that duplicate booking requests from the same user are processed exactly once using an `Idempotency-Key`.

#### Scenario: Retried Request
- **WHEN** a client retries a booking request with the same `Idempotency-Key` within 24 hours
- **THEN** the system SHALL return the existing transaction status without creating a duplicate registration or deducting another seat

### Requirement: Registration Branching (Free vs Paid)
The system SHALL distinguish between free workshops (immediate confirmation) and paid workshops (transfer to payment flow).

#### Scenario: Free Workshop Booking
- **WHEN** a student books a free workshop
- **THEN** the system SHALL immediately set status to SUCCESS and publish a registration event

#### Scenario: Paid Workshop Booking
- **WHEN** a student books a paid workshop
- **THEN** the system SHALL set the status to PENDING and initiate the payment flow

### Requirement: Asynchronous Confirmation
The system SHALL process post-registration tasks (QR code generation, email, push notifications) asynchronously using RabbitMQ.

#### Scenario: Successful Registration Event
- **WHEN** a registration status becomes SUCCESS
- **THEN** a `student.registered.successfully` event MUST be published to RabbitMQ for workers to process notifications and QR codes

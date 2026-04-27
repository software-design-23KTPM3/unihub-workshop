## ADDED Requirements

### Requirement: Asynchronous Multi-Channel Notification
The system SHALL support sending notifications via multiple channels (Email, Push, In-App) asynchronously using a worker pattern and RabbitMQ.

#### Scenario: Successful Notification Dispatch
- **WHEN** a `NotificationEvent` is published to RabbitMQ
- **THEN** the Notification Worker SHALL consume the event, select the appropriate strategy (e.g., `EmailNotificationStrategy`), and deliver the message without blocking the main application flow

### Requirement: Notification Retries and Dead Letter Queue (DLQ)
The system SHALL implement a retry mechanism with exponential backoff for failed notification deliveries.

#### Scenario: Delivery Failure Retry
- **WHEN** a notification delivery fails due to a transient error (e.g., SMTP timeout)
- **THEN** the system SHALL retry up to 3 times with exponential backoff before moving the message to a Dead Letter Queue (DLQ)

### Requirement: Extensible Notification Architecture
The system SHALL use the Strategy Pattern to allow adding new notification channels (e.g., Telegram, SMS) without modifying core business logic.

#### Scenario: Adding New Channel
- **WHEN** a new notification type (e.g., TELEGRAM) and corresponding strategy class are added
- **THEN** the worker SHALL be able to route notifications to the new channel based on the event payload

### Requirement: Notification Idempotency
The system SHALL prevent duplicate notifications for the same event by tracking a unique `eventId`.

#### Scenario: Redelivered Queue Message
- **WHEN** a worker receives a message that has already been successfully processed (checked via `eventId`)
- **THEN** it SHALL ignore the message and acknowledge it to the queue

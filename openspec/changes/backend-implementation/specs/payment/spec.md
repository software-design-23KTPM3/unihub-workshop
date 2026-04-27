## ADDED Requirements

### Requirement: Idempotent Payment Processing
The system SHALL ensure that each payment request is processed exactly once using an `Idempotency-Key` and a unique `transactionId` from the payment gateway.

#### Scenario: Duplicate Webhook Delivery
- **WHEN** the payment gateway sends the same successful payment webhook multiple times
- **THEN** the system SHALL acknowledge the webhook but MUST NOT update the registration or transaction status more than once

### Requirement: Circuit Breaker for Payment Gateway
The system SHALL implement a Circuit Breaker pattern to isolate failures in external payment gateways.

#### Scenario: Payment Gateway Failure
- **WHEN** the payment gateway failure rate exceeds 50%
- **THEN** the system SHALL enter OPEN state and reject further paid booking attempts with a "Service Under Maintenance" message, while allowing free workshop bookings and other features to function normally

### Requirement: Soft-Lock Expiration (Reconciliation)
The system SHALL release reserved seats if payment is not completed within a 15-minute window.

#### Scenario: Payment Timeout
- **WHEN** a student initiates a paid booking but fails to complete payment within 15 minutes
- **THEN** the system SHALL expire the soft-lock in Redis and increment the `available_slots` counter back

### Requirement: Secure Webhook Handling
The system SHALL verify the digital signature of all incoming webhooks from the payment gateway.

#### Scenario: Invalid Webhook Signature
- **WHEN** a webhook is received with an invalid signature
- **THEN** the system SHALL reject the request and log a security warning

## ADDED Requirements

### Requirement: Offline-First QR Scanning
The Mobile App SHALL allow staff to scan QR codes and store them locally in SQLite with an `UNSYNCED` status when there is no internet connection.

#### Scenario: Scanning without Internet
- **WHEN** staff scans a valid QR code while offline
- **THEN** the system SHALL store the `registration_id` and the scan `timestamp` in the local SQLite database and display a success message within 200ms

### Requirement: Bulk Synchronization Mechanism
The system SHALL provide a bulk synchronization API to upload locally stored scan records to the server once connectivity is restored.

#### Scenario: Successful Bulk Sync
- **WHEN** the Mobile App detects internet connectivity and sends a batch of `UNSYNCED` records to `/api/v1/sync`
- **THEN** the server SHALL update the registration status to `CHECKED_IN` in PostgreSQL and return 200 OK, enabling the app to mark records as `SYNCED`

### Requirement: Time Consistency and Idempotency
The server SHALL preserve the original scan timestamp from the mobile device and handle duplicate scans of the same QR code.

#### Scenario: Duplicate Scan Handling
- **WHEN** the server receives multiple scan records for the same `registration_id`
- **THEN** it SHALL only process the first one (based on timestamp) and ignore subsequent duplicates

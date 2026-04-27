## ADDED Requirements

### Requirement: Centralized Authentication
The system SHALL use Keycloak as the central Identity Provider to manage users and issue tokens.

#### Scenario: Successful Login
- **WHEN** user provides valid credentials to Keycloak
- **THEN** system returns an Access Token (JWT) containing UserID and Roles

### Requirement: Stateless Authorization via API Gateway
The API Gateway SHALL verify the JWT signature and expiration for every incoming request using a cached Public Key.

#### Scenario: Unauthorized Request
- **WHEN** a request arrives with an expired or invalid token
- **THEN** the Gateway MUST return 401 Unauthorized immediately

### Requirement: Role-Based Access Control (RBAC)
The Backend Service SHALL enforce access control based on roles (STUDENT, ORGANIZER, CHECKIN_STAFF) embedded in the JWT.

#### Scenario: Forbidden Access
- **WHEN** a user with role STUDENT attempts to call an API restricted to ORGANIZER
- **THEN** the system MUST return 403 Forbidden

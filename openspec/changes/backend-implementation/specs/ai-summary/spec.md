## ADDED Requirements

### Requirement: Asynchronous PDF Summarization
The system SHALL process uploaded workshop PDFs asynchronously to generate summaries without blocking the workshop creation flow.

#### Scenario: Successful PDF Upload and Processing
- **WHEN** an organizer uploads a valid PDF for a workshop
- **THEN** the system SHALL return 202 Accepted, store the file, and publish a summarization event to RabbitMQ for background processing

### Requirement: PDF Content Extraction and Cleaning
The AI Summary Worker SHALL extract text from PDF files and clean it (removing HTML tags, special characters, and non-semantic whitespace) before sending it to the AI model.

#### Scenario: Text Extraction Success
- **WHEN** the worker processes a stored PDF
- **THEN** it SHALL use a PDF parsing library (e.g., Apache PDFBox) to extract text, clean it, and prepare chunks for the LLM

### Requirement: AI Model Integration (Google AI Studio)
The system SHALL integrate with an external AI API (e.g., Google AI Studio) to generate concise workshop summaries.

#### Scenario: Successful AI Summary Generation
- **WHEN** cleaned text is sent to the AI API
- **THEN** the system SHALL receive a summary, store it in PostgreSQL, and update the workshop's `summaryStatus` to COMPLETED

### Requirement: Processing Robustness and Idempotency
The system SHALL handle processing failures with retries and avoid duplicate summarization for the same file.

#### Scenario: Summarization Failure Retry
- **WHEN** the AI API call fails due to a transient error
- **THEN** the system SHALL retry up to 3 times before marking the status as FAILED in the database

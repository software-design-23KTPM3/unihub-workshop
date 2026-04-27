## ADDED Requirements

### Requirement: Nightly Scheduled CSV Student Sync
The system SHALL execute a scheduled job (Cron) every night at 2:00 AM to synchronize student data from a legacy CSV export.

#### Scenario: Successful CSV Import
- **WHEN** the sync job finds a valid CSV file (e.g., `students_YYYYMMDD.csv`)
- **THEN** it SHALL process the file in chunks (500-1000 records) to minimize RAM usage and perform bulk upserts into the PostgreSQL database

### Requirement: Memory-Efficient Processing (Streaming)
The system SHALL use streaming or chunking techniques to process large CSV files without loading the entire file into memory (OOM prevention).

#### Scenario: Large File Import
- **WHEN** a CSV file with 50,000 records is processed
- **THEN** the system SHALL process it in batches and complete the sync in less than 5 minutes while maintaining stable RAM usage

### Requirement: Robust Error Handling and Logging
The system SHALL log all synchronization activities and separate invalid records into an error file without halting the entire job.

#### Scenario: Partial Success with Errors
- **WHEN** a CSV file contains some invalid rows (e.g., missing email or malformed MSSV)
- **THEN** the system SHALL skip those rows, record them in an `error_log_syncId.csv`, and continue processing valid rows, marking the job as PARTIAL_SUCCESS in `sync_logs`

### Requirement: Idempotent Data Updates
The system SHALL use an upsert strategy (`ON CONFLICT (mssv) DO UPDATE`) to ensure that re-running the job does not create duplicate student records.

#### Scenario: Job Rerun
- **WHEN** the same CSV file is processed twice
- **THEN** no new records SHALL be created, and existing records SHALL remain updated with the same values

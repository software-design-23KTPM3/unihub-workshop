CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. Tạo tất cả các ENUM TYPE trước
CREATE TYPE workshop_status AS ENUM ('ACTIVE', 'CANCELLED');
CREATE TYPE summary_status AS ENUM ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED');
CREATE TYPE registration_status AS ENUM ('PENDING', 'SUCCESS', 'FAILED', 'CHECKED_IN');
CREATE TYPE transaction_status AS ENUM ('PENDING', 'SUCCESS', 'FAILED');
CREATE TYPE notification_type AS ENUM ('EMAIL', 'PUSH', 'IN_APP');
CREATE TYPE notification_status AS ENUM ('PENDING', 'SENT', 'FAILED');
CREATE TYPE sync_status AS ENUM ('SUCCESS', 'PARTIAL', 'FAILED', 'RUNNING');
CREATE TYPE checkin_event_status AS ENUM ('ACCEPTED', 'DUPLICATE', 'INVALID');

-- 2. Cấu hình CAST (Ép kiểu) để Hibernate nói chuyện được với Postgres Enum
CREATE CAST (varchar AS workshop_status) WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS summary_status) WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS registration_status) WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS notification_type) WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS notification_status) WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS transaction_status) WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS sync_status) WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS checkin_event_status) WITH INOUT AS IMPLICIT;

-- 3. Tạo các bảng
CREATE TABLE students (
    mssv VARCHAR(20) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    birthday VARCHAR(20),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_students_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

CREATE TABLE workshops (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    speaker VARCHAR(255),
    speaker_title VARCHAR(255),
    topic VARCHAR(255),
    room VARCHAR(100),
    room_map_text TEXT,
    tags JSONB NOT NULL DEFAULT '[]'::jsonb,
    organizer_id VARCHAR(255),
    max_seats INTEGER NOT NULL,
    available_slots INTEGER NOT NULL,
    registration_start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    registration_end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    is_paid BOOLEAN DEFAULT FALSE,
    price DECIMAL(12, 2) DEFAULT 0.00,
    status workshop_status NOT NULL DEFAULT 'ACTIVE',
    summary_text TEXT,
    summary_status summary_status NOT NULL DEFAULT 'PENDING',
    pdf_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_workshops_capacity CHECK (max_seats > 0),
    CONSTRAINT chk_workshops_available_slots CHECK (available_slots >= 0 AND available_slots <= max_seats),
    CONSTRAINT chk_workshops_registration_period CHECK (registration_end_time > registration_start_time),
    CONSTRAINT chk_workshops_registration_before_start CHECK (registration_end_time <= start_time),
    CONSTRAINT chk_workshops_time_range CHECK (end_time > start_time),
    CONSTRAINT chk_workshops_price CHECK (price >= 0),
    CONSTRAINT chk_workshops_paid_price CHECK ((is_paid = true AND price > 0) OR (is_paid = false AND price = 0))
);

CREATE TABLE registrations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id VARCHAR(20) NOT NULL REFERENCES students(mssv),
    workshop_id UUID NOT NULL REFERENCES workshops(id),
    status registration_status NOT NULL DEFAULT 'PENDING',
    qr_code VARCHAR(255) UNIQUE,
    idempotency_key UUID UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    checked_in_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_registrations_student_workshop UNIQUE (student_id, workshop_id)
);

CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    registration_id UUID NOT NULL UNIQUE REFERENCES registrations(id),
    amount DECIMAL(12, 2) NOT NULL,
    status transaction_status NOT NULL DEFAULT 'PENDING',
    idempotency_key UUID UNIQUE NOT NULL,
    pg_transaction_id VARCHAR(255),
    provider VARCHAR(50) DEFAULT 'MOCK',
    payment_url TEXT,
    failure_reason TEXT,
    expires_at TIMESTAMP WITH TIME ZONE,
    paid_at TIMESTAMP WITH TIME ZONE,
    raw_callback JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_transactions_amount CHECK (amount >= 0)
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id VARCHAR(20) NOT NULL REFERENCES students(mssv),
    workshop_id UUID REFERENCES workshops(id),
    type notification_type NOT NULL,
    content TEXT NOT NULL,
    status notification_status NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    provider_message_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE sync_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name VARCHAR(255),
    total_records INTEGER DEFAULT 0,
    success_count INTEGER DEFAULT 0,
    error_count INTEGER DEFAULT 0,
    status sync_status NOT NULL DEFAULT 'RUNNING',
    error_file_path TEXT,
    message TEXT,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    target_type VARCHAR(100),
    target_id VARCHAR(255),
    old_value JSONB,
    new_value JSONB,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45)
);

CREATE TABLE checkin_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    registration_id UUID REFERENCES registrations(id),
    student_id VARCHAR(20) NOT NULL REFERENCES students(mssv),
    workshop_id UUID NOT NULL REFERENCES workshops(id),
    qr_code VARCHAR(255) NOT NULL,
    staff_id VARCHAR(255) NOT NULL,
    device_id VARCHAR(255),
    scanned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    synced_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    status checkin_event_status NOT NULL DEFAULT 'ACCEPTED',
    conflict_reason TEXT,
    client_event_id UUID,
    CONSTRAINT uq_checkin_events_client_event UNIQUE (client_event_id),
    CONSTRAINT uq_checkin_events_registration_accepted UNIQUE (registration_id, status)
);

-- 4. Indices
CREATE INDEX idx_registrations_student ON registrations(student_id);
CREATE INDEX idx_registrations_workshop ON registrations(workshop_id);
CREATE INDEX idx_workshops_status ON workshops(status);
CREATE INDEX idx_workshops_organizer ON workshops(organizer_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_student ON notifications(student_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_checkin_events_workshop ON checkin_events(workshop_id);
CREATE INDEX idx_checkin_events_student ON checkin_events(student_id);
CREATE INDEX idx_checkin_events_status ON checkin_events(status);
CREATE INDEX idx_workshops_tags ON workshops USING GIN (tags);

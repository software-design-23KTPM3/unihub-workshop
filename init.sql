-- Create schemas and tables for UniHub Workshop

CREATE TYPE workshop_status AS ENUM ('ACTIVE', 'CANCELLED');
CREATE TYPE summary_status AS ENUM ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED');
CREATE TYPE registration_status AS ENUM ('PENDING', 'SUCCESS', 'FAILED', 'CHECKED_IN');
CREATE TYPE transaction_status AS ENUM ('PENDING', 'SUCCESS', 'FAILED');
CREATE TYPE notification_type AS ENUM ('EMAIL', 'PUSH', 'IN_APP');
CREATE TYPE notification_status AS ENUM ('PENDING', 'SENT', 'FAILED');
CREATE TYPE sync_status AS ENUM ('SUCCESS', 'PARTIAL', 'FAILED', 'RUNNING');

CREATE TABLE students (
    mssv VARCHAR(20) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE workshops (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    speaker VARCHAR(255),
    room VARCHAR(100),
    max_seats INTEGER NOT NULL,
    available_slots INTEGER NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    is_paid BOOLEAN DEFAULT FALSE,
    price DECIMAL(12, 2) DEFAULT 0.00,
    status workshop_status DEFAULT 'ACTIVE',
    summary_text TEXT,
    summary_status summary_status DEFAULT 'PENDING',
    pdf_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE registrations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id VARCHAR(20) REFERENCES students(mssv),
    workshop_id UUID REFERENCES workshops(id),
    status registration_status DEFAULT 'PENDING',
    qr_code VARCHAR(255) UNIQUE,
    idempotency_key UUID UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    checked_in_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    registration_id UUID REFERENCES registrations(id),
    amount DECIMAL(12, 2) NOT NULL,
    status transaction_status DEFAULT 'PENDING',
    idempotency_key UUID UNIQUE NOT NULL,
    pg_transaction_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id VARCHAR(20) REFERENCES students(mssv),
    type notification_type NOT NULL,
    content TEXT NOT NULL,
    status notification_status DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE sync_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    total_records INTEGER DEFAULT 0,
    success_count INTEGER DEFAULT 0,
    error_count INTEGER DEFAULT 0,
    status sync_status DEFAULT 'RUNNING',
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    target_id VARCHAR(255),
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45)
);

-- Indices for performance
CREATE INDEX idx_registrations_student ON registrations(student_id);
CREATE INDEX idx_registrations_workshop ON registrations(workshop_id);
CREATE INDEX idx_workshops_status ON workshops(status);
CREATE INDEX idx_notifications_status ON notifications(status);

-- Seed Data
INSERT INTO students (mssv, email, name, status) VALUES 
('2312345', 'student1@unihub.com', 'Nguyen Van A', 'ACTIVE'),
('2312346', 'student2@unihub.com', 'Tran Thi B', 'ACTIVE');

INSERT INTO workshops (id, name, speaker, room, max_seats, available_slots, start_time, end_time, is_paid, price, status) VALUES 
('11111111-1111-1111-1111-111111111111', 'Spring Boot Microservices', 'John Doe', 'A101', 50, 50, CURRENT_TIMESTAMP + INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '1 day 2 hours', false, 0, 'ACTIVE'),
('22222222-2222-2222-2222-222222222222', 'Advanced AI in Practice', 'Jane Smith', 'B202', 30, 30, CURRENT_TIMESTAMP + INTERVAL '2 days', CURRENT_TIMESTAMP + INTERVAL '2 days 3 hours', true, 100000, 'ACTIVE');

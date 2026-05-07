-- Seed Students
INSERT INTO students (mssv, email, name, status, created_at, updated_at) VALUES 
('21110001', 'student1@test.com', 'Nguyen Van A', 'ACTIVE', NOW(), NOW()),
('21110002', 'student2@test.com', 'Tran Thi B', 'ACTIVE', NOW(), NOW()),
('21110003', 'student3@test.com', 'Le Van C', 'ACTIVE', NOW(), NOW())
ON CONFLICT (mssv) DO NOTHING;

-- Seed Workshops
INSERT INTO workshops (id, name, description, speaker, speaker_title, topic, room, max_seats, available_slots, start_time, end_time, is_paid, price, status, created_at, updated_at) VALUES 
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Modern AI Workshop', 'Introduction to Generative AI', 'Dr. Smith', 'AI Specialist', 'Technology', 'Room 101', 50, 48, NOW() - INTERVAL '1 hour', NOW() + INTERVAL '2 hours', true, 100000, 'OPEN', NOW(), NOW()),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'Cloud Computing', 'AWS and Azure basics', 'Ms. Jane', 'Cloud Architect', 'Infrastructure', 'Room 202', 30, 29, NOW() + INTERVAL '1 day', NOW() + INTERVAL '1 day 2 hours', false, 0, 'OPEN', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Seed Registrations (for testing check-in)
-- Note: UUIDs for registrations are random, but we can fix some for testing
INSERT INTO registrations (id, student_id, workshop_id, status, idempotency_key, qr_code, created_at, updated_at) VALUES 
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b11', '21110001', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'SUCCESS', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c11', '{"studentId":"21110001","workshopId":"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"}', NOW(), NOW()),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b12', '21110002', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'SUCCESS', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c12', '{"studentId":"21110002","workshopId":"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"}', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

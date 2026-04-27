-- Temporary seed script
INSERT INTO students (mssv, email, name, status) VALUES 
('2312345', 'student1@unihub.com', 'Nguyen Van A', 'ACTIVE'),
('2312346', 'student2@unihub.com', 'Tran Thi B', 'ACTIVE') ON CONFLICT DO NOTHING;

INSERT INTO workshops (id, name, speaker, room, max_seats, available_slots, start_time, end_time, is_paid, price, status) VALUES 
('11111111-1111-1111-1111-111111111111', 'Spring Boot Microservices', 'John Doe', 'A101', 50, 50, CURRENT_TIMESTAMP + INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '1 day 2 hours', false, 0, 'ACTIVE'),
('22222222-2222-2222-2222-222222222222', 'Advanced AI in Practice', 'Jane Smith', 'B202', 30, 30, CURRENT_TIMESTAMP + INTERVAL '2 days', CURRENT_TIMESTAMP + INTERVAL '2 days 3 hours', true, 100000, 'ACTIVE') ON CONFLICT DO NOTHING;

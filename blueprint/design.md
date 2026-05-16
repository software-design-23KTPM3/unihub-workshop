# UniHub Workshop — Comprehensive Technical Design

## 1. Kiến trúc tổng thể (Architectural Overview)

Hệ thống UniHub Workshop được xây dựng dựa trên kiến trúc **Event-Driven Microservices** kết hợp với **API Gateway Pattern**. Hệ thống được thiết kế để xử lý tải đột biến (High-burst traffic) và đảm bảo tính toàn vẹn dữ liệu thông qua cơ chế xử lý bất đồng bộ.

### 1.1. Lý do lựa chọn kiến trúc
- **Khả năng chịu tải (Scalability):** Xử lý hơn 12.000 sinh viên truy cập trong thời gian ngắn nhờ cơ chế Rate Limiting và Atomic Slot Reservation trên Redis.
- **Tính sẵn sàng (Availability):** Sử dụng Message Broker (RabbitMQ) để tách rời các dịch vụ. Nếu Mail Server hoặc AI Service gặp sự cố, luồng đăng ký chính vẫn hoạt động ổn định.
- **Xử lý nền (Background Processing):** Các tác vụ nặng như bóc tách PDF bằng AI và gửi mail hàng loạt được đẩy sang các Worker chuyên biệt.

---

## 2. Sơ đồ C4 Model (Kiến trúc phân lớp)

### 2.1. Level 1 — System Context (Bối cảnh hệ thống)
Sơ đồ này thể hiện ranh giới của hệ thống UniHub và các tương tác với tác nhân bên ngoài.

**Mô tả sơ đồ L1:**
- **Actors:** Sinh viên (Đăng ký/Xem QR), Ban tổ chức (Quản trị/Báo cáo), Nhân sự điểm danh (Quét QR Offline).
- **Hệ thống ngoại vi:** 
    - *SMTP Provider:* Gửi mail vé QR.
    - *Payment Sandbox:* Xử lý giao dịch thanh toán giả lập.
    - *AI Service:* Trích xuất nội dung từ tài liệu PDF của Workshop.
    - *Legacy CSV System:* Cung cấp dữ liệu sinh viên đầu vào.

![](./images/l1.png)

### 2.2. Level 2 — Container (Sơ đồ thành phần)
Phân rã hệ thống thành các khối kỹ thuật triển khai độc lập.

**Mô tả sơ đồ L2:**
- **API Gateway (Nginx/Lua):** Chặn các yêu cầu spam bằng Rate Limiting (Token Bucket) và xác thực JWT sơ bộ.
- **Identity Provider (Keycloak):** Quản lý định danh và cấp phát JWT Token.
- **Backend Core:** Spring Boot instances xử lý logic nghiệp vụ và quản lý slot trên `redis-lock`.
- **Message Broker (RabbitMQ):** Xương sống giao tiếp giữa Core và các Workers.
- **Workers Layer:** Gồm AISummaryWorker (xử lý PDF), NotificationWorker (gửi mail) và RegistrationWorker (ghi DB theo lô).
- **Data Layer:** PostgreSQL (Lưu trữ bền vững) và Redis clusters (Rate limit & Locking).

![](./images/l2.png)

---

## 3. Luồng dữ liệu nghiệp vụ (Operational Flows)

### 3.1. Luồng Đăng ký & Thanh toán (f1)
Áp dụng cơ chế **Sync-over-Async** để đạt hiệu năng cao nhất.

**Phân tích luồng f1:**
- **Giai đoạn Đồng bộ (Sync):** Core API kiểm tra Idempotency và thực hiện giữ chỗ (Slot reservation) trên Redis thông qua Lua script. Nếu thành công, trả về `202 Accepted` ngay lập tức.
- **Giai đoạn Bất đồng bộ (Async):** Registration Worker nhận sự kiện từ RabbitMQ, thực hiện Bulk Insert vào PostgreSQL để tối ưu hóa kết nối DB.
- **Webhook Thanh toán:** Khi nhận thông báo từ Payment Sandbox, Server xác minh chữ ký HMAC-SHA256, cập nhật trạng thái `SUCCESS` và kích hoạt luồng gửi vé QR.

![](./images/f1.png)

### 3.2. Luồng Check-in Offline (f2)
Đảm bảo tính liên tục của việc điểm danh ngay cả khi mất mạng.

**Phân tích luồng f2:**
- **Store (Lưu trữ):** Dữ liệu quét QR được lưu tạm thời vào SQLite trên thiết bị Mobile App.
- **Forward (Đồng bộ):** Khi có mạng, App gửi danh sách Batch Sync lên Server.
- **Xử lý:** Backend duyệt lô dữ liệu, đối soát trạng thái vé hợp lệ và ghi nhận vào bảng `CheckinEventRecord` để làm bằng chứng hậu kiểm (Audit trail).

![](./images/f2.png)

---

## 4. Thiết kế cơ sở dữ liệu (Database Schema)

### 4.1. Sơ đồ Thực thể (ERD)
![](./images/db.png)

**Mô tả các bảng chính:**
- **Registrations:** Lưu QR Code độc nhất và trạng thái vé (`PENDING`, `SUCCESS`, `CHECKED_IN`).
- **Workshops:** Lưu số lượng chỗ trống và kết quả tóm tắt AI (JSONB).
- **CheckinEventRecord:** Nhật ký chi tiết mọi hành động quét mã, lưu vết `staffId` và thời gian thực (`scannedAt`).
- **AuditLogs:** Lưu vết mọi thay đổi của Admin trên hệ thống để đảm bảo tính minh bạch.

---

## 5. Bảo mật & Kiểm soát truy cập

### 5.1. Luồng Xác thực & RBAC (Auth Flow)
Sơ đồ minh họa quá trình xác thực đa lớp từ Gateway đến Backend.

**Mô tả sơ đồ auth:**
1. **Keycloak:** Người dùng lấy Token.
2. **Gateway:** Nginx dùng Public Key xác thực chữ ký JWT, kiểm tra Rate Limit.
3. **Backend:** `JwtAuthConverter` ánh xạ Role từ JWT sang Spring Security (Role-Based Access Control).

![](./images/auth.png)

---

## 6. Các cơ chế bảo vệ hệ thống

1. **Rate Limiting:** Triển khai bằng Lua Script tại Gateway, giới hạn 20 req/s cho mỗi IP.
2. **Circuit Breaker:** Resilience4j bao bọc các lời gọi External API (AI, Payment) với ngưỡng lỗi 50%.
3. **Idempotency:** Sử dụng khóa duy nhất sinh ra từ Client để tránh đăng ký/thanh toán trùng lặp (check tại Redis và DB Unique Constraint).
4. **Registration Cleanup:** Tác vụ lập lịch tự động giải phóng các slot `PENDING` quá hạn 15 phút để nhường chỗ cho sinh viên khác.

---

## 7. Các quyết định kỹ thuật (ADRs)

1. **ADR 1 (RabbitMQ):** Sử dụng Topic Exchange để linh hoạt trong việc mở rộng các Consumer mới (ví dụ: Analytics) mà không ảnh hưởng luồng Core.
2. **ADR 2 (Redis Lock):** Xử lý tranh chấp chỗ ngồi (Race Condition) trên RAM để bảo vệ hiệu năng Database.
3. **ADR 3 (Stateless Auth):** Sử dụng JWT để hệ thống có thể scale-out dễ dàng trên Docker/K8s.
4. **ADR 4 (Chunking AI):** Chia nhỏ văn bản PDF (Text Threshold: 100k) để tối ưu việc xử lý ngôn ngữ tự nhiên qua API AI.

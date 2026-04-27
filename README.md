# UniHub Workshop Backend

Hệ thống Backend cho nền tảng quản lý Workshop của UniHub, được xây dựng theo kiến trúc Microservices bất đồng bộ, hỗ trợ xử lý độ tập trung cao (high-concurrency), đảm bảo tính lũy đẳng (idempotency) và tích hợp AI.

## 🚀 Tính năng chính

- **High-Concurrency Seat Booking**: Sử dụng Redis Atomic Counter để giữ chỗ hiệu năng cao.
- **Event-Driven Architecture**: Xử lý đăng ký, thông báo, thanh toán qua RabbitMQ.
- **AI Integration**: Tự động tóm tắt nội dung Workshop từ tài liệu PDF.
- **API Gateway**: Quản lý traffic, xác thực JWT và Rate Limiting ngay tại Gateway.
- **Offline Check-in**: API đồng bộ dữ liệu điểm danh từ mobile với logic giải quyết xung đột.

## 🛠️ Công nghệ sử dụng

- **Backend core**: Java 17, Spring Boot 3, Spring Data JPA, Spring Security (OAuth2).
- **Hạ tầng**: Docker, PostgreSQL, Redis, RabbitMQ, Keycloak.
- **Gateway**: OpenResty (Nginx + Lua).
- **Thư viện**: PDFBox (Text extraction), Resilience4j (Circuit Breaker).

## 📋 Tiền đề cài đặt

Trước khi bắt đầu, hãy đảm bảo máy bạn đã cài đặt:
- Docker & Docker Compose
- Java 17 (JDK)
- Maven 3.8+

## ⚙️ Hướng dẫn cài đặt và khởi chạy

### Bước 1: Khởi tạo hạ tầng (Docker)

Hệ thống sử dụng Docker để quản lý các dịch vụ phụ trợ. Chạy lệnh sau tại thư mục gốc của dự án:

```bash
docker-compose up -d
```

Lệnh này sẽ khởi chạy:
- **PostgreSQL (Port 5432)**: Chứa dữ liệu chính và dữ liệu Keycloak.
- **Redis Rate (Port 6380)**: Dùng cho Rate Limiting tại Gateway.
- **Redis Lock/Slots (Port 6381)**: Dùng cho Atomic Reservation và Business Logic.
- **RabbitMQ (Port 5672, Admin 15672)**: Message broker.
- **Keycloak (Port 8080)**: Quản lý xác thực (tự động import realm `unihub`).
- **Nginx Gateway (Port 80)**: Cổng vào duy nhất của hệ thống.

### Bước 2: Build ứng dụng Backend

Di chuyển vào thư mục backend và build bằng Maven:

```bash
cd backend-core
mvn clean install
```

### Bước 3: Khởi chạy Backend

Sau khi build thành công, chạy ứng dụng:

```bash
mvn spring-boot:run
```
*Ghi chú: Backend sẽ chạy tại cổng **8081**.*

## 🧪 Kiểm tra hệ thống

### 1. Truy cập API qua Gateway
Tất cả các request nên đi qua Gateway tại cổng **80**:
- `GET http://localhost/api/v1/workshops`: Lấy danh sách Workshop.

### 2. Quản lý xác thực (Keycloak)
- URL: `http://localhost:8080`
- User quản trị: `admin` / `admin`
- Realm: `unihub`

### 3. Theo dõi hàng đợi (RabbitMQ)
- URL: `http://localhost:15672`
- User/Pass: `guest` / `guest`

## 📂 Cấu trúc thư mục quan trọng

- `/backend-core`: Mã nguồn Spring Boot chính.
- `/docker-compose.yml`: Cấu hình toàn bộ hạ tầng.
- `/init.sql`: Script khởi tạo schema database.
- `/nginx`: Cấu hình Gateway và script Lua xử lý logic Layer 7.
- `/realm-export.json`: Cấu hình Keycloak mẫu (Role: ADMIN, ORGANIZER, STUDENT).

---
*Phát triển bởi UniHub Team - Hệ thống Backend hiệu năng cao cho quản lý sự kiện.*

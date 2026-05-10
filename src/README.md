# UniHub Workshop

Hệ thống UniHub Workshop gồm web app, mobile app, backend core, payment sandbox, gateway, consumers và sync service. Toàn bộ source code chạy được nằm trong thư mục `src/`.

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

### Bước 1: Khởi chạy toàn bộ hệ thống bằng Docker

Chạy lệnh sau trong thư mục `src/`:

```bash
cd src
docker-compose up -d
```

Lệnh này sẽ khởi chạy:
- **PostgreSQL (Port 5432)**: Chứa dữ liệu chính và dữ liệu Keycloak.
- **Redis Rate (Port 6380)**: Dùng cho Rate Limiting tại Gateway.
- **Redis Lock/Slots (Port 6381)**: Dùng cho Atomic Reservation và Business Logic.
- **RabbitMQ (Port 5672, Admin 15672)**: Message broker.
- **Keycloak (Port 8080)**: Quản lý xác thực (tự động import realm `unihub`).
- **Nginx Gateway (Port 80)**: Cổng vào duy nhất của hệ thống.
- **Backend Core x3**: Ba instance sau gateway.
- **Web app (Port 3000)**: Giao diện sinh viên và admin.
- **Payment sandbox (Port 8090, qua gateway `/sandbox`)**.
- **Notification/Summary consumers** và **CSV sync service**.

### Bước 2: Build ứng dụng Backend

Di chuyển vào thư mục backend và build bằng Maven:

```bash
cd src/backend
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
- `GET http://localhost/api/workshops`: Lấy danh sách Workshop.
- Web app: `http://localhost:3000`
- Payment sandbox checkout đi qua gateway: `http://localhost/sandbox/...`

### 2. Quản lý xác thực (Keycloak)
- URL: `http://localhost:8080`
- User quản trị: `admin` / `admin`
- Realm: `unihub`

### 3. Theo dõi hàng đợi (RabbitMQ)
- URL: `http://localhost:15672`
- User/Pass: `guest` / `guest`

## 📂 Cấu trúc thư mục quan trọng

- `src/backend`: Backend Spring Boot chính.
- `src/app-web`: Web frontend.
- `src/app-mobile`: Mobile app Android cho check-in.
- `src/service-gateway`: OpenResty/Nginx gateway và Lua rate limiting.
- `src/service-payment`: Payment sandbox.
- `src/consumer-notification`: Email/in-app notification consumer.
- `src/consumer-summary`: AI summary consumer.
- `src/service-sync`: CSV student sync service.
- `src/service-sync-data`: CSV input folder mounted to `/service-sync-data` in `service-sync`.
- `src/service-keycloak/realm-export.json`: Keycloak realm seed.
- `src/uploads`: Uploaded PDF folder mounted to backend and summary consumer.
- `src/app-data/init.sql`, `src/app-data/seed.sql`: Database schema and seed data.

---
*Phát triển bởi UniHub Team - Hệ thống Backend hiệu năng cao cho quản lý sự kiện.*

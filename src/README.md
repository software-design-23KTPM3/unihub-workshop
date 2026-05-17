# 🏫 UniHub Workshop System - Hướng Dẫn Khởi Chạy & Kiểm Thử Chi Tiết

Chào mừng bạn đến với **UniHub Workshop** - Hệ thống quản lý và đăng ký hội thảo chuyên đề hiệu năng cao. Hệ thống được thiết kế theo kiến trúc Microservices và Event-Driven Architecture, hỗ trợ giữ chỗ đồng thời cao (High-Concurrency Seat Booking), tích hợp AI tóm tắt tài liệu, cổng thanh toán giả lập và đồng bộ dữ liệu ngoại tuyến.

Toàn bộ source code chạy được nằm trong thư mục `src/`. Tài liệu này cung cấp hướng dẫn chi tiết và đầy đủ nhất giúp người chấm/đánh giá có thể clone repository, khởi chạy toàn bộ hệ thống bằng một câu lệnh duy nhất và kiểm thử mọi tính năng mà không cần cài đặt thêm môi trường phức tạp.

---

## 🏗️ Kiến Trúc Hệ Thống

Hệ thống được vận hành đồng bộ thông qua các dịch vụ sau:
*   **Web App Frontend**: Giao diện người dùng cho sinh viên đăng ký giữ chỗ và ban tổ chức quản lý hội thảo.
*   **Nginx API Gateway**: Điểm tiếp nhận request duy nhất cổng `80`, tích hợp Lua Script hỗ trợ Rate Limiting bằng Redis và điều phối tải (Load Balancing) đến các Backend instance.
*   **Backend Core (3 Instances)**: Cụm Spring Boot microservices xử lý nghiệp vụ chính, cấu hình Virtual Threads (Spring Boot 3 + Java 21) tối ưu hiệu năng.
*   **Keycloak Identity Server**: Quản lý định danh (OAuth2 / OIDC), phân quyền RBAC và tự động seed sẵn dữ liệu người dùng.
*   **Redis Rate & Redis Lock**: Hai phân vùng Redis độc lập cho Rate Limiting tại Gateway (Port 6380) và Distributed Lock / Atomic Counters cho nghiệp vụ giữ chỗ (Port 6381).
*   **RabbitMQ Message Broker**: Kênh truyền tin bất đồng bộ cho thông báo và quy trình xử lý tóm tắt văn bản bằng AI.
*   **Notification Consumer**: Lắng nghe hàng đợi từ RabbitMQ để gửi email/thông báo check-in thành công.
*   **AI Summary Consumer**: Tích hợp Apache PDFBox để trích xuất text từ file PDF tài liệu hội thảo và gọi LLM API (hoặc Mock) để cập nhật tóm tắt tự động.
*   **Sync Service**: Tiến trình ngầm tự động đồng bộ danh sách sinh viên từ file CSV của nhà trường vào cơ sở dữ liệu và tự động tạo tài khoản tương ứng trên Keycloak khi khởi động.
*   **Adminer**: Giao diện trực quan để quản lý cơ sở dữ liệu PostgreSQL.

---

## 📋 Tiền Đề Cài Đặt (Prerequisites)

Bạn **chỉ cần duy nhất** công cụ sau cài đặt sẵn trên máy:
*   **Docker** & **Docker Compose** (Khuyến nghị Docker Desktop mới nhất hoặc Docker Engine >= 20.10, hỗ trợ câu lệnh `docker compose`).

> [!NOTE]  
> Bạn **không cần** cài đặt JDK 21 hoặc Maven trên máy của mình. Toàn bộ mã nguồn Java Backend, Sync Service, Gateway và Frontend đều được xây dựng qua quy trình **Docker Multi-Stage Build** để tự động biên dịch và đóng gói ngay bên trong môi trường Docker bị cô lập, đảm bảo tính nhất quán 100%.

---

## ⚙️ Hướng Dẫn Khởi Chạy Nhanh (Quick Start)

### Bước 1: Khởi chạy toàn bộ hệ thống bằng Docker Compose

Mở Terminal tại thư mục `src/` (nơi chứa file `docker-compose.yml`) và chạy câu lệnh duy nhất sau:

```bash
docker compose up -d --build
```
*(Nếu bạn sử dụng phiên bản Docker cũ, vui lòng thay thế bằng `docker-compose up -d --build`)*

Lệnh này sẽ tự động:
1.  Tải các Base Image cần thiết.
2.  Biên dịch mã nguồn và build các Docker Image cho Backend, Gateway, Consumers, Sync-Service, Frontend.
3.  Thiết lập mạng nội bộ (Docker Network) và khởi chạy đồng loạt 14 containers.
4.  Tự động import cấu hình Realm định danh `unihub` vào Keycloak từ file `service-keycloak/realm-export.json`.
5.  Khởi tạo schema database PostgreSQL và nạp dữ liệu mẫu ban đầu từ `app-data/init.sql` và `app-data/seed.sql`.
6.  Tự động kích hoạt **Sync Service** để đọc danh sách 1000+ sinh viên mẫu từ `service-sync-data/student_latest.csv` và tạo tài khoản đăng nhập tương ứng trên Keycloak.

### Bước 2: Kiểm tra trạng thái hệ thống

Đợi khoảng **20 - 30 giây** để Keycloak import realm thành công và các dịch vụ Spring Boot kết nối hoàn tất. Bạn có thể kiểm tra sức khỏe của API Gateway bằng cách truy cập:
*   **Gateway Health Endpoint**: [http://localhost/health](http://localhost/health) (Phản hồi `{"status":"UP"}` nghĩa là hệ thống đã sẵn sàng!).

Bạn cũng có thể xem log của dịch vụ đồng bộ sinh viên để chắc chắn dữ liệu đã sẵn sàng:
```bash
docker compose logs -f sync-service
```

---

## 🔐 Danh Sách Tài Khoản & Cổng Dịch Vụ

Dưới đây là bảng thông tin truy cập các dịch vụ và tài khoản đăng nhập mẫu đã được thiết lập sẵn trong hệ thống:

### 1. Thông Tin Các Cổng Dịch Vụ công cộng

| Dịch vụ | URL | Thông tin bổ sung |
| :--- | :--- | :--- |
| **Web Frontend (Sinh viên & Admin)** | [http://localhost:3000](http://localhost:3000) | Giao diện tương tác chính của người dùng |
| **API Gateway** | [http://localhost](http://localhost) | Cổng định tuyến API duy nhất (`/api/...`, `/sandbox/...`) |
| **Keycloak Admin Console** | [http://localhost:8080](http://localhost:8080) | Quản lý định danh và phân quyền tài khoản |
| **RabbitMQ Management** | [http://localhost:15672](http://localhost:15672) | Quản trị và giám sát hàng đợi tin nhắn |
| **Database Manager (Adminer)** | [http://localhost:8082](http://localhost:8082) | Quản trị DB PostgreSQL trực quan |

### 2. Thông Tin Tài Khoản Đăng Nhập Mẫu

#### A. Tài Khoản Quản Trị Hệ Thống (Keycloak / RabbitMQ / Adminer)
*   **Keycloak Admin Console** ([http://localhost:8080](http://localhost:8080)):
    *   Username: `admin` / Password: `admin`
*   **RabbitMQ Dashboard** ([http://localhost:15672](http://localhost:15672)):
    *   Username: `guest` / Password: `guest`
*   **Adminer Database** ([http://localhost:8082](http://localhost:8082)):
    *   Hệ quản trị (System): `PostgreSQL`
    *   Máy chủ (Server): `postgres` (Tên container trong Docker network)
    *   Tài khoản: `admin` / Mật khẩu: `password`
    *   Cơ sở dữ liệu (Database): `unihub_db`

#### B. Tài Khoản Thử Nghiệm Nghiệp Vụ (Đăng nhập tại Frontend: [http://localhost:3000](http://localhost:3000))
*   **Tài khoản Sinh viên (STUDENT)**:
    *   *Cách thức*: Dữ liệu sinh viên được tự động đồng bộ từ file CSV. Toàn bộ 1000+ sinh viên mẫu đều có tài khoản đăng nhập hoạt động ngay lập tức.
    *   *Tài khoản mẫu 1*: Username: `21127001` | Password: `01012004` (MSSV và Ngày sinh định dạng `ddMMyyyy`)
    *   *Tài khoản mẫu 2*: Username: `21127002` | Password: `15052004`
    *   *Tài khoản mẫu 3*: Username: `21127003` | Password: `20102004`
*   **Tài khoản Ban tổ chức (ORGANIZER)**:
    *   *Quyền*: Tạo mới workshop, duyệt tài liệu PDF hội thảo, cập nhật trạng thái.
    *   *Tài khoản*: Username: `organizer` | Password: `password`
*   **Tài khoản Điểm danh (STAFF)**:
    *   *Quyền*: Quét vé check-in, kiểm soát vé sinh viên.
    *   *Tài khoản*: Username: `staff` | Password: `password`

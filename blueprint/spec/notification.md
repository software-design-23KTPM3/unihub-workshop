# Đặc tả: Hệ thống Thông báo Đa Kênh (Notification Distribution)

## Mô tả
Tính năng này chuyên phụ trách vai trò cầu nối thông báo cho sinh viên. Đảm nhận luồng thông tin xác nhận sau khi Đăng kí Workshops (sinh viên sẽ nhận được thông báo qua nhiều nền tảng sau khi đăng khí thành công Workshop). Nền tảng cấu trúc theo định hướng linh hoạt mở rộng để có thể dễ dàng khai thác các kênh mới tương lai của ban giám hiệu nhà trường (thêm Telegram, SMS) mà không cần lập trình viên phải dỡ mã nguồn ra code phân tách lại core.

## Luồng chính
1. Tạo sự kiện thông báo
    - Khi sinh viên đăng ký workshop thành công, Workshop Service (Backend) tạo một Notification Event
    - Event có dạng JSON, bao gồm:
        - eventId (unique id để chống gửi trùng)
        - userId
        - notificationType (EMAIL / PUSH / IN_APP)
        - payload (nội dung thông báo)
    - Event được publish vào Message Queue (RabbitMQ)
2. Nhận và xử lý event
    - Notification Worker (Consumer Service) lắng nghe message từ RabbitMQ
    - Worker thực hiện:
        - Validate event
        - Kiểm tra eventId để đảm bảo idempotency
        - Nếu event đã xử lý => bỏ qua
        - Parse dữ liệu:
            - userId
            - notiuficationType
            - payload
3. Chọn chiến lược gửi (Strategy Pattern)
    - Hệ thống sử dụng Strategy Pattern để xử lý đa kênh. Mỗi strategy thực hiện gửi riêng
    - Mapping:
        - EMAIL → EmailNotificationStrategy
        - PUSH → PushNotificationStrategy
        - IN_APP → InAppNotificationStrategy
    - Worker chọn strategy tương ứng tại runtime dựa trên notificationType
    - Input:
        - userId
        - payload
        - metadata (email, deviceToken,...)
    - Output:
        {
            "status": "SUCCESS | FAILED",
            "providerMessageId": "...",
            "error": "... (optional)"
        }
4. Lưu trạng thái & kết quả
    - Mỗi notification được lưu trạng thái:
        - PENDING
        - SENT
        - FAILED
    - Worker cập nhật trạng thái sau khi xử lý xong từng channel
5. Retry & DLQ
    - Nếu gửi thất bại:
        - Retry tối đa 3 lần
        - Delay theo exponential backoff
    - Nếu vẫn thất bại:
        - Đẩy message vào Dead Letter Queue (DLQ)
        - Log lỗi vào monitoring system
### Các thành phần tham gia
1. Backend services
2. Message Queue (RabbitMQ)
3. Notification Worker
4. Strategy Layer
5. Notification services (Email, Push Notification,...)
6. Notification Database
7. Dead Letter Queue (DLQ)
8. Client App (Web/ Mobile)

## Kịch bản lỗi
<!-- Điều gì xảy ra khi: timeout, mất mạng, dữ liệu không hợp lệ, ... -->
1. Queue không hoạt động
- Event được retry publish từ Workshop Service
- RabbitMQ persistence đảm bảo không mất message
- Worker reconnect + resume consume
2. Gửi notification thất bại
- Retry tối đa 3 lần
- Áp dụng exponential backoff
- Nếu vẫn fail => chuyển sang DLQ
3. Timeout external service
- Timeout cấu hình theo từng provider
- Retry nếu cần thiết
- Nếu quá ngưỡng => mark FAILED
4. Ứng dụng sinh viên mất mạng
- Không ảnh hưởng đến hệ thống backend
- In-app notification vẫn được lưu và hiển thị khi user online lại
5. Dữ liệu không hợp lệ
Ví dụ:
    - Email null
Xử lý:
    - Đánh dấu FAILED, không crash worker 
6. Worker crash giữa chừng
    - Envent chưa ack => RabbitMQ re-deliver
    - Idempotency (userId, workshopId) đảm bảo không gửi trùng    

## Ràng buộc
<!-- Giới hạn hiệu năng, bảo mật, tính nhất quán cần đảm bảo -->
1. Tính năng mở rông: Khí cần thêm kênh Telegram, lập trình viên chỉ cần thêm class TelegramNotification implements NotificationChannel mà không cần sửa đổi logic cốt lõi của Notification Service
2. Hiệu năng:
    - Notification không block luồng đăng ký workshop
    - Worker xử lý async (Backend chỉ đưa yêu cầu gửi email vào queue, worker sẽ xử lý sau, nên user không phải chờ.)
3. Template:
    - Nội dung notification phải dùng template engine
    - Không hardcode message trong Java code
4. Bảo mật: 
    - Payload không chứa dữ liệu nhạy cảm (password, token)
    - Message queue có thể bật encryption (TLS / at-rest encryption)
    - Validate input trước khi gửi external service
## Tiêu chí chấp nhận
<!-- Làm thế nào để biết tính năng này hoạt động đúng? -->
1. Sinh viên nhận được thông báo đúng kênh đã cấu hình sau thời gian đăng kí thành công workshop không quá 1 phút
2. Không xảy ra gửi trùng notification với cùng 1 workshopId 
3. Các lỗi từ dịch vụ thứ 3 (Firebase, Email Provider) không làm sập Notification Service 
4. Retry tối đa 3 lần/ channel. Có expontential backoff 
5. Message fail sau retry phải vào DLQ 

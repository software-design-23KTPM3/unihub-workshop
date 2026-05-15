# Đặc tả: Thông báo (Notification Service)

## Mô tả
Hệ thống thông báo giúp sinh viên nhận được thông tin cập nhật về trạng thái đăng ký, thanh toán và các thay đổi của Workshop. Hệ thống kết hợp giữa thông báo tức thời trong ứng dụng (In-app) và thông báo qua Email.

## Luồng chính

### 1. Kích hoạt thông báo (Trigger)
Thông báo được kích hoạt từ Backend khi:
- Sinh viên giữ chỗ thành công (Thông báo chờ thanh toán hoặc đăng ký thành công).
- Thanh toán thành công (Thông báo xác nhận và gửi mã QR).
- Workshop bị hủy (Thông báo cho sinh viên đã đăng ký).

### 2. Thông báo trong ứng dụng (In-app Notification)
- Backend lưu trực tiếp bản ghi vào bảng `notifications` trong Database.
- Mobile App/Web App sẽ query các thông báo này để hiển thị cho người dùng.

### 3. Thông báo Email (Asynchronous Email)
1.  **Gửi tới Queue**: Backend đóng gói thông tin (người nhận, tiêu đề, nội dung, dữ liệu QR) và gửi vào RabbitMQ `notification.exchange` với routing key `notification.key`.
2.  **Xử lý tại Worker**: 
    - `NotificationWorker` lắng nghe `notification.queue`.
    - Giải mã yêu cầu và sử dụng `NotificationChannel` (Email) để gửi thư.
    - Dữ liệu Email bao gồm cả hình ảnh mã QR (dưới dạng Base64) để sinh viên có thể sử dụng ngay từ email.

## Cấu trúc dữ liệu thông báo
- **Title**: Tiêu đề thông báo.
- **Message**: Nội dung chi tiết.
- **QR Code Payload**: Dữ liệu mã QR (Registration ID).
- **Workshop Info**: Tên, thời gian, địa điểm, diễn giả.

## Kịch bản lỗi
*   **Lỗi gửi Email**: Nếu `NotificationWorker` không gửi được email (do SMTP lỗi), message có thể được retry hoặc đưa vào Dead Letter Queue (tùy cấu hình).
*   **Hệ thống bận**: Nhờ sử dụng RabbitMQ, việc gửi hàng loạt thông báo (ví dụ khi hủy workshop) sẽ không làm chậm các tiến trình chính của Backend.

## Ràng buộc
*   **Tính bất đồng bộ**: Mọi thông báo Email bắt buộc phải được xử lý qua hàng đợi (RabbitMQ) để không làm tăng latency của API.
*   **Bảo mật**: Thông tin cá nhân trong email phải được bảo vệ và không chứa các dữ liệu nhạy cảm ngoài mã QR tham dự.

## Tiêu chí chấp nhận
*   Sinh viên nhận được thông báo in-app ngay sau khi đăng ký/thanh toán.
*   Email được gửi thành công kèm theo mã QR (nếu có) trong vòng 1-2 phút sau khi sự kiện xảy ra.
*   Nội dung email hiển thị đúng định dạng và đầy đủ thông tin workshop.

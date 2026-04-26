# Đặc tả: AI Summary

## Mô tả
Tính năng tự động hóa việc tóm tắt nội dung tài liệu giới thiệu workshop (PDF). Khi Ban tổ chức tải file PDF lên, hệ thống sẽ tự động xử lý, trích xuất nội dung, làm sạch văn bản và gửi sang mô hình AI để tạo bản tóm tắt giúp sinh viên dễ nắm bắt thông tin, hiển thị trên trang chi tiết workshop.

## Luồng chính
1. Khởi tạo yêu cầu: 
    - Ban tổ chức upload file PDF tại trang tạo/sửa workshop.
2. Xử lý tải file (Bất đồng bộ):
    - Workshop Service (Backend) thực hiện: 
        - Lưu file vào Storage (Local Storage hoặc S3/MinIO).
        - Cập nhật metadata của workshop vào Database.
        - Trả về phản hồi HTTP 202 Accepted cho máy khách ngay lập tức.
    - Hệ thống tiếp tục cập nhật trạng thái tiến trình: `summaryStatus = PENDING`.
    - Phát sinh sự kiện (Event Driven):
        ```json
        {
            "eventId": "UUID",
            "workshopId": "string",
            "fileUrl": "string",
            "fileHash": "sha256",
            "createdAt": "timestamp"
        }
        ```
    - Chuyển event vào Message Queue (RabbitMQ).
3. Nhận và xử lý:
    - Consumer (AI Summary Worker) kéo thông điệp từ RabbitMQ.
    - Kiểm tra Idempotency thông qua `fileHash` (nhằm bỏ qua nếu đã xử lý trùng lặp).
    - Cập nhật Database: `summaryStatus = PROCESSING`.
4. Trích xuất và làm sạch dữ liệu:
    - Tải nguyên liệu nguyên bản nội dung file từ Storage.
    - Thông qua Framework (Apache PDFBox) rút trích các dòng văn bản.
    - Loại bỏ mã HTML, ký tự đặc biệt rác, header hay khoảng trắng không có ý nghĩa ngữ nghĩa.
5. Xử lý giới hạn Token (Chunking):
    - Chia văn bản theo giới hạn token (Token Limit) của thuật toán AI đang dùng.
    - Nếu file rất lớn, chia thành đa khối nhỏ để Model AI hiểu và sau đó áp dụng thao tác tổng hợp đa kết quả con (Map-Reduce algorithm) thành một đoạn Summary duy nhất.
6. Gọi mô hình AI tóm tắt:
    - Gửi request đến Google AI Studio lấy đoạn Text rút gọn chuẩn định dạng hội thảo.
7. Lưu chuyển kết quả:
    - Backend ghi nhận dữ liệu về `summaryText` cho mã Workshop ID. Update status = `COMPLETED` hoặc `FAILED`.
8. Đồng bộ Realtime (SSE):
    - (Background) Publisher gửi payload kết quả tới kết nối Server-Sent Events (SSE).
    - Admin UI chủ động tự đổi dòng trạng thái không cần F5.

### Các thành phần tham gia 
- Workshop Sub-System
- Message Broker (RabbitMQ)
- Worker Group (AI Automation Process)
- Storage System
- PDF Parse Engine
- LLM Cloud Platform (Google AI Studio)
- Dữ liệu (PostgreSQL/SQL Server)
- SSE Endpoint

## Kịch bản lỗi
<!-- Điều gì xảy ra khi: timeout, mất mạng, dữ liệu không hợp lệ, ... -->
1. Upload file thất bại (File không lưu được vào kho lưu trữ) 
    - Trả lỗi HTTP 500 cho Admin
    - Không tạo event
2. File PDF không hợp lệ (File corrupt / không đọc được)
    - Worker đánh dấu: summaryStatus = FAILED
    - Log error + bỏ qua bước gọi AI 
3. Lỗi trích xuất PDF (PDFBox không thể đọc được file PDF hoặc không trích xuất được nội dung)
    - Retry tối đa 2 lần
    - Nếu vẫn thất bại => đánh dấu FAILED
4. Lỗi gọi AI API (Timeout / rate limit / server error)
    - Retry tối đa 3 lần
    - Nếu vẫn thất bại => đánh dấu FAILED
5. Worker crash giữa chừng
    - Message chưa ACK => RabbitMQ re-deliver
    - Idempotency (workshopId) đảm bảo không xử lý trùng
6. Storage không truy cập được
    - Retry tải file
    - Nếu vẫn thất bại => đánh dấu FAILED
## Ràng buộc
1. Bất đồng bộ
    - Upload file không được block luồng tạo workshop
    - Trả về HTTP 202 ngay lập tức
2. Idempotency
    - Mỗi workshop chỉ có 1 summary active
    - Key: workshopId
3. Hiệu năng
    - Xử lý tối đa 1–5 phút / file tùy size
4. Giới hạn AI
    - Token limit theo model
    - Chunking text nếu vượt giới hạn input
5. Bảo mật
    - File URL không public nếu chứa dữ liệu nhạy cảm
    - AI prompt không chứa thông tin nội bộ không cần thiết
    - Validate file trước khi gửi AI
## Tiêu chí chấp nhận
1. Tạo summary thành công
    - Admin upload PDF => hệ thống tạo summary tự động
    - Kết quả lưu vào DB đúng workshop
2. Không block user flow
    - Upload file trả về HTTP 202 ngay lập tức
    - Không chờ AI xử lý
3. Không xử lý trùng
    - Một fileHash chỉ tạo 1 summary duy nhất
    - Không duplicate khi worker retry
4. Retry hoạt động đúng
    - Fail AI / PDF / Storage => retry theo rule
    - Sau retry vẫn fail => mark FAILED
5. Realtime update (optional)
    - Nếu Admin đang xem page:
        - Nhận được update qua SSE trong < 30s sau khi hoàn tất

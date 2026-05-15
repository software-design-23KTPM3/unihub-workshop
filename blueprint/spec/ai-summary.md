# Đặc tả: Tóm tắt nội dung bằng AI (AI Content Summary)

## Mô tả
Tính năng tự động tóm tắt nội dung của Workshop từ tài liệu PDF đính kèm. Hệ thống sử dụng mô hình ngôn ngữ lớn (LLM) để trích xuất những ý chính, giúp sinh viên nắm bắt nội dung nhanh chóng trước khi đăng ký.

## Luồng chính

### 1. Tải lên tài liệu (Upload)
- Khi Organizer tạo mới hoặc cập nhật Workshop kèm theo file PDF.
- File được lưu vào hệ thống lưu trữ (thư mục `uploads`).
- Hệ thống gửi một message (chứa `workshopId`) vào RabbitMQ `ai.summary.exchange`.

### 2. Trích xuất nội dung (Text Extraction)
`AISummaryWorker` lắng nghe hàng đợi và thực hiện:
1.  **Đọc PDF**: Sử dụng thư viện `PDFBox` để trích xuất toàn bộ văn bản từ file PDF.
2.  **Làm sạch dữ liệu**: Loại bỏ các ký tự thừa, số trang, khoảng trắng dư thừa để tối ưu hóa context cho AI.
3.  **Kiểm tra kích thước**: 
    - Nếu văn bản dưới 100,000 ký tự: Gửi toàn bộ cho AI xử lý.
    - Nếu văn bản quá lớn: Chia nhỏ văn bản thành các đoạn (chunking) và xử lý từng phần.

### 3. Tóm tắt bằng AI
- Gọi dịch vụ AI (qua `AISummaryService`) để tạo nội dung tóm tắt.
- Kết quả trả về là một đoạn văn bản tóm tắt súc tích.

### 4. Cập nhật kết quả
- Cập nhật trường `summary_text` trong bảng `workshops`.
- Cập nhật `summary_status` thành `COMPLETED` (hoặc `FAILED` nếu có lỗi).
- Sau khi hoàn tất, sinh viên có thể xem nội dung tóm tắt này trên trang chi tiết Workshop.

## Kịch bản lỗi
*   **PDF bị mã hóa**: Nếu file PDF bị đặt mật khẩu hoặc mã hóa, `PDFBox` không thể trích xuất văn bản -> Đánh dấu trạng thái `FAILED`.
*   **File quá lớn**: Xử lý bằng cơ chế chia nhỏ (chunking) để tránh vượt quá giới hạn token của API AI.
*   **Lỗi kết nối AI**: Nếu API AI không phản hồi, worker sẽ log lỗi và đánh dấu `FAILED`.

## Ràng buộc
*   **Bất đồng bộ**: Quá trình trích xuất và tóm tắt có thể mất nhiều thời gian (vài giây đến vài phút) nên bắt buộc phải xử lý qua hàng đợi (RabbitMQ) để không block giao diện quản trị.
*   **Tài nguyên**: Giới hạn kích thước file PDF tải lên (mặc định 25MB) để tránh quá tải server.

## Tiêu chí chấp nhận
*   Workshop sau khi upload PDF hợp lệ sẽ có nội dung tóm tắt trong vòng vài phút.
*   Nội dung tóm tắt hiển thị đúng trên ứng dụng của sinh viên.
*   Trạng thái tóm tắt (`PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`) được cập nhật chính xác.

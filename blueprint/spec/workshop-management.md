# Đặc tả: Quản lý Workshop (Workshop Management)

## Mô tả
Tính năng cho phép Ban tổ chức (Organizer) quản lý vòng đời workshop: tạo mới, cập nhật thông tin và hủy workshop. Hệ thống tập trung vào hiệu năng đọc bằng cơ chế Cache Invalidation và đảm bảo tính nhất quán dữ liệu giữa Database và Redis.

## Luồng chính

### 1. Khởi động hệ thống (Startup Synchronization)
Khi ứng dụng Backend khởi chạy, `WorkshopServiceImpl` tự động quét toàn bộ workshop trong database và đồng bộ:
1.  **Slot Counter**: Nếu chưa có trong Redis, khởi tạo `workshop_slots:{id}` bằng giá trị `available_slots` từ DB.
2.  **Meta Data**: Đồng bộ `status`, `registration_start_epoch`, và `registration_end_epoch` vào Redis Hash `workshop_meta:{id}`.

### 2. Tạo mới / Cập nhật Workshop
1.  **Gửi yêu cầu**: Admin/Organizer gửi request `POST` hoặc `PUT` kèm thông tin workshop và file PDF (tùy chọn).
2.  **Validate**: Kiểm tra các ràng buộc (tên không trống, capacity > 0, thời gian hợp lệ). Khi cập nhật, `capacity` không được nhỏ hơn số lượng sinh viên đã đăng ký thành công.
3.  **Lưu Database**: Lưu thông tin vào PostgreSQL.
4.  **Xử lý PDF**: Nếu có file PDF, lưu vào storage và gửi message vào RabbitMQ `ai.summary.exchange` để bắt đầu tiến trình tóm tắt nội dung tự động.
5.  **Đồng bộ Redis**:
    - Cập nhật slot counter nếu có thay đổi capacity.
    - Cập nhật `workshop_meta:{id}` sau khi transaction commit.
6.  **Invalidate Cache**: Xóa cache danh sách `workshop_list` và cache chi tiết `workshop_details:{id}` để sinh viên thấy dữ liệu mới nhất.

### 3. Hủy Workshop
1.  **Gửi yêu cầu**: Organizer gửi request `PATCH /api/admin/workshops/{id}/cancel`.
2.  **Cập nhật trạng thái**: Đặt `status = CANCELLED` và `available_slots = 0` trong DB.
3.  **Xóa Redis**: Xóa key `workshop_slots:{id}` và `workshop_meta:{id}` để chặn đăng ký mới ngay lập tức.
4.  **Invalidate Cache**: Xóa cache để cập nhật giao diện người dùng.

## Kịch bản lỗi
*   **Capacity không hợp lệ**: Cố gắng đặt capacity nhỏ hơn số người đã đăng ký -> Trả về lỗi `400 Bad Request`.
*   **Phòng trùng lịch**: Hệ thống kiểm tra trùng lặp phòng/thời gian trước khi lưu (Business validation).
*   **Lỗi PDF**: Nếu PDF lỗi, trạng thái AI Summary sẽ được đánh dấu là `FAILED` bởi worker.

## Ràng buộc
*   **RBAC**: Chỉ người dùng có role `ORGANIZER` hoặc `ADMIN` mới được phép thực hiện các thao tác quản lý.
*   **Transaction**: Các thao tác đồng bộ Redis Meta và gửi RabbitMQ message chỉ được thực hiện SAU KHI database transaction commit thành công (`TransactionSynchronizationManager`).
*   **Cache**: Sử dụng `StringRedisTemplate` để quản lý cache và slots.

## Tiêu chí chấp nhận
*   Workshop mới được tạo có ngay slot counter trên Redis.
*   Khi thay đổi thông tin (ví dụ: đổi phòng), sinh viên truy cập lại sẽ thấy ngay thông tin mới (cache đã bị xóa).
*   File PDF tải lên được tự động kích hoạt tiến trình AI Summary.
*   Workshop đã hủy không thể đăng ký mới.
*   Số lượng sinh viên đã đăng ký thành công không bao giờ vượt quá capacity của workshop.
*   Mọi thao tác quản lý đều được xác thực role nghiêm ngặt tại tầng Service.

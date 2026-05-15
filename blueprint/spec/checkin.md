# Đặc tả: Điểm danh (Offline Check-in Sync)

## Mô tả
Tính năng cho phép nhân sự (Check-in Staff) thực hiện điểm danh sinh viên tại cửa phòng Workshop bằng Mobile App. Hệ thống hỗ trợ điểm danh offline trên app và đồng bộ dữ liệu về server sau đó.

## Luồng chính

### 1. Quét mã QR (Tại Mobile App)
- Nhân viên sử dụng Mobile App để quét mã QR trên vé của sinh viên.
- App lưu trữ các sự kiện điểm danh cục bộ (Local Storage) nếu không có mạng.

### 2. Đồng bộ dữ liệu (Sync to Server)
Khi có mạng, Mobile App gửi danh sách các sự kiện điểm danh lên Backend API `/api/checkin/sync`.

### 3. Xử lý tại Backend
Với mỗi sự kiện điểm danh (`CheckinEvent`), Backend thực hiện:
1.  **Tìm kiếm Registration**: Hệ thống tìm bản ghi đăng ký theo thứ tự ưu tiên:
    - Theo `registrationId` (nếu có).
    - Theo `qrCode` (giải mã từ mã quét được).
    - Theo cặp `studentId` và `workshopId`.
2.  **Kiểm tra điều kiện**: Chỉ chấp nhận điểm danh nếu `Registration` có trạng thái là `SUCCESS`.
3.  **Cập nhật**: 
    - Chuyển trạng thái sang `CHECKED_IN`.
    - Lưu thời gian điểm danh thực tế từ Mobile App (`checkedInAt`).
4.  **Ghi Log**: Nếu không tìm thấy hoặc trạng thái không hợp lệ, ghi log cảnh báo và bỏ qua sự kiện đó.

## Kịch bản lỗi
*   **Vé chưa thanh toán**: Nếu sinh viên có vé ở trạng thái `PENDING` (chưa thanh toán), hệ thống sẽ không cho phép điểm danh.
*   **Vé giả mạo**: Nếu mã QR không khớp với bất kỳ bản ghi nào trong hệ thống, sự kiện điểm danh sẽ bị loại bỏ.
*   **Đồng bộ trùng lặp**: Nếu một vé đã được điểm danh trước đó, các lần đồng bộ sau cho cùng một vé sẽ được xử lý an toàn (Idempotent - không gây lỗi).

## Ràng buộc
*   **RBAC**: Chỉ người dùng có role `CHECKIN_STAFF`, `ORGANIZER` hoặc `ADMIN` mới được phép gọi API đồng bộ điểm danh.
*   **Offline-First**: Mobile App chịu trách nhiệm đảm bảo dữ liệu không bị mất khi offline; Backend chịu trách nhiệm giải quyết xung đột khi đồng bộ.

## Tiêu chí chấp nhận
*   Dữ liệu điểm danh từ Mobile App được cập nhật chính xác vào Database Backend.
*   Trạng thái sinh viên chuyển từ `SUCCESS` sang `CHECKED_IN`.
*   Thời gian điểm danh trong Database khớp với thời gian ghi nhận trên Mobile App.
*   Trải nghiệm người dùng: Tốc độ phản hồi khi quét trên App phải < 200ms để không gây ùn tắc tại cửa phòng.
*   Hệ thống ghi nhận đúng thời gian quét thực tế (Timestamp từ Mobile App), không phải thời gian đồng bộ lên Server.
*   Nhân sự có thể làm việc liên tục khi mất mạng trong ít nhất 2 giờ (lưu trữ cục bộ tối thiểu 2000 bản ghi).
*   Sau khi có mạng 1 phút, dữ liệu trên trang quản trị (Admin Dashboard) phải được cập nhật mới nhất.
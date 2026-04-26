# Đặc tả: Điểm danh Offline (Offline-First Check-in)

## Mô tả
Tính năng cho phép nhân sự tại cửa phòng workshop quét mã QR để ghi nhận sự hiện diện của sinh viên. Đặc tả tập trung vào khả năng hoạt động trong môi trường mạng không ổn định (Offline-First) và cơ chế đồng bộ dữ liệu hàng loạt (Bulk Sync).

## Luồng chính
1. **Ghi nhận cục bộ (Offline)**: Nhân sự dùng Mobile App quét QR của sinh viên. App giải mã QR, lấy thông tin và lưu ngay vào **SQLite (Local DB)** với trạng thái `UNSYNCED` kèm timestamp thực tế. App báo "Thành công" ngay lập tức.
2. **Phát hiện mạng**: Một Background Job trên Mobile App liên tục lắng nghe trạng thái kết nối Internet.
3. **Đồng bộ hàng loạt (Sync)**: Khi có mạng, Job này query toàn bộ các bản ghi `UNSYNCED`, đóng gói thành một mảng JSON và gọi API `/api/v1/sync` (Bulk Update).
4. **Xử lý tại Server**: Core API nhận danh sách, thực hiện cập nhật trạng thái `CHECKED_IN` cho các sinh viên tương ứng trong PostgreSQL theo phương thức xử lý lô (Batch processing).
5. **Hoàn tất**: Server trả về `200 OK`. App cập nhật trạng thái các bản ghi trong SQLite thành `SYNCED`.

## Kịch bản lỗi
* **Quét trùng (Duplicate)**: Một mã QR bị quét 2 lần. Server phải đảm bảo tính lũy đẳng (idempotency), chỉ ghi nhận lần đầu tiên thành công.
* **Lỗi đồng bộ**: Nếu API trả về lỗi (4xx, 5xx), Background Job sẽ thực hiện chiến lược **Exponential Backoff** (thử lại sau 10s, 30s, 1m...) để tránh làm quá tải server.
* **App bị đóng đột ngột**: Do dữ liệu đã được Persistent vào SQLite, nên khi mở lại App, Background Job sẽ tự động tiếp tục luồng đồng bộ.

## Ràng buộc
* **Tính nhất quán (Consistency)**: Dữ liệu điểm danh cuối cùng trong PostgreSQL phải trùng khớp với dữ liệu thực tế đã quét trên App.
* **Trải nghiệm người dùng**: Tốc độ phản hồi khi quét trên App phải < 200ms để không gây ùn tắc tại cửa phòng.

## Tiêu chí chấp nhận
* Hệ thống ghi nhận đúng thời gian quét thực tế (Timestamp từ Mobile App), không phải thời gian đồng bộ lên Server.
* Nhân sự có thể làm việc liên tục khi mất mạng trong ít nhất 2 giờ (lưu trữ cục bộ tối thiểu 2000 bản ghi).
* Sau khi có mạng 1 phút, dữ liệu trên trang quản trị (Admin Dashboard) phải được cập nhật mới nhất.
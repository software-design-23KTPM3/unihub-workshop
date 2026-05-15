# Đặc tả: Đăng ký Workshop (Seat Booking)

## Mô tả
Tính năng cho phép sinh viên đăng ký tham gia workshop. Hệ thống sử dụng cơ chế **Atomic Reservation** tại Redis để đảm bảo không xảy ra tình trạng Overbooking (đăng ký vượt quá số ghế) ngay cả khi có hàng ngàn sinh viên đăng ký cùng lúc.

## Luồng chính

### Bước 1: Giữ chỗ tại Redis (Atomic Reservation)
Khi nhận yêu cầu đăng ký, hệ thống thực thi một Lua script tại Redis để đảm bảo tính nguyên tử:
1.  **Kiểm tra tồn tại**: Đảm bảo các key quản lý workshop (slots và meta) tồn tại trong Redis.
2.  **Kiểm tra trùng lặp**: Sử dụng Redis Set `workshop_registrations:{workshopId}` để kiểm tra sinh viên (`studentId`) đã đăng ký chưa. Nếu rồi, trả về `-1`.
3.  **Kiểm tra trạng thái**: Workshop phải ở trạng thái `ACTIVE`. Nếu không, trả về `-4`.
4.  **Kiểm tra thời gian**: Thời điểm hiện tại phải nằm trong khoảng `registration_start` và `registration_end`. Nếu không, trả về `-5` hoặc `-6`.
5.  **Trừ số lượng slot**: Sử dụng lệnh `DECR` trên key `workshop_slots:{workshopId}`.
    - Nếu kết quả `< 0`: Hoàn lại slot bằng lệnh `INCR` và trả về `-2` (Hết chỗ).
    - Nếu kết quả `>= 0`: Thêm sinh viên vào Redis Set `workshop_registrations:{workshopId}` và trả về `1` (Thành công).

### Bước 2: Phản hồi cho Client
Nếu giữ chỗ thành công tại Redis, Backend xử lý tiếp:
- Nếu là workshop miễn phí: Đặt trạng thái ban đầu là `SUCCESS`.
- Nếu là workshop có phí: Đặt trạng thái ban đầu là `PENDING`.
- Trả về mã `200 OK` (hoặc `201 Created`) kèm thông tin đăng ký cho Client.

### Bước 3: Lưu trữ bất đồng bộ (Async Persistence)
Một tiến trình chạy ngầm (Async Task) được kích hoạt ngay sau khi Redis giữ chỗ thành công:
1.  **Lưu Database**: Lưu bản ghi `Registration` vào PostgreSQL.
2.  **Tạo Transaction**: Nếu là workshop có phí, tạo bản ghi `Transaction` liên kết với registration.
3.  **Gửi thông báo**: Publish thông tin vào RabbitMQ để `NotificationWorker` gửi Email/In-app kèm mã QR.
4.  **Rollback**: Nếu việc lưu Database thất bại, hệ thống thực hiện **Rollback tại Redis**:
    - Xóa sinh viên khỏi Redis Set.
    - Tăng lại slot counter (`INCR`).

## Kịch bản lỗi
*   **Hết chỗ**: Trả về lỗi "Workshop is sold out".
*   **Ngoài khung giờ**: Trả về lỗi "Registration has not opened yet" hoặc "Registration period has ended".
*   **Đã đăng ký**: Trả về lỗi "You have already registered for this workshop".
*   **Lỗi hệ thống**: Nếu Redis hoặc Database gặp sự cố, hệ thống trả về lỗi 500 và đảm bảo không bị mất dữ liệu nhờ tính nhất quán của Redis + DB Transaction.

## Ràng buộc
*   **Idempotency**: Sử dụng `Idempotency-Key` (nếu có) và cơ chế `SISMEMBER` trong Redis để chặn đăng ký trùng lặp.
*   **Tính nhất quán**: Redis đóng vai trò là lớp bảo vệ (Guard) để kiểm soát số lượng, Database là nơi lưu trữ trạng thái bền vững.
*   **Thời gian giữ chỗ**: Đối với workshop có phí, nếu quá 30 phút không thanh toán, registration sẽ bị hủy và slot được hoàn lại (thủ công hoặc qua job quét).

## Tiêu chí chấp nhận
*   Không bao giờ xảy ra Overbooking (số lượng đăng ký thành công không vượt quá capacity).
*   Sinh viên nhận được phản hồi cực nhanh nhờ việc giữ chỗ thực hiện trên RAM (Redis).
*   Mã QR được sinh ra duy nhất cho mỗi bản ghi đăng ký.

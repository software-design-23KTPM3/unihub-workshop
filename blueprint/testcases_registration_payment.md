# Test Cases: Registration & Payment Workshop

Tài liệu này bao gồm các test case chi tiết cho chức năng Đăng ký Workshop và Thanh toán, tập trung vào các vấn đề kỹ thuật quan trọng: tranh chấp chỗ ngồi, rollback vé khi quá hạn thanh toán, Idempotency (chống trừ tiền 2 lần), và Circuit Breaker (đứt gãy cổng thanh toán).

## 1. Đăng ký Workshop Miễn phí (Free Workshop Registration)

| ID | Tên Test Case | Mô tả / Kịch bản (Scenario) | Kết quả mong đợi (Expected Result) |
|---|---|---|---|
| REG-FREE-01 | Đăng ký workshop miễn phí thành công | - SV chọn workshop miễn phí còn chỗ.<br>- Bấm "Đăng ký". | - Hệ thống trừ 1 chỗ thành công.<br>- Trạng thái đăng ký: `SUCCESS`.<br>- Sinh viên nhận được QR Code và email xác nhận. |
| REG-FREE-02 | Đăng ký workshop khi đã hết chỗ | - SV chọn workshop miễn phí hiển thị hết chỗ (0 chỗ).<br>- Bấm "Đăng ký". | - Hệ thống báo lỗi: "Workshop đã hết chỗ".<br>- Không trừ chỗ, không tạo đăng ký. |
| REG-FREE-03 | Đăng ký trùng lặp (Duplicate Registration) | - SV đã đăng ký thành công workshop A.<br>- Cố tình bấm đăng ký lại workshop A. | - Hệ thống báo lỗi: "Bạn đã đăng ký workshop này".<br>- Không trừ chỗ, giữ nguyên trạng thái đăng ký cũ. |

## 2. Tranh chấp chỗ ngồi (Concurrency - Race Condition)

| ID | Tên Test Case | Mô tả / Kịch bản (Scenario) | Kết quả mong đợi (Expected Result) |
|---|---|---|---|
| REG-CONC-01 | Tranh chấp chỗ ngồi cuối cùng (High Concurrency) | - Workshop chỉ còn đúng 1 chỗ (Seat = 1).<br>- N (ví dụ 100) sinh viên đồng thời gửi request đăng ký ở cùng một thời điểm mili-giây. | - Chỉ có đúng 1 sinh viên đăng ký thành công.<br>- (N - 1) sinh viên còn lại nhận thông báo "Workshop đã hết chỗ".<br>- Số chỗ còn lại trong DB (hoặc Redis) cập nhật chính xác là 0, tuyệt đối **không bị âm chỗ**. |

## 3. Đăng ký Workshop Có phí (Paid Registration & Rollback Seat)

*Quy trình chuẩn: Chọn đăng ký -> Tạm giữ chỗ -> Chờ thanh toán -> Thanh toán -> Thành công/Thất bại.*

| ID | Tên Test Case | Mô tả / Kịch bản (Scenario) | Kết quả mong đợi (Expected Result) |
|---|---|---|---|
| REG-PAID-01 | Thanh toán thành công trong thời gian quy định | - SV đăng ký workshop có phí, hệ thống tạm giữ chỗ (trừ 1 chỗ, trạng thái đăng ký là `PENDING_PAYMENT`).<br>- SV thực hiện thanh toán thành công trong thời gian cho phép (ví dụ: 15 phút). | - Hệ thống nhận được thông báo thanh toán thành công.<br>- Cập nhật trạng thái thành `SUCCESS`.<br>- Cấp QR Code và gửi thông báo. |
| REG-PAID-02 | Quá hạn thanh toán (Payment Time Exceed -> Rollback) | - SV đăng ký workshop có phí, hệ thống tạm giữ chỗ (trạng thái `PENDING_PAYMENT`).<br>- SV bỏ đi hoặc không thanh toán sau khi hết thời hạn (ví dụ: qua 15 phút). | - Hệ thống tự động đánh dấu đăng ký là `EXPIRED` (hoặc `CANCELLED`).<br>- **Rollback chỗ:** Hệ thống nhả chỗ đã giữ, cộng lại 1 chỗ vào số lượng vé (Seat + 1) để người khác đăng ký. |
| REG-PAID-03 | Thanh toán thất bại ngay lập tức | - SV đang ở trang thanh toán nhưng thanh toán lỗi do thẻ hết tiền hoặc bấm huỷ.<br>- Cổng thanh toán lập tức trả về trạng thái `FAILED`. | - Đăng ký chuyển sang trạng thái `FAILED` hoặc `CANCELLED`.<br>- **Rollback chỗ:** Hệ thống cộng lại 1 chỗ cho workshop (Seat + 1). |

## 4. Chống trừ tiền hai lần (Idempotency Key)

| ID | Tên Test Case | Mô tả / Kịch bản (Scenario) | Kết quả mong đợi (Expected Result) |
|---|---|---|---|
| PAY-IDEM-01 | Bấm thanh toán nhiều lần cùng lúc | - Sinh viên bấm nút "Thanh toán" 3-4 lần liên tục (Spam click) hoặc do app tự động retry khi rớt mạng.<br>- Client gửi nhiều request lên server nhưng có cùng một `Idempotency Key`. | - Server nhận diện được `Idempotency Key` bị trùng lặp.<br>- Chỉ thực hiện giao dịch cho request đầu tiên.<br>- Các request sau bị chặn lại và trả về kết quả của giao dịch đầu (đang xử lý hoặc đã thành công).<br>- **Đảm bảo không bị trừ tiền 2 lần.** |

## 5. Xử lý cổng thanh toán không ổn định (Circuit Breaker & Graceful Degradation)

| ID | Tên Test Case | Mô tả / Kịch bản (Scenario) | Kết quả mong đợi (Expected Result) |
|---|---|---|---|
| PAY-CIRC-01 | Cổng thanh toán lỗi liên tục (Circuit Breaker OPEN) | - Dịch vụ thanh toán bên thứ 3 (VNPAY/Momo) bị sập hoặc timeout.<br>- Các request thanh toán trả về lỗi vượt ngưỡng cấu hình của Circuit Breaker (ví dụ: 10 lỗi liên tiếp). | - Circuit Breaker chuyển sang trạng thái **OPEN** (Ngắt mạch).<br>- Request thanh toán tiếp theo bị từ chối ngay lập tức (Fail-fast) mà không cần đợi timeout cổng thanh toán.<br>- Thông báo rõ ràng: "Hệ thống thanh toán đang bảo trì, vui lòng thử lại sau". |
| PAY-CIRC-02 | Tự động phục hồi hệ thống (Circuit Breaker HALF-OPEN) | - Circuit Breaker đang OPEN. Hết thời gian cooldown (ví dụ: 30s), chuyển sang trạng thái **HALF-OPEN**.<br>- Hệ thống cho phép 1-2 request đi qua để "thử nghiệm". Cổng thanh toán phản hồi thành công. | - Circuit Breaker nhận thấy cổng thanh toán đã ổn định, chuyển về **CLOSED**.<br>- Các giao dịch thanh toán kế tiếp hoạt động bình thường trở lại. |
| PAY-CIRC-03 | Cách ly lỗi (Graceful Degradation) | - Circuit Breaker của luồng Thanh toán đang ở trạng thái **OPEN** do lỗi hệ thống thanh toán. | - Sinh viên **vẫn có thể** xem lịch, xem chi tiết workshop bình thường.<br>- Sinh viên **vẫn có thể** đăng ký các workshop **Miễn phí** bình thường (Các service khác không bị sụp theo dịch vụ thanh toán). |

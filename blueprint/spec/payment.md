# Đặc tả: Thanh toán 

## Mô tả
Tính năng này quản lý luồng đăng ký workshop có thu phí. Kiểm soát tính trạng thái giao dịch một lần duy nhất (Idempotency) và có khả năng chịu tải 12,000 học sinh/lần.

## Luồng chính
1. Khởi tạo đăng ký thanh toán
    - Sinh viên bấm “Đăng ký” trên workshop có phí
    - Client tạo:
        - Idempotency-Key (UUID v4) gửi kèm trong header request để chống duplicate request
2. Kiểm tra Rate Limit (Bảo vệ tải đột biến)
    - Request từ Client đi qua API Gateway / Load Balancer.
    - Cổng Gateway áp dụng thuật toán Rate Limiting (Token Bucket).
    - Nếu phát hiện lưu lượng tăng vọt hoặc spam (vượt ngưỡng cho phép của User/IP): 
        - Chặn request 
        - Trả về lỗi HTTP 429 - Too Many Requests.
    - Nếu request hợp lê:
        - Request được chuyển tiếp vào Backend Service.
3. Kiểm tra Idempotency
    - Backend kiểm tra Redis:
        - Nếu key đã tồn tại (Không gọi lại Payment Gateway):
            - status = SUCCESS => trả lại kết quả thanh toán cũ
            - status = PENDING => trả lại trạng thái đang xử lý
        - Nếu chưa tồn tại:
            - Lưu vào Redis:
                {
                    "status": "PENDING",
                    "workshopId": "...",
                    "userId": "...",
                    "createdAt": "...",
                    "expiresAt": "time"
                }
4. Soft-lock tài nguyên (giữ chỗ)
    - Hệ thống thực hiện giữ chỗ tạm thời:
        - availableSeats = availableSeats - 1 (TEMP LOCK)
    - TTL: 15 phút. 
    - Soft-lock KHÔNG chỉ dựa vào TTL đơn thuần. Phải dùng atomic operation (Redis Lua script hoặc DB row lock)
    - Nếu payment gateway timeout > 10 phút: seat vẫn được giữ trong “payment pending window” tối thiểu = max(soft-lock TTL, PG timeout buffer)
5. Tạo thanh toán với Payment Gateway
    - Backend gọi Payment Gateway
    - Input
        {
            "orderId": "internal_txn_id",
            "amount": 100000,
            "userId": "...",
            "callbackUrl": "https://unihub/webhook/payment"
        }
    - Output:
        {
            "paymentUrl": "https://pg/checkout/xyz",
            "transactionId": "pg_txn_id"
        }
    - Trả paymentUrl về client
6. Sinh viên thanh toán
    - User thực hiện thanh toán trên Payment Gateway
7. Webhook từ Payment Gateway
    - Payment Gateway gọi callback:     
        {
            "transactionId": "...",
            "status": "SUCCESS | FAILED",
            "signature": "..."
        }      
8. Xử lý webhook
    - Backend verify signature. 
    - Check idempotency (transactionId) 
    - Nếu SUCCESS:
        - Update DB:
            - transaction = SUCCESS
            - booking = CONFIRMED
        - Chuyển Soft-lock → Hard-commit
        - Update Redis:
            - status = SUCCESS
        - Emit event: PaymentCompletedEvent
    - Nếu FAILED:
        - rollback soft-lock
        - update status FAILED
        - release seat
## Kịch bản lỗi
1. Rủi ro quá tải đột biến (Spike Load 12,000 request/3 phút):
    - Áp dụng Rate Limiting bằng thuật toán **Token Bucket** ngay tại cổng API Gateway để drop bớt request ảo hoặc spam.
2. Tranh chấp chỗ ngồi Booking:
    - Soft-Lock dùng code Lua Script trên Redis giúp đảm bảo Atomic. Chỉ có user lấy được phép lock mới vào được bước 4 xử lý. Hàng trăm người cùng vào, hệ thống vẫn duy trì không Overbooking/Double Booking.
3. Sinh viên bỏ dở hoặc Lỗi chập mạng Thanh Toán:
    - Khi hết ngưỡng 15 phút, Soft-lock tự rớt TTL, Trình thu hồi (Timeout Evict) trên Redis sẽ nhả ghế tự động cho người khác săn vé.
4. Cổng thanh toán bên thứ ba bị Mất Mạng/Sập:
    - Áp dụng Pattern bảo vệ **Circuit Breaker**:
        - `CLOSED`: Hệ thống vận hành trơn tru.
        - `OPEN`: Hệ API VNPay/Momo đang dính lỗi 500 Server Error diện rộng, ngắt mọi luồng gọi kết nối sau đó 15-30 giây để bảo vệ CPU bên nhà trường. 
        - **Graceful Degradation** (Xử lý hiệu năng hạ cấp): Lúc vào trạng thái Open, nếu có yêu cầu đăng ký sinh viên mới, ứng dụng sẽ ghi luồng "Giữ vé Offline Pending". Khách hàng vẫn còn ghế nhưng tiền nợ lại sang bước đóng sau. Nhằm không ảnh hưởng tính năng Lịch học miễn phí khác. 
        - `HALF-OPEN`: Mở nhẹ đường truyền để kiểm định nếu đối tác ổn định lại.
5. Crash App nhà trường:
    - Redis + DB Transaction Logs có vai trò giữ trạng thái Event của Webhook PG đổ về, đảm bảo an toàn truy vết luồng tiền.

## Ràng buộc
1. Idempotency (Siêu quan trọng):
    - Tiêu chí 1: Nút bấm trên App client phải chặn Multiple Click, API Core từ chối việc cùng một Device tạo Double order cùng lúc.
    - Tiêu chí 2: Webhook gọi lặp 3-4 lần cho 1 đơn đều được ghi nhận đã xử lý hoàn tất mà không phát sinh thêm nghiệp vụ đắp tiền/đắp vé 2 lần.
2. Consistency Data Database:
    - Áp dụng Lock ở mức độ cấp Row của Redis trước khi DB chốt nhằm bảo toàn tài nguyên. Không có chuyện bán lỗ ghế (Oversell).

## Tiêu chí chấp nhận
1. Hoàn toàn chịu tải nhẹ nhàng dưới 12,000 request qua giới hạn bảo vệ Rate Limiter.
2. Triệt tiêu sai xót trừ nhầm ví của học sinh 2 lần bằng Idempotency.
3. Mọi tính năng tra cứu, xem tin, tải vé ở nhánh Offline free hoàn toàn chạy bình thường kể cả khi Payment Gateway hỏng theo chiến lược Circuit Breaker rẽ nhánh.
4. Tài nguyên được tự động rớt rảnh rỗi lại sau 15p delay mà không bị khóa vĩnh viễn.

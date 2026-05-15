# Đặc tả: Thanh toán (Payment Integration)

## Mô tả
Tính năng xử lý thanh toán cho các workshop có phí. Hệ thống tích hợp với một **Payment Sandbox** (giả lập cổng thanh toán) để xử lý các giao dịch. Luồng thanh toán đảm bảo tính an toàn bằng chữ ký số và tính nhất quán bằng Webhook.

## Luồng chính

### 1. Khởi tạo thanh toán (Start Payment)
1.  **Yêu cầu**: Sinh viên nhấn "Thanh toán" cho một registration đang ở trạng thái `PENDING`.
2.  **Xử lý tại Backend**:
    - Kiểm tra quyền sở hữu (chỉ sinh viên đăng ký mới được thanh toán).
    - Kiểm tra trạng thái đăng ký (`PENDING`).
    - Lấy thông tin `Transaction` hiện có hoặc tạo mới nếu cần.
    - Gọi API của **Payment Sandbox** (`POST /sandbox/api/payments`) để tạo phiên thanh toán và lấy `paymentUrl`.
    - Trả về `paymentUrl` cho Client.
3.  **Điều hướng**: Client điều hướng người dùng sang trang thanh toán của Sandbox.

### 2. Xử lý tại Sandbox
- Người dùng thực hiện thanh toán trên giao diện Sandbox.
- Sandbox hỗ trợ giả lập các kịch bản: **Thành công**, **Thất bại**, hoặc **Lỗi Server** (để test khả năng chịu lỗi của hệ thống).

### 3. Callback & Webhook
1.  **Return URL**: Sau khi thanh toán xong, Sandbox điều hướng người dùng quay lại Web App qua `returnUrl` (ví dụ: `/student/tickets/{id}/payment`).
2.  **Webhook (Server-to-Server)**:
    - Sandbox gửi một `POST` request chứa kết quả thanh toán đến `/api/payments/webhook`.
    - **Xác thực**: Backend kiểm tra chữ ký **HMAC-SHA256** của payload bằng `webhook-secret` để đảm bảo request đến từ Sandbox.
    - **Cập nhật**: 
        - Nếu status là `SUCCESS`: Cập nhật `Transaction` thành `SUCCESS`, `Registration` thành `SUCCESS`, và gửi thông báo thành công cho sinh viên.
        - Nếu status là `FAILED`: Cập nhật `Transaction` thành `FAILED`.

## Bảo mật và Ràng buộc
*   **Chữ ký số (Signature)**: Payload webhook được ký theo công thức: `hmacSha256(transactionId + "|" + gatewayPaymentId + "|" + status, secret)`. Chữ ký được gửi trong header `X-Payment-Signature`.
*   **Idempotency**: Sử dụng `Idempotency-Key` để tránh xử lý trùng lặp các giao dịch từ phía cổng thanh toán.
*   **Hết hạn (Expiry)**: Giao dịch có thời gian hết hạn (mặc định 30 phút). Nếu quá thời gian này mà không có webhook thành công, registration sẽ bị coi là hết hạn.

## Kịch bản lỗi & Kiểm thử
*   **Giả lập lỗi Gateway**: Hệ thống cho phép giả lập lỗi từ phía cổng thanh toán (`simulateGatewayFailure=true`) để kiểm tra logic xử lý lỗi.
*   **Sai chữ ký**: Webhook có chữ ký không khớp sẽ bị từ chối với lỗi `400 Bad Request`.
*   **Giao dịch không tồn tại**: Trả về lỗi nếu `transactionId` trong webhook không có trong hệ thống.

## Tiêu chí chấp nhận
*   Giao dịch thành công sẽ chuyển trạng thái Registration sang `SUCCESS` và sinh viên nhận được mã QR.
*   Hệ thống từ chối các webhook giả mạo (không có chữ ký hoặc chữ ký sai).
*   Trạng thái thanh toán được cập nhật đồng bộ giữa Web App, Backend và Sandbox.

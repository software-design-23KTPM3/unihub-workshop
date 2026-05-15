# Đặc tả: Xác thực và Phân quyền (Authentication & Authorization)

## Mô tả
Hệ thống sử dụng **Keycloak** làm Identity Provider trung tâm để quản lý người dùng và cấp phát Token. Toàn bộ cơ chế kiểm soát truy cập dựa trên mô hình **RBAC (Role-Based Access Control)** và tính chất **Stateless** của JWT để đảm bảo hệ thống có thể mở rộng (scale).

## Luồng chính
1. **Đăng nhập**: Người dùng đăng nhập qua Keycloak (Web/App). Sau khi thành công, Keycloak trả về **Access Token (JWT)**.
2. **Đính kèm Token**: Client đính kèm JWT vào header `Authorization: Bearer <token>` trong mọi request.
3. **Tiền xử lý tại Gateway**: 
    - Nginx Gateway (OpenResty) sử dụng Lua script để giải mã (decode) JWT (không verify chữ ký tại đây để tối ưu hiệu năng).
    - Trích xuất `sub` (userId) để thực hiện **Rate Limiting** theo người dùng.
    - Đính kèm `X-User-Id` và `X-User-Role` vào header trước khi chuyển tiếp request vào Backend.
4. **Xác thực và Phân quyền tại Backend**: 
    - Spring Boot Core API nhận request và thực hiện **Verify chữ ký (Signature)** và thời hạn (Expiration) của JWT bằng Public Key của Keycloak.
    - Sử dụng `JwtAuthConverter` để trích xuất Role từ `realm_access.roles` (prefix `ROLE_`).
    - Các API được bảo vệ bằng `@PreAuthorize` hoặc cấu hình trong `SecurityFilterChain`.

## Các Endpoint công khai (Public)
* `/api/health`: Kiểm tra trạng thái hệ thống.
* `/api/test/stress`: Endpoint dùng cho load test.
* `/api/payments/webhook`: Nhận callback từ cổng thanh toán.
* `/api/payments/sandbox-server-failure`: Endpoint giả lập lỗi server thanh toán.

## Kịch bản lỗi
* **Token không hợp lệ/Hết hạn**: Backend trả về mã `401 Unauthorized`.
* **Sai Role**: Người dùng không có quyền truy cập API (ví dụ Sinh viên truy cập API Admin). Backend trả về `403 Forbidden`.
* **Vượt quá Rate Limit**: Gateway trả về `429 Too Many Requests`.

## Ràng buộc
* **Stateless**: Không sử dụng Session tại Backend. Toàn bộ thông tin định danh nằm trong JWT.
* **Bảo mật**: Header `Authorization` phải được gửi qua HTTPS (trong môi trường Production).
* **Hiệu năng**: Rate limiting tại Gateway sử dụng Redis để lưu trữ bucket tokens, đảm bảo phản hồi cực nhanh.

## Tiêu chí chấp nhận
* Người dùng không có Token hợp lệ không thể truy cập các API bị bảo vệ.
* Rate limit hoạt động đúng theo từng người dùng và từng endpoint.
* Phân quyền (ADMIN, ORGANIZER, STUDENT) được kiểm soát chặt chẽ tại tầng Backend.
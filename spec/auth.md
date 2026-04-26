# Đặc tả: Xác thực và Phân quyền (Authentication & Authorization)

## Mô tả
Hệ thống sử dụng **Keycloak** làm Identity Provider trung tâm để quản lý người dùng và cấp phát Token. Toàn bộ cơ chế kiểm soát truy cập dựa trên mô hình **RBAC (Role-Based Access Control)** và tính chất **Stateless** của JWT để đảm bảo hệ thống có thể mở rộng (scale) không giới hạn khi tải cao.

## Luồng chính
1. **Đăng nhập**: Người dùng truy cập Web/App, bị điều hướng sang trang Login của Keycloak. Sau khi nhập đúng thông tin, Keycloak trả về **Access Token (JWT)** chứa thông tin UserID và Roles.
2. **Đính kèm Token**: Client đính kèm JWT vào header `Authorization: Bearer <token>` trong mọi request gửi lên hệ thống.
3. **Xác thực tại Gateway**: Nginx Gateway sử dụng Lua script để verify chữ ký (Signature) và thời gian hết hạn (Expiration) của JWT bằng Public Key của Keycloak.
4. **Phân quyền tại Backend**: Request hợp lệ được chuyển vào Spring Boot Core API. Tại đây, annotation `@PreAuthorize` sẽ kiểm tra danh sách Role trong JWT (STUDENT, ORGANIZER, CHECKIN_STAFF) để cho phép hoặc từ chối thực thi hàm nghiệp vụ.

## Kịch bản lỗi
* **Token hết hạn**: Gateway trả về mã `401 Unauthorized`. Client phải dùng Refresh Token (nếu có) hoặc bắt người dùng đăng nhập lại.
* **Sai Role**: Sinh viên cố truy cập API admin. Backend trả về `403 Forbidden`.
* **Keycloak Downtime**: Nếu Keycloak sập, người dùng cũ vẫn truy cập được cho đến khi Token hết hạn (do Gateway verify stateless). Người dùng mới sẽ không thể đăng nhập.

## Ràng buộc
* **Tính bảo mật**: Access Token chỉ có thời gian sống (TTL) ngắn (ví dụ: 5 phút) để giảm thiểu rủi ro khi bị lộ Token.
* **Hiệu năng**: Việc xác thực tại Gateway phải diễn ra cực nhanh (< 5ms) bằng cách cache Public Key trong Redis.

## Tiêu chí chấp nhận
* Người dùng chỉ nhìn thấy các chức năng thuộc quyền hạn của mình trên UI.
* Mọi request không có Token hoặc Token giả mạo đều bị chặn đứng tại Nginx.
* Danh sách Roles trong JWT khớp chính xác với cấu hình trên Keycloak.
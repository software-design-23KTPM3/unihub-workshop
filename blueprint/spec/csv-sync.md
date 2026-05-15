# Đặc tả: Đồng bộ dữ liệu sinh viên từ CSV (CSV Sync)

## Mô tả
Tính năng tự động đồng bộ danh sách sinh viên từ các file CSV được cung cấp bởi hệ thống của trường. Dữ liệu này được sử dụng để xác thực thông tin sinh viên khi đăng ký workshop và đồng bộ tài khoản lên hệ thống Keycloak.

## Luồng chính

### 1. Kích hoạt đồng bộ (Scheduled Job)
- Một job chạy định kỳ (cấu hình qua Cron expression trong `.env`) sẽ quét thư mục đầu vào (mặc định: `/service-sync-data`).
- Tìm kiếm tất cả các file có đuôi `.csv`.

### 2. Xử lý file CSV
Với mỗi file tìm thấy, `StudentSyncService` thực hiện:
1.  **Đọc file**: Sử dụng `CSVReader` để đọc dữ liệu.
2.  **Bỏ qua Header**: Dòng đầu tiên của file được coi là header và bị bỏ qua.
3.  **Schema**: File CSV yêu cầu 4 cột: `mssv`, `email`, `name`, `birthday` (định dạng `ddMMyyyy`).
4.  **Đồng bộ Database**:
    - Kiểm tra sinh viên theo `mssv`.
    - Nếu đã tồn tại: Cập nhật thông tin (`email`, `name`, `birthday`).
    - Nếu chưa có: Tạo mới bản ghi sinh viên với trạng thái `ACTIVE`.
    - Dữ liệu được lưu theo batch (mỗi batch 100 bản ghi).
5.  **Đồng bộ Keycloak**:
    - Gọi `KeycloakIntegrationService` để tạo hoặc cập nhật tài khoản người dùng trên Keycloak tương ứng với thông tin sinh viên.
6.  **Lưu trữ (Archive)**: Sau khi xử lý xong, file CSV có thể được di chuyển sang thư mục archive (đang cấu hình tùy chọn).

## Kịch bản lỗi
*   **Lỗi định dạng file**: Nếu file không đúng định dạng CSV hoặc thiếu cột, hệ thống ghi log lỗi và bỏ qua file đó.
*   **Lỗi đồng bộ từng dòng**: Nếu một dòng dữ liệu bị lỗi (ví dụ: sai format), hệ thống ghi log lỗi cho sinh viên đó và tiếp tục xử lý các dòng tiếp theo.
*   **Lỗi kết nối Keycloak**: Nếu không thể kết nối tới Keycloak, hệ thống ghi log và tiếp tục đồng bộ dữ liệu vào Database local.

## Ràng buộc
*   **Idempotency**: Việc chạy lại job với cùng một file CSV không gây ra dữ liệu trùng lặp nhờ cơ chế kiểm tra `mssv`.
*   **Hiệu năng**: Sử dụng Batch Save (`saveAll`) để tối ưu hóa việc ghi vào Database.
*   **Tách biệt**: Tính năng này chạy trong một service riêng biệt (`service-sync`), không ảnh hưởng đến hiệu năng của API chính.

## Tiêu chí chấp nhận
*   Dữ liệu sinh viên từ file CSV được cập nhật đầy đủ vào bảng `students` trong PostgreSQL.
*   Tài khoản sinh viên được tạo/cập nhật thành công trên Keycloak.
*   Hệ thống xử lý được file lớn mà không gây treo hoặc tràn bộ nhớ.

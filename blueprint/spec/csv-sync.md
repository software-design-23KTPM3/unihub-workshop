# Đặc tả: Đồng bộ dữ liệu sinh viên từ CSV (CSV Sync)

## Mô tả
Tính năng xử lý file CSV chứa thông tin sinh viên được export từ hệ thống cũ của trường và ban đêm. Dữ liệu này dùng để xác thực sinh viên khi đăng kí.

## Luồng chính
1. Kích hoạt đồng bộ
    - Scheduler (Cronjob) chạy vào 02:00AM mỗi ngày
    - Tạo syncId (UUID) để theo dõi toàn bộ job
2. Tìm và tải file CSV
    - Worker kết nối SFTP/ thư mục nội bộ
    - Lấy file mới nhất theo naming convention (vd: students_YYYYMMDD.csv)
    - Nếu có nhiều file => chọn file cuối cùng
3. Kiểm tra tính hợp lệ của file
    - Validate:
        - Định dạng CSV
        - Header đúng schema (mssv, email, name,...)
    - Nếu thất bại: Hủy bỏ job
4. Đọc dữ liệu theo stream (chunking processing)
    - Không load toàn bộ file vào RAM
    - Đọc từng chunk (500-1000 dòng)
    - Với mỗi record:
        - Xóa khoảng trắng
        - Chuẩn hóa email (lowercase)
        - Validate format MSSV/email
5. Xử lý dữ liệu
    - Kiểm tra: các file bắt buộc, trùng MSSV  trong cùng file
    - Nếu lỗi: bỏ qua record, ghi vào error buffer
6. Cập nhật dữ liệu vào Database theo batch
    - Với mỗi chunk:
        - Thực hiện: INSERT ... ON CONFLICT (mssv) DO UPDATE
        - Update các field: mssv, email, name, status
    - Commit theo batch
7. Ghi log đồng bộ
    - Lưu vào bảng sync_logs: syncId, total_records, success_count, error_count, status (SUCCESS/ PARTIAL/ FAILED), started_at / finished_at 
8. Xử lý file sau sync
    - Di chuyển file sang: /archive/YYYY/MM/DD/
9. Xuất file lỗi (nếu có):
    - Ghi file: error_log_syncId.csv. File chưa dòng lỗi, nguyên nhân

## Kịch bản lỗi
1. File CSV sai định dạng (vd: Header không đúng/ lệch cột)
    - Hủy bỏ job
    - Ghi log: FAILED
    - Không ghi dữ liệu vào database
2. Một số dòng dữ liệu lỗi (vd: thiếu email, MSSV trùng trong file)
    - Bỏ qua record
    - Ghi vào error_log
    - Tiếp tục xử lý các dòng khác
3. Không tim thấy file
    - Retry tối đa 3 lần, mỗi lần cách nhau tầm 15 phút
    - Nếu vẫn thất bại: ghi log FAILED và gửi thông báo lỗi
4. Lỗi database (deadlock/ connection lost)
    - Retry batch hiện tại (tối đa 3 lần)
    - Nếu vẫn thất bại: rollback batch đó, hủy bỏ job
5. Worker crash giữa chừng
    - Dựa vào sync_logs: Nếu status = RUNNING thì job có thể được retry lại từ đầu

## Ràng buộc
1. Memory Management:
    - Bắt buộc sử dụng streaming/chunking khi xử lý file lớn
    - Không sử dụng `readAll()` hoặc các API load toàn bộ dữ liệu vào RAM để tránh Out Of Memory / job failure
2. Performace 
    - Background Job Processing Strategy:
        - Job chạy vào giờ thấp điểm (night batch)
        - Kiểm soát connection pool để tránh ảnh hưởng API production
    - Database Write Optimization:
        - Batch size: 500–1000 records
        - Bắt buộc UNIQUE index trên mssv
        - Tránh table lock toàn bộ, ưu tiên batch upsert/partial update
3. Idempotency
    - Upsert đảm bảo: chạy lại job không tạo duplicate
    - Sycnc theo file snapshot (không incremental)
4. Tính nhất quán dữ liệu
    - Batch xử lý độc lập
    - Không partial write trong 1 batch
5. Tích hợp một chiều cô lập 
    - Job hoạt động theo mô hình read-only đối với CSV source
    - Chỉ thực hiện write vào PostgreSQL (target system)
    - Không có bất kỳ cơ chế ghi ngược (write-back) nào về hệ thống legacy của trường

## Tiêu chí chấp nhận
1. Performance
    - Sync file 50.000 dòng trong < 5 phút
    - RAM ổn định, không bị Out Of Memory
2. Độ bền hệ thống
    - File lỗi không làm crash toàn bộ job
    - Worker crash có thể retry an toàn
3. Tính chính xác dữ liệu
    - Không duplicate MSSV
    - Dữ liệu được update đúng
4. Logging & Observability
    - Có sync_logs cho mỗi job
    - Có file error_log cho dòng lỗi
5. Không ảnh hưởng hệ thống chính
    - User vẫn truy cập app bình thường trong lúc sync

# Đặc tả: Quản lý Workshop (Workshop Management)

## Mô tả

Tính năng cho phép Ban tổ chức (Admin/Organizer) thực hiện các thao tác quản lý vòng đời workshop: tạo mới, cập nhật thông tin, và huỷ workshop. Mặc dù về bản chất là các thao tác CRUD, luồng này đòi hỏi sự cẩn thận ở hai điểm cốt lõi:

- **Kiểm soát truy cập (RBAC):** Chỉ đúng vai trò mới được phép ghi dữ liệu — kiểm tra tại Gateway, không chỉ ở tầng giao diện.
- **Tính nhất quán dữ liệu (Cache Invalidation):** Khi Admin thay đổi thông tin trong database, bản sao đang được 12.000 sinh viên đọc từ Redis phải được làm mới ngay lập tức để tránh hiển thị dữ liệu cũ.

Chi tiết luồng xử lý AI Summary (tải lên PDF, tạo tóm tắt tự động) được mô tả riêng tại [`ai_summary.md`](./ai_summary.md).

---

## Luồng chính

### Các thành phần tham gia

| Thành phần | Vai trò |
|---|---|
| **Admin Web App** | Giao diện quản trị dành riêng cho Ban tổ chức |
| **API Gateway & Load Balancer** | Xác thực JWT, kiểm tra RBAC, phân tải request |
| **Workshop Service** | Xử lý logic nghiệp vụ tạo / cập nhật / huỷ workshop |
| **PostgreSQL** | Lưu trữ thông tin gốc của workshop (source of truth) |
| **Redis** | Cache danh sách và chi tiết workshop phục vụ sinh viên đọc tốc độ cao; lưu slot counter |
| **RabbitMQ + AI Worker** | Hàng đợi và tiến trình xử lý PDF bất đồng bộ — xem `ai_summary.md` |
| **Cloud Storage** (S3/MinIO) | Lưu trữ file PDF đính kèm workshop |

---

### Luồng 1 — Tạo mới / Cập nhật thông tin Workshop

Áp dụng khi Admin tạo workshop mới hoặc chỉnh sửa thông tin hiện có (đổi phòng, đổi giờ, cập nhật diễn giả, điều chỉnh số chỗ).

#### Bước 1 — Gửi yêu cầu (Admin Web App)

1. Admin điền form thông tin: tên workshop, diễn giả, phòng tổ chức, số ghế tối đa, ngày giờ, loại (miễn phí / có phí), giá (nếu có).
2. Frontend gửi `POST /api/admin/workshops` (tạo mới) hoặc `PATCH /api/admin/workshops/{id}` (cập nhật) kèm:
   - Header `Authorization: Bearer <JWT Token>`
   - Body: thông tin workshop

#### Bước 2 — Kiểm tra an ninh và phân quyền (API Gateway)

3. Gateway giải mã JWT và thực hiện **RBAC Check**:
   - Kiểm tra field `role` trong token payload.
   - Nếu `role` **không phải** `ADMIN` hoặc `ORGANIZER` → từ chối ngay với `403 Forbidden`. Sinh viên và nhân sự check-in không thể gọi các API này dù cố tình.
   - Nếu hợp lệ → chuyển request đến Workshop Service.

#### Bước 3 — Lưu trữ vào Database (Workshop Service)

4. Workshop Service validate dữ liệu đầu vào (tên không rỗng, số ghế > 0, thời gian hợp lệ, phòng không trùng lịch với workshop khác cùng khung giờ).
5. Ghi mới hoặc cập nhật bản ghi vào PostgreSQL.

#### Bước 4 — Đồng bộ Cache (Redis) 

6. Sau khi ghi database thành công, Workshop Service thực hiện **Cache Invalidation** ngay lập tức:
   - Xoá cache chi tiết: `DEL workshop_details:{id}`
   - Xoá cache danh sách: `DEL workshop_list`

   > Bước này là bắt buộc. Nếu bỏ qua, sinh viên sẽ đọc dữ liệu cũ từ Redis cho đến khi cache tự hết hạn — dẫn đến hiển thị sai phòng, sai giờ hoặc sai số chỗ.

7. **Chỉ khi tạo mới workshop:** Khởi tạo slot counter trên Redis:
   ```
   SET workshop_slots:{id} {max_seats}
   ```
   Key này là nền tảng cho luồng đăng ký (xem [`booking.md`](./booking.md)).

8. Trả về `200 OK` (cập nhật) hoặc `201 Created` (tạo mới) kèm thông tin workshop cho Admin Web App.

---

### Luồng 2 — Huỷ Workshop

Áp dụng khi Admin cần huỷ một workshop đã được công bố — tình huống nhạy cảm vì có thể đã có sinh viên đăng ký.

#### Bước 1 — Gửi yêu cầu huỷ (Admin Web App)

1. Admin chọn workshop cần huỷ và xác nhận hành động.
2. Frontend gửi `PATCH /api/admin/workshops/{id}/cancel` kèm JWT Token.

#### Bước 2 — Kiểm tra phân quyền (API Gateway)

3. Tương tự Luồng 1: Gateway kiểm tra `role`. Chỉ `ADMIN` hoặc `ORGANIZER` được phép huỷ. Trả `403 Forbidden` nếu không đủ quyền.

#### Bước 3 — Xử lý huỷ (Workshop Service)

4. Workshop Service cập nhật trạng thái workshop thành `CANCELLED` trong PostgreSQL — **không xoá bản ghi** để giữ lịch sử.
5. Đặt `available_seats = 0` trong database.
6. Xoá slot counter trên Redis: `DEL workshop_slots:{id}` — ngăn sinh viên đăng ký mới ngay lập tức.
7. Thực hiện Cache Invalidation: `DEL workshop_details:{id}`, `DEL workshop_list`.

#### Bước 4 — Thông báo cho sinh viên đã đăng ký (Bất đồng bộ)

8. Workshop Service publish một event lên RabbitMQ:
   ```
   Event: workshop.cancelled
   Payload: { workshop_id, workshop_name, cancelled_at }
   ```
9. **Notification Worker** consume event và gửi thông báo huỷ đến toàn bộ sinh viên đã có `registrations.status = SUCCESS` cho workshop này (qua app và email).
10. Trả về `200 OK` cho Admin ngay lập tức — không đợi thông báo gửi xong.

> Xử lý hoàn tiền cho sinh viên đã thanh toán là trách nhiệm của luồng thanh toán, không thuộc phạm vi file này — xem [`payment.md`](./payment.md).

---

## Kịch bản lỗi

### KL-01: Admin không có quyền (sai role)

**Tình huống:** Tài khoản nhân sự check-in hoặc sinh viên cố tình gọi API quản lý workshop.

**Xử lý:** Gateway kiểm tra `role` trong JWT và từ chối ngay với `403 Forbidden` trước khi request chạm đến Workshop Service. Không có bất kỳ thao tác database hay cache nào được thực hiện.

---

### KL-02: Dữ liệu đầu vào không hợp lệ

**Tình huống:** Số ghế nhập bằng 0, tên workshop rỗng, thời gian kết thúc trước thời gian bắt đầu, hoặc phòng đã có workshop khác trong cùng khung giờ.

**Xử lý:** Workshop Service validate và trả về `422 Unprocessable Entity` kèm thông báo lỗi cụ thể. Không ghi gì vào database và không chạm vào cache.

---

### KL-03: Ghi database thành công nhưng Cache Invalidation thất bại

**Tình huống:** Sau khi ghi PostgreSQL thành công, lệnh `DEL` trên Redis bị lỗi (Redis tạm thời không kết nối được).

**Xử lý:** Workshop Service retry Cache Invalidation tối đa 3 lần với exponential backoff. Nếu vẫn thất bại, ghi log lỗi cảnh báo và đặt TTL ngắn (ví dụ: 30 giây) cho các key liên quan thay vì xoá — cache sẽ tự hết hạn và được làm mới ở lần đọc tiếp theo. Dữ liệu gốc trong PostgreSQL vẫn đúng; sinh viên tối đa thấy thông tin cũ trong 30 giây.

---

### KL-04: Cập nhật số ghế xuống thấp hơn số đã đăng ký

**Tình huống:** Workshop đang có 50 sinh viên đã đăng ký, Admin cố cập nhật `max_seats = 40`.

**Xử lý:** Workshop Service kiểm tra số lượng `registrations.status = SUCCESS` trước khi lưu. Nếu `max_seats_mới < số_đã_đăng_ký` → trả về `409 Conflict` với thông báo rõ ràng: *"Số ghế tối đa không thể thấp hơn số sinh viên đã đăng ký (hiện có: 50)."* Không thực hiện bất kỳ thay đổi nào.

---

### KL-05: Huỷ workshop khi có sinh viên đang trong trạng thái PENDING thanh toán

**Tình huống:** Admin huỷ workshop trong khi có sinh viên vừa giữ chỗ và đang chờ thanh toán.

**Xử lý:** Workshop Service xoá slot counter trên Redis (`DEL workshop_slots:{id}`) ngay khi nhận lệnh huỷ — ngăn các slot mới bị giữ. Các bản ghi `PENDING` hiện có sẽ bị xử lý bởi slot expiry job (hết hạn tự nhiên sau 15 phút) hoặc qua reconciliation của Payment Service.

---

## Ràng buộc

### Phân quyền

- Chỉ tài khoản có `role = ADMIN` hoặc `role = ORGANIZER` mới được gọi các API ghi (`POST`, `PATCH`, `DELETE`) của workshop.
- Kiểm tra quyền phải diễn ra tại **API Gateway**, không chỉ tại tầng giao diện hoặc trong Workshop Service.
- Nhân sự check-in (`role = CHECKIN_STAFF`) và sinh viên (`role = STUDENT`) nhận `403 Forbidden` nếu gọi nhầm các endpoint này.

### Tính nhất quán dữ liệu

- Cache Invalidation (`DEL workshop_details:{id}`, `DEL workshop_list`) **phải được thực hiện ngay sau** khi ghi database thành công, trong cùng một luồng xử lý request — không được delay hoặc làm bất đồng bộ.
- Slot counter Redis (`workshop_slots:{id}`) phải được khởi tạo đúng bằng `max_seats` khi tạo mới workshop; phải được cập nhật đồng bộ khi `max_seats` thay đổi.
- Huỷ workshop không được xoá bản ghi khỏi database — chỉ cập nhật `status = CANCELLED` để giữ lịch sử và phục vụ audit.

### Hiệu năng

- Thời gian phản hồi cho thao tác tạo mới / cập nhật (bao gồm cả cache invalidation) phải dưới **300ms** trong điều kiện bình thường.
- Các thao tác quản lý workshop không được làm chậm hoặc gián đoạn luồng đọc danh sách workshop của sinh viên.

### Bảo mật

- JWT Token phải có thời gian hết hạn ngắn (khuyến nghị: 1 giờ) cho tài khoản Admin; cần có cơ chế refresh token.
- Mọi thao tác ghi (tạo, cập nhật, huỷ) phải được ghi vào audit log: `{ admin_id, action, workshop_id, timestamp, ip_address }`.

---

## Tiêu chí chấp nhận

### AC-01: Tạo mới workshop thành công

- Admin gửi thông tin hợp lệ → hệ thống trả về `201 Created` kèm thông tin workshop (bao gồm `id` vừa được tạo).
- Bản ghi xuất hiện trong PostgreSQL với đúng thông tin đã nhập.
- Redis có key `workshop_slots:{id}` với giá trị bằng `max_seats`.
- Cache `workshop_list` bị xoá — lần đọc tiếp theo của sinh viên sẽ lấy dữ liệu mới từ database.

### AC-02: Cập nhật thông tin workshop thành công

- Admin cập nhật phòng tổ chức → hệ thống trả về `200 OK`.
- Bản ghi trong PostgreSQL phản ánh phòng mới.
- Cache `workshop_details:{id}` và `workshop_list` bị xoá.
- Sinh viên truy cập trang chi tiết workshop ngay sau đó thấy thông tin phòng mới (không phải phòng cũ từ cache).

### AC-03: Phân quyền hoạt động đúng

- Gửi request tạo workshop với JWT của tài khoản sinh viên → nhận `403 Forbidden`; không có bản ghi nào được tạo trong database.
- Gửi request tạo workshop với JWT của tài khoản Admin hợp lệ → nhận `201 Created`.

### AC-04: Từ chối cập nhật số ghế không hợp lệ

- Workshop đang có 50 đăng ký thành công; Admin cố cập nhật `max_seats = 40` → nhận `409 Conflict` với thông báo lỗi cụ thể; dữ liệu trong database và Redis không thay đổi.

### AC-05: Huỷ workshop và thông báo sinh viên

- Admin huỷ workshop → hệ thống trả về `200 OK` ngay lập tức.
- Trạng thái workshop trong PostgreSQL chuyển thành `CANCELLED`; key `workshop_slots:{id}` bị xoá khỏi Redis.
- Trong vòng 60 giây, toàn bộ sinh viên có đăng ký `SUCCESS` nhận được thông báo huỷ qua app và email.
- Workshop không còn hiển thị (hoặc hiển thị trạng thái "Đã huỷ") trên trang danh sách của sinh viên.

### AC-06: Audit log ghi nhận đầy đủ

- Sau mỗi thao tác tạo / cập nhật / huỷ, bảng audit log có đúng 1 bản ghi mới với `admin_id`, `action`, `workshop_id`, và `timestamp` chính xác.

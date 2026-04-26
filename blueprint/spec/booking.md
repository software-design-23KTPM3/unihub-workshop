# Đặc tả: Đăng ký Workshop (Booking)

## Mô tả

Tính năng cho phép sinh viên đăng ký tham dự workshop trong Tuần lễ kỹ năng và nghề nghiệp. Hệ thống hỗ trợ hai loại workshop: **miễn phí** (xác nhận ngay) và **có thu phí** (chuyển sang luồng thanh toán trước khi xác nhận). Sau khi đăng ký thành công, sinh viên nhận được mã QR dùng để check-in tại cửa phòng.

Chi tiết luồng xử lý thanh toán (circuit breaker, idempotency, webhook, reconciliation) được mô tả riêng tại [`payment.md`](./payment.md).

---

## Luồng chính

### Các thành phần tham gia

| Thành phần | Vai trò |
|---|---|
| **Client** (Web / Mobile) | Giao diện sinh viên thao tác |
| **API Gateway & Load Balancer** | Xác thực JWT, kiểm tra rate limit, phân tải request |
| **Redis** | Lưu rate limit counter, idempotency key, atomic slot counter |
| **Backend Node** | Xử lý logic đăng ký, điều phối luồng thanh toán nếu có phí |
| **Payment Service** | Xử lý toàn bộ luồng thanh toán — xem chi tiết tại `payment.md` |
| **PostgreSQL** | Lưu trạng thái đăng ký cuối cùng (source of truth) |
| **RabbitMQ** | Message broker nhận event sau khi đăng ký thành công |
| **Notification Worker** | Tạo mã QR, gửi email và push notification bất đồng bộ |

---

### Bước 1 — Khởi tạo yêu cầu (Client)

1. Sinh viên bấm nút **"Đăng ký"** trên trang chi tiết workshop.
2. Frontend tự động sinh một **Idempotency-Key** dạng UUID v4 (ví dụ: `abc-123-def-456`) và lưu tạm vào bộ nhớ local của trình duyệt/app.
3. Frontend gửi `POST /api/registrations` kèm:
   - Header `Authorization: Bearer <JWT Token>`
   - Header `Idempotency-Key: abc-123-def-456`
   - Body: `{ workshop_id, student_id }`

---

### Bước 2 — Kiểm tra tại API Gateway & Load Balancer

4. Request chạm vào **API Gateway**. Hai thao tác được thực hiện tuần tự:

   **a. Xác thực & bảo vệ (Gateway Role):**
   - Verify chữ ký JWT — xác nhận đúng là tài khoản sinh viên hợp lệ.
   - Kiểm tra **Rate Limit** trên Redis theo `user_id`: tối đa 5 request/phút cho endpoint đăng ký.
     - Nếu vượt ngưỡng → trả về `429 Too Many Requests`, kèm header `Retry-After`.
     - Nếu hợp lệ → cho đi tiếp.

   **b. Phân phối tải (Load Balancer Role):**
   - Dùng thuật toán Round Robin chọn một Backend Node đang sẵn sàng (Node 1 / 2 / 3...) và chuyển request đến.

---

### Bước 3 — Kiểm tra idempotency và giữ chỗ (Backend Node)

5. Backend Node nhận request và thực hiện hai kiểm tra trên Redis:

   **a. Kiểm tra Idempotency-Key:**
   - Tra cứu key `idempotency:{Idempotency-Key}` trong Redis.
   - Nếu **đã tồn tại**: trả về ngay trạng thái giao dịch đã lưu, bỏ qua toàn bộ các bước tiếp theo (đây là request retry từ client).
   - Nếu **chưa tồn tại**: lưu key vào Redis với TTL = 24 giờ, tiếp tục xử lý.

   **b. Giành chỗ ngồi (Atomic Slot Counter):**
   - Gọi lệnh `DECR workshop_slots:{workshop_id}` trên Redis.
   - Nếu giá trị trả về **< 0**: hoàn lại slot (`INCR`), trả về `400 Bad Request` với thông báo "Workshop đã hết chỗ".
   - Nếu giá trị trả về **≥ 0**: chỗ đã được **giữ tạm thời** cho sinh viên này (slot reservation), tiếp tục bước thanh toán.

   > Lúc này slot đã bị trừ trong Redis nhưng chưa được ghi vào database — đây là trạng thái "giữ chỗ tạm" (pending). Nếu thanh toán thất bại hoặc bị huỷ, slot phải được hoàn trả.

---

### Bước 4 — Phân nhánh theo loại workshop

6. Backend Node kiểm tra trường `is_paid` của workshop:

   - **Workshop miễn phí:** Bỏ qua bước thanh toán hoàn toàn. Ghi ngay bản ghi `registrations.status = SUCCESS` vào PostgreSQL, chuyển thẳng đến Bước 5.

   - **Workshop có phí:** Chuyển sang **Payment Service** để xử lý thanh toán. Bản ghi được tạo với trạng thái `PENDING` trong khi chờ kết quả. Xem toàn bộ chi tiết luồng xử lý (circuit breaker, webhook, idempotency, reconciliation) tại [`payment.md`](./payment.md). Sau khi Payment Service xác nhận thành công, cập nhật `registrations.status = SUCCESS` và tiếp tục Bước 5.

   > Nếu thanh toán thất bại hoặc hết hạn, Payment Service hoàn trả slot và cập nhật trạng thái tương ứng. Luồng booking kết thúc tại đây mà không tiến đến Bước 5.

---

### Bước 5 — Xử lý bất đồng bộ (Notification & QR)

7. Sau khi `registrations.status = SUCCESS` được ghi vào database, Backend Node publish một event lên **RabbitMQ**:
    ```
    Event: student.registered.successfully
    Payload: { student_id, workshop_id, registration_id }
    ```
    Backend trả về `200 OK` với thông báo "Đăng ký thành công" ngay cho frontend — không đợi email hay QR.

8. **Notification Worker** (tiến trình độc lập) consume event từ RabbitMQ và thực hiện song song:
    - Tạo file ảnh mã QR từ `registration_id`.
    - Gửi email xác nhận kèm mã QR đính kèm.
    - Gửi push notification qua mobile app.

---

## Kịch bản lỗi

### KL-01: Sinh viên retry sau khi mạng bị ngắt giữa chừng

**Tình huống:** Frontend gửi request nhưng mất kết nối trước khi nhận phản hồi. Frontend tự động retry với cùng `Idempotency-Key`.

**Xử lý:** Bước 3a phát hiện key đã tồn tại trong Redis → trả về trạng thái giao dịch đã lưu, không tạo thêm giao dịch mới. Sinh viên nhận lại kết quả như lần đầu.

---

### KL-02: Nhiều sinh viên cùng giành chỗ cuối

**Tình huống:** Workshop còn 1 chỗ, 50 sinh viên cùng gửi request trong vài mili-giây.

**Xử lý:** Lệnh `DECR` trên Redis là **atomic** — chỉ đúng một request nhận giá trị `0` (thành công giữ chỗ); 49 request còn lại nhận giá trị âm và bị từ chối ngay với `400 Hết chỗ`. Không có race condition.

---

### KL-03: Dữ liệu đầu vào không hợp lệ

**Tình huống:** `workshop_id` không tồn tại, sinh viên đã đăng ký workshop này trước đó, hoặc workshop chưa mở đăng ký.

**Xử lý:** Validation tại Backend Node trước bước 3, trả về `422 Unprocessable Entity` với thông báo lỗi cụ thể. Không tiêu tốn tài nguyên Redis hay database.

---

### KL-04: Thanh toán thất bại hoặc bị huỷ

**Tình huống:** Sinh viên huỷ thanh toán, hoặc Payment Service báo về kết quả thất bại.

**Xử lý:** Payment Service hoàn trả slot và cập nhật trạng thái đăng ký. Chi tiết xử lý các trường hợp lỗi thanh toán (timeout, trừ tiền hai lần, cổng sập) xem tại [`payment.md`](./payment.md).

---

## Ràng buộc

### Hiệu năng

- Thời gian phản hồi bước 1–3 (từ request đến khi giữ chỗ thành công) phải dưới **500ms** ở điều kiện bình thường.
- Hệ thống phải chịu được đồng thời ít nhất **1.200 request/giây** tại đỉnh điểm mà không mất dữ liệu.
- Rate limit: tối đa **5 request/phút/user** cho endpoint `POST /api/registrations`.
- Idempotency key có TTL = **24 giờ**.

### Bảo mật

- Mọi request phải kèm JWT hợp lệ; token phải được verify tại Gateway, không chỉ tại Backend.
- Chỉ sinh viên đã xác thực (có tài khoản hợp lệ trong hệ thống, đã được đồng bộ từ CSV) mới được đăng ký.

### Tính nhất quán dữ liệu

- Số chỗ trống trên Redis và trong PostgreSQL phải được đồng bộ định kỳ (mỗi 5 phút) để phát hiện lệch lạc do lỗi bất thường.
- Trạng thái `SUCCESS` trong PostgreSQL là **source of truth** cuối cùng — Redis chỉ là bộ đệm tốc độ cao.
- Không bao giờ có hai bản ghi `SUCCESS` cùng `student_id` và `workshop_id` trong bảng `registrations`.

### Tách biệt lỗi (Fault Isolation)

- Sự cố Payment Service **không được** ảnh hưởng đến: xem lịch workshop, check-in, tra cứu thông tin đăng ký, hoặc đăng ký workshop miễn phí.
- Sự cố RabbitMQ / Notification Worker **không được** ảnh hưởng đến luồng đăng ký chính — việc chưa nhận được QR / email không đồng nghĩa với đăng ký thất bại.

---

## Tiêu chí chấp nhận

### AC-01: Đăng ký thành công — workshop miễn phí

- Sinh viên bấm "Đăng ký" → hệ thống trả về `200 OK` và `registrations.status = SUCCESS` ngay lập tức, không qua bước thanh toán.
- Sinh viên nhận được push notification và email xác nhận kèm mã QR trong vòng 60 giây.
- Mã QR quét được tại mobile app check-in và trả về thông tin đăng ký đúng.

### AC-02: Đăng ký thành công — workshop có phí

- Sinh viên bấm "Đăng ký" → hệ thống giữ chỗ tạm và chuyển sang luồng thanh toán.
- Sau khi Payment Service xác nhận thành công, `registrations.status = SUCCESS` được ghi vào database.
- Sinh viên nhận được mã QR trong vòng 60 giây sau khi thanh toán xong.

### AC-03: Không có hai sinh viên cùng nhận chỗ cuối

- Khi chạy test tải 100 concurrent requests cho một workshop còn 1 chỗ, đúng 1 request nhận kết quả giữ chỗ thành công; 99 request còn lại nhận `400 Hết chỗ`.
- Sau khi tất cả request hoàn tất, `available_seats` trong database bằng 0 (không âm, không dương).

### AC-04: Idempotency hoạt động đúng

- Gửi cùng một request (cùng `Idempotency-Key`) 3 lần liên tiếp → chỉ tạo đúng 1 bản ghi trong `registrations`; lần 2 và lần 3 trả về kết quả giống lần 1 với `200 OK`.

### AC-05: Rate limiting hoạt động đúng

- Gửi 6 request liên tiếp trong 1 phút từ cùng một tài khoản → request thứ 6 nhận `429 Too Many Requests` kèm header `Retry-After`.
- Sau khi hết cửa sổ thời gian (1 phút), tài khoản có thể gửi request bình thường trở lại.

### AC-06: Tính năng không liên quan hoạt động bình thường khi Payment Service gặp sự cố

- Khi Payment Service không khả dụng: sinh viên vẫn xem được danh sách workshop, thông tin chi tiết, số chỗ còn lại, và đăng ký được workshop miễn phí.
- Nhân sự vẫn check-in được bình thường qua mã QR đã tạo trước đó.

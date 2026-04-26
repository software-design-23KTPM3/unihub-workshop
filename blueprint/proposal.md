# UniHub Workshop — Project Proposal

---

## Vấn đề

### Bối cảnh hiện tại

Trường Đại học A tổ chức "Tuần lễ kỹ năng và nghề nghiệp" hàng năm — sự kiện kéo dài 5 ngày với 8–12 workshop diễn ra song song mỗi ngày tại nhiều phòng khác nhau. Toàn bộ quy trình đăng ký hiện đang được vận hành bằng Google Form kết hợp thông báo thủ công qua email.

### Tại sao Google Form không còn đủ?

Quy trình hiện tại tồn tại nhiều điểm yếu nghiêm trọng khi quy mô sự kiện ngày càng mở rộng:

**1. Không kiểm soát được số chỗ theo thời gian thực**
Google Form không có cơ chế giới hạn và khoá chỗ đăng ký ngay khi hết slot. Khi một workshop chỉ có 60 chỗ nhưng hàng trăm sinh viên cùng submit form trong vài giây, kết quả là danh sách đăng ký vượt sức chứa mà ban tổ chức chỉ phát hiện sau đó, gây bất công và phải xử lý thủ công.

**2. Không có quy trình thanh toán tích hợp**
Các workshop có thu phí phải xử lý thanh toán qua kênh riêng biệt (chuyển khoản thủ công, nộp tiền trực tiếp), không liên kết với hồ sơ đăng ký. Ban tổ chức phải đối chiếu thủ công giữa danh sách đăng ký và danh sách đã thanh toán — dễ sai sót và tốn nhiều nhân lực.

**3. Check-in thủ công, không đáng tin cậy**
Nhân sự tại cửa phòng phải đối chiếu danh sách in sẵn hoặc mở Google Sheet để xác nhận từng sinh viên. Quy trình này chậm, dễ nhầm lẫn và hoàn toàn không hoạt động được khi mất kết nối mạng.

**4. Thông báo phân tán, không nhất quán**
Email xác nhận được gửi thủ công hoặc bằng script rời rạc. Không có hệ thống nhắc nhở tự động, không có kênh thông báo thống nhất, và mỗi học kỳ muốn thêm kênh mới (Zalo, Telegram) lại phải viết lại từ đầu.

**5. Không đồng bộ được dữ liệu sinh viên**
Việc xác thực sinh viên đăng ký hoàn toàn dựa vào thông tin họ tự nhập — không có cơ chế kiểm tra với hệ thống quản lý sinh viên của trường, dẫn đến nguy cơ đăng ký giả mạo hoặc nhầm thông tin.

**6. Không có khả năng mở rộng**
Khi quy mô tăng lên 12.000 sinh viên truy cập trong 10 phút đầu mở đăng ký, Google Form không có bất kỳ cơ chế bảo vệ nào trước tải đột biến — toàn bộ hạ tầng có thể sập hoặc phản hồi chậm đến mức không sử dụng được.

### Hậu quả cụ thể

- Ban tổ chức mất 2–3 người-giờ mỗi ngày để xử lý đăng ký thủ công trong tuần lễ sự kiện.
- Xảy ra tranh chấp chỗ ngồi mỗi năm: ít nhất 5–10% sinh viên đăng ký thành công nhưng không có chỗ thực tế.
- Không có dữ liệu thống kê đáng tin cậy để cải thiện sự kiện các năm sau.
- Trải nghiệm sinh viên kém: không biết mình đã được xác nhận hay chưa, phải hỏi lại ban tổ chức.

---

## Mục tiêu

### Mục tiêu nghiệp vụ

| # | Mục tiêu | Tiêu chí đo lường |
|---|----------|-------------------|
| 1 | Số hoá toàn bộ quy trình đăng ký, từ xem thông tin đến nhận mã QR | 100% workshop trong tuần lễ được quản lý qua hệ thống |
| 2 | Loại bỏ tranh chấp chỗ ngồi | 0 trường hợp hai sinh viên cùng nhận được chỗ cuối cùng |
| 3 | Hỗ trợ tải đột biến khi mở đăng ký | Hệ thống ổn định với 12.000 sinh viên truy cập trong 10 phút đầu, trong đó 60% dồn vào 3 phút đầu tiên |
| 4 | Tích hợp check-in kỹ thuật số | Nhân sự check-in bằng QR, giảm thời gian xác nhận mỗi sinh viên xuống dưới 5 giây |
| 5 | Đảm bảo check-in liên tục kể cả khi mất mạng | 0% dữ liệu check-in bị mất khi kết nối gián đoạn |
| 6 | Tích hợp an toàn với thanh toán | 0 trường hợp trừ tiền hai lần; tính năng không liên quan đến thanh toán vẫn hoạt động khi cổng thanh toán gặp sự cố |
| 7 | Đồng bộ dữ liệu sinh viên tự động | Hệ thống tự nhập CSV hàng đêm mà không gián đoạn dịch vụ đang chạy |

### Mục tiêu kỹ thuật

- Xây dựng kiến trúc có khả năng mở rộng kênh thông báo (email → app → Telegram) mà không cần thay đổi logic nghiệp vụ.
- Thiết kế hệ thống phân quyền rõ ràng, kiểm soát truy cập theo vai trò tại mọi điểm cuối.
- Áp dụng các cơ chế bảo vệ hệ thống: rate limiting, circuit breaker, idempotency key.
- Đảm bảo tách biệt lỗi — sự cố của một thành phần (cổng thanh toán, AI, hệ thống cũ) không kéo sập toàn bộ dịch vụ.

---

## Người dùng và nhu cầu

### Sinh viên

 Khoảng 12.000 sinh viên của trường, đa dạng về trình độ kỹ thuật, truy cập chủ yếu qua điện thoại di động và trình duyệt web.

**Nhu cầu:**
- Xem toàn bộ lịch workshop trong tuần (tên, diễn giả, phòng, giờ, sơ đồ phòng, số chỗ còn lại theo thời gian thực).
- Đăng ký tham dự — cả workshop miễn phí lẫn có phí.
- Nhận xác nhận tức thì sau khi đăng ký thành công và mã QR để check-in.
- Nhận thông báo nhắc nhở trước sự kiện.

**Điều quan trọng nhất:** Tính minh bạch (biết còn chỗ không, đã đăng ký thành công chưa) và sự công bằng (không bị "giật mất chỗ" do lỗi hệ thống).

### Ban tổ chức

 Đội ngũ nội bộ của trường, khoảng 5–15 người, có quyền quản trị sự kiện.

**Nhu cầu:**
- Tạo mới, chỉnh sửa, đổi phòng, đổi giờ và huỷ workshop qua trang web admin.
- Tải lên file PDF giới thiệu workshop và nhận bản tóm tắt tự động từ AI.
- Theo dõi số lượng đăng ký theo thời gian thực, xem thống kê tham dự sau sự kiện.
- Quản lý tài khoản và phân quyền cho nhân sự check-in.

**Điều quan trọng nhất:** Kiểm soát hoàn toàn thông tin sự kiện và khả năng phản ứng nhanh khi cần thay đổi (đổi phòng đột xuất, huỷ workshop).

### Nhân sự check-in

 Tình nguyện viên hoặc nhân viên được phân công tại cửa mỗi phòng workshop, sử dụng điện thoại di động.

**Nhu cầu:**
- Quét mã QR của sinh viên để xác nhận tham dự nhanh chóng.
- Tiếp tục check-in ngay cả khi kết nối mạng không ổn định hoặc mất hoàn toàn.
- Dữ liệu tự động đồng bộ lên server khi mạng phục hồi, không cần thao tác thủ công.

**Điều quan trọng nhất:** Hoạt động được trong mọi điều kiện mạng — khu vực trong trường có vùng phủ sóng yếu.

---

## Phạm vi

### Thuộc phạm vi đồ án

**Tính năng nghiệp vụ:**
- Xem danh sách workshop, chi tiết (diễn giả, phòng, sơ đồ, số chỗ), lọc và tìm kiếm.
- Đăng ký workshop miễn phí và có phí, quản lý huỷ đăng ký.
- Hệ thống thông báo qua app và email, thiết kế mở rộng được sang kênh mới.
- Trang web admin: tạo/sửa/huỷ workshop, quản lý người dùng, xem thống kê.
- AI Summary: xử lý PDF và tạo tóm tắt tự động bằng mô hình AI.
- Mobile app check-in với hỗ trợ offline và đồng bộ dữ liệu.
- Đồng bộ dữ liệu sinh viên từ CSV hàng đêm.
- Sinh và xác thực mã QR.

**Cơ chế kỹ thuật:**
- Phân quyền theo vai trò (RBAC): sinh viên, ban tổ chức, nhân sự check-in.
- Rate limiting để bảo vệ API khỏi tải đột biến.
- Đảm bảo tính nhất quán chỗ ngồi (tránh race condition khi đăng ký đồng thời).
- Circuit breaker và graceful degradation cho cổng thanh toán.
- Idempotency key để chống trừ tiền hai lần.
- Cơ chế offline-first và đồng bộ dữ liệu check-in.
- Pipeline nhập CSV an toàn, xử lý file lỗi và dữ liệu trùng.

### Không thuộc phạm vi đồ án

- **Cổng thanh toán thật:** Hệ thống sẽ tích hợp với cổng thanh toán giả lập (mock/sandbox); không kết nối thật với ngân hàng hay ví điện tử trong phạm vi đồ án.
- **Hạ tầng production:** Không triển khai lên môi trường production thực tế của trường; hệ thống chạy trên môi trường local/staging bằng Docker.
- **API hệ thống quản lý sinh viên:** Hệ thống cũ không có API; phạm vi đồ án chỉ xử lý tích hợp một chiều qua CSV export.
- **Ứng dụng mobile native:** Mobile app check-in được xây dựng dưới dạng Progressive Web App (PWA) hoặc web app responsive, không phát hành lên App Store/Google Play.
- **Đa ngôn ngữ và quốc tế hoá:** Hệ thống chỉ hỗ trợ tiếng Việt trong phạm vi đồ án.
- **Tích hợp kênh thông báo mới (Telegram, Zalo):** Kiến trúc được thiết kế để hỗ trợ mở rộng, nhưng chỉ triển khai app notification và email trong đồ án này.

---

## Rủi ro và ràng buộc

### 1. Tranh chấp chỗ ngồi (Race Condition)

**Rủi ro:** Một workshop chỉ có 60 chỗ nhưng hàng trăm sinh viên cùng đăng ký trong vài giây ngay khi mở đăng ký. Nếu không có cơ chế khoá dữ liệu phù hợp, có thể xảy ra tình huống nhiều sinh viên cùng đọc "còn 1 chỗ" và tất cả cùng đăng ký thành công, dẫn đến tổng số đăng ký vượt quá sức chứa.

**Ràng buộc kỹ thuật:** Hệ thống phải đảm bảo tính nhất quán mạnh (strong consistency) cho thao tác ghi slot — cần dùng cơ chế optimistic/pessimistic locking hoặc atomic counter ở tầng database, kết hợp với distributed lock nếu có nhiều instance backend.

### 2. Tải đột biến khi mở đăng ký

**Rủi ro:** Dự kiến khoảng 12.000 sinh viên truy cập trong 10 phút đầu, 60% dồn vào 3 phút đầu tiên — tương đương khoảng 1.200 request/giây tại đỉnh điểm. Backend API không có cơ chế bảo vệ sẽ bị quá tải, dẫn đến lỗi toàn diện.

**Ràng buộc kỹ thuật:** Cần triển khai rate limiting ở tầng API gateway theo từng IP và từng user ID; sử dụng hàng đợi (queue) để san bằng tải cho các thao tác tốn tài nguyên; thiết kế trang chờ (waiting room) để đảm bảo tính công bằng.

### 3. Thanh toán không ổn định

**Rủi ro:** Cổng thanh toán có thể gặp sự cố (timeout, lỗi kết nối) đúng lúc sinh viên đang trong luồng đăng ký có phí. Nếu không xử lý đúng, có hai hậu quả: (a) sinh viên bị trừ tiền nhưng không nhận được chỗ (thanh toán thành công nhưng hệ thống không ghi nhận), hoặc (b) bị trừ tiền hai lần khi retry.

**Ràng buộc kỹ thuật:**
- Cần dùng idempotency key cho mọi request tạo giao dịch để đảm bảo mỗi giao dịch chỉ được xử lý đúng một lần dù client gửi lại bao nhiêu lần.
- Cần triển khai circuit breaker để cô lập lỗi: khi cổng thanh toán sập, các tính năng không liên quan (xem lịch, check-in, xem thông tin) vẫn hoạt động bình thường.
- Trạng thái thanh toán cần được lưu và có cơ chế reconciliation để phát hiện và xử lý các giao dịch treo.

### 4. Check-in offline

**Rủi ro:** Một số khu vực trong trường có kết nối mạng không ổn định. Nhân sự check-in có thể mất mạng trong khi đang xác nhận sinh viên. Nếu app yêu cầu kết nối liên tục, toàn bộ quy trình check-in tại khu vực đó sẽ tê liệt.

**Ràng buộc kỹ thuật:** Mobile app check-in phải hoạt động theo mô hình offline-first: lưu dữ liệu check-in vào bộ nhớ local ngay lập tức, đánh dấu trạng thái "chờ đồng bộ", và tự động đồng bộ lên server khi kết nối phục hồi. Cần xử lý xung đột khi có nhiều thiết bị check-in cùng lúc (một sinh viên được quét ở hai cửa).

### 5. Tích hợp một chiều với hệ thống quản lý sinh viên

**Rủi ro:** Hệ thống cũ của trường không có API. Cách duy nhất để lấy dữ liệu sinh viên là qua file CSV được export theo lịch cố định vào ban đêm. File CSV có thể chứa lỗi định dạng, dữ liệu trùng, hoặc encoding sai. Nếu quá trình nhập CSV thất bại hoặc làm hỏng dữ liệu, hệ thống đang chạy ban ngày có thể bị ảnh hưởng.

**Ràng buộc kỹ thuật:**
- Pipeline nhập CSV phải chạy độc lập, không ảnh hưởng đến database đang phục vụ request.
- Cần validate và làm sạch dữ liệu trước khi import (kiểm tra format, phát hiện trùng lặp, xử lý encoding UTF-8).
- Cần cơ chế rollback hoặc staging: import vào bảng tạm, kiểm tra, rồi mới merge vào bảng chính.
- Phải có logging và alerting khi import thất bại để ban tổ chức phát hiện kịp thời.

### 6. Kiểm soát truy cập

**Rủi ro:** Trang admin và chức năng quét QR là thông tin nhạy cảm. Nếu phân quyền không chặt chẽ, sinh viên có thể truy cập chức năng quản trị, hoặc nhân sự check-in có thể xem/chỉnh sửa dữ liệu không thuộc phạm vi.

**Ràng buộc kỹ thuật:** Cần triển khai RBAC với ít nhất ba vai trò rõ ràng (sinh viên, ban tổ chức, nhân sự check-in), kiểm tra quyền tại mọi API endpoint, không chỉ ở tầng giao diện. Token xác thực cần có thời gian hết hạn và cơ chế thu hồi.

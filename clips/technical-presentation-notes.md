# UniHub Workshop - Ghi chú trình bày kỹ thuật

File này dùng để quay video trình bày các vấn đề kỹ thuật theo requirement. Mỗi mục gồm: requirement liên quan, cách hệ thống xử lý, flow demo, và code minh hoạ nên mở khi quay.

## 1. Chống tranh chấp chỗ ngồi bằng Redis + Lua

### Requirement liên quan

Một workshop chỉ có 60 chỗ nhưng có hàng trăm sinh viên đăng ký cùng lúc. Hệ thống phải đảm bảo không có hai sinh viên cùng nhận được chỗ cuối cùng.

### Cách xử lý trong project

Backend không trừ chỗ trực tiếp trong PostgreSQL ở bước đầu tiên. Khi sinh viên bấm đăng ký, backend gọi Redis trừ chỗ bằng một Lua script atomic. Redis giữ:

- `workshop_slots:{workshopId}`: số ghế còn lại.
- `workshop_meta:{workshopId}`: status và registration window.
- `workshop_registrations:{workshopId}`: set các sinh viên đã giữ chỗ.

Lua script chạy trong Redis nên các lệnh check trùng, check status, check window, `DECR`, và `SADD` được thực hiện như một khối atomic.

### Code minh hoạ

File: `src/backend/src/main/java/com/unihub/backend/core/service/impl/RedisServiceImpl.java`

```java
public Long registerUserInRedis(UUID workshopId, String userId) {
    String userSetKey = "workshop_registrations:" + workshopId;
    String slotKey = WORKSHOP_SLOTS_PREFIX + workshopId;
    String metaKey = WORKSHOP_META_PREFIX + workshopId;

    String script = "if redis.call('EXISTS', KEYS[2]) == 0 or redis.call('EXISTS', KEYS[3]) == 0 then " +
            "  return -3 " +
            "end " +
            "if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then " +
            "  return -1 " +
            "end " +
            "local status = redis.call('HGET', KEYS[3], 'status') " +
            "if status ~= 'ACTIVE' then " +
            "  return -4 " +
            "end " +
            "local remaining = redis.call('DECR', KEYS[2]) " +
            "if remaining < 0 then " +
            "  redis.call('INCR', KEYS[2]) " +
            "  return -2 " +
            "end " +
            "redis.call('SADD', KEYS[1], ARGV[1]) " +
            "return 1";

    return redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            Arrays.asList(userSetKey, slotKey, metaKey),
            userId,
            String.valueOf(java.time.Instant.now().getEpochSecond()));
}
```

### Lỗi và rollback

Nếu Redis đã giữ chỗ thành công nhưng insert DB thất bại, backend rollback lại Redis.

```java
public void rollbackRegistration(UUID workshopId, String userId) {
    String script = "local removed = redis.call('SREM', KEYS[1], ARGV[1]) " +
            "if removed == 1 and redis.call('EXISTS', KEYS[2]) == 1 then " +
            "  redis.call('INCR', KEYS[2]) " +
            "end " +
            "return removed";

    redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            Arrays.asList(userSetKey, slotKey),
            userId);
}
```

### Flow demo

1. Mở workshop còn ít chỗ.
2. Gọi nhiều request đăng ký cùng lúc.
3. Chỉ tối đa `maxSeats` registration thành công.
4. Request sau khi hết chỗ nhận lỗi sold out.

---

## 2. Kiểm soát tải đột biến bằng Token Bucket Rate Limiting

### Requirement liên quan

Khoảng 12.000 sinh viên truy cập trong 10 phút đầu, 60% dồn vào 3 phút đầu. Backend API cần được bảo vệ, client spam request phải bị chặn.

### Cách xử lý trong project

Gateway Nginx chạy Lua script trước khi forward request vào backend. Mỗi user có bucket riêng theo `path + user_id`. Redis lưu số token hiện tại và thời điểm refill cuối. Nếu hết token, gateway trả `429 Too Many Requests`, backend không bị gọi.

### Code minh hoạ

File: `src/service-gateway/rate_limit.lua`

```lua
local function check_rate_limit(red, user_id)
    local capacity = 20
    local refill_rate = 5
    local now = ngx.now()
    local path = ngx.var.uri
    local key = "rate_limit:" .. path .. ":" .. user_id

    local script = [[
        local key = KEYS[1]
        local cap = tonumber(ARGV[1])
        local rate = tonumber(ARGV[2])
        local now = tonumber(ARGV[3])

        local data = redis.call('HMGET', key, 'tokens', 'last_time')
        local last_tokens = tonumber(data[1]) or cap
        local last_time = tonumber(data[2]) or now

        local delta = math.max(0, now - last_time) * rate
        local current_tokens = math.min(cap, last_tokens + delta)

        if current_tokens >= 1 then
            local new_tokens = current_tokens - 1
            redis.call('HMSET', key, 'tokens', new_tokens, 'last_time', now)
            redis.call('EXPIRE', key, math.ceil((cap - new_tokens) / rate) + 1)
            return 1
        else
            return 0
        end
    ]]

    local res, err = red:eval(script, 1, key, capacity, refill_rate, now)
    return res == 1, err
end
```

Khi bị limit:

```lua
if not allowed then
    ngx.status = 429
    ngx.say(cjson.encode({
        error = "Too Many Requests",
        message = "Calm down, User " .. safe_user_id .. "!"
    }))
    return ngx.exit(429)
end
```

### Flow demo

1. Login sinh viên.
2. Spam nút register hoặc dùng script gửi request liên tục.
3. Sau khi vượt ngưỡng, gateway trả 429.
4. Các API khác vẫn sống, backend không bị quá tải.

---

## 3. Luồng đăng ký workshop có phí

### Requirement liên quan

Workshop có thể miễn phí hoặc có thu phí. Sau khi đăng ký workshop có phí, sinh viên cần thanh toán trước khi có QR hợp lệ.

### Cách xử lý trong project

Luồng đăng ký luôn bắt đầu bằng reserve seat trong Redis. Sau đó:

- Workshop miễn phí: registration có status `SUCCESS`, sinh QR, gửi notification/email.
- Workshop có phí: registration có status `PENDING`, tạo transaction `PENDING`, sinh viên được redirect sang trang payment.

Task insert DB chạy async, nên frontend payment page có cơ chế retry/poll nếu registration chưa kịp persist.

### Code minh hoạ

File: `src/backend/src/main/java/com/unihub/backend/core/service/impl/RegistrationServiceImpl.java`

```java
Long result = redisService.registerUserInRedis(workshopId, studentMssv);
if (result == null || result != 1) {
    if (result != null && result == -2) {
        throw new WorkshopSoldOutException("Workshop is sold out");
    }
    throw new InvalidWorkshopException("Workshop is not available for registration");
}

Workshop workshop = workshopRepository.findById(workshopId)
        .orElseThrow(() -> new RuntimeException("Workshop not found"));

RegistrationStatus initialStatus = workshop.getIsPaid()
        ? RegistrationStatus.PENDING
        : RegistrationStatus.SUCCESS;

startAsyncRegistrationTasks(
        registrationId,
        idempotencyKey,
        student.getMssv(),
        workshop.getId(),
        initialStatus);

return RegistrationResponse.builder()
        .registrationId(registrationId)
        .status(initialStatus)
        .message(initialStatus == RegistrationStatus.SUCCESS
                ? "Registration successful."
                : "Registration initiated. Please check your 'Order History'...")
        .build();
```

Tạo transaction cho workshop có phí:

```java
if (workshop.getIsPaid()) {
    transactionRepository.save(Transaction.builder()
            .registration(savedRegistration)
            .amount(workshop.getPrice())
            .status(TransactionStatus.PENDING)
            .idempotencyKey(idempotencyKey)
            .provider("SANDBOX")
            .paymentUrl("sandbox://payments/" + savedRegistration.getId())
            .expiresAt(ZonedDateTime.now().plusMinutes(30))
            .build());
}
```

Frontend redirect sau khi reserve workshop có phí.

File: `src/app-web/src/pages/student/StudentWorkshopDetailPage.jsx`

```jsx
const registration = await registerWorkshop(workshop.id);
const registrationId = registration.registrationId || registration.id;

if (registration.status === 'PENDING') {
  setMyRegistration({
    id: registrationId,
    workshop,
    workshopId: workshop.id,
    status: registration.status,
    paymentStatus: 'PENDING',
  });
  navigate(`/student/tickets/${registrationId}/payment`);
  return;
}
```

### Flow demo

1. Đăng ký workshop miễn phí: thấy success, ticket có QR.
2. Đăng ký workshop có phí: thấy pending, frontend redirect đến trang payment.
3. Thanh toán success: backend nhận webhook, registration chuyển `SUCCESS`, QR mới hợp lệ.

---

## 4. Circuit Breaker cho cổng thanh toán không ổn định

### Requirement liên quan

Nếu cổng thanh toán gặp sự cố kéo dài, sinh viên vẫn phải xem được lịch workshop. Payment fail không được làm sập backend.

### Cách xử lý trong project

Backend gọi payment sandbox qua `PaymentGatewayClient`. Client này được bọc bởi Resilience4j Circuit Breaker. Khi số lần lỗi vượt ngưỡng, circuit chuyển sang `OPEN`, các request thanh toán tiếp theo bị chặn nhanh và trả thông báo "payment unavailable". Các API workshop/registration khác không phụ thuộc payment sandbox nên vẫn hoạt động.

### Code minh hoạ

File: `src/backend/src/main/java/com/unihub/backend/core/service/impl/PaymentGatewayClient.java`

```java
public SandboxPaymentCreateResponse createPayment(SandboxPaymentCreateRequest request) {
    try {
        return paymentGatewayCircuitBreaker.executeSupplier(() -> restClient.post()
                .uri(baseUrl + "/sandbox/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(SandboxPaymentCreateResponse.class));
    } catch (Exception ex) {
        throw new PaymentGatewayUnavailableException(
                "Dịch vụ thanh toán đang tạm gián đoạn. Chỗ của bạn vẫn được giữ, vui lòng thử lại sau.");
    }
}
```

File: `src/backend/src/main/resources/application.yml`

```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentGateway:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 4
        failureRateThreshold: 50
        waitDurationInOpenState: 20s
        permittedNumberOfCallsInHalfOpenState: 2
        minimumNumberOfCalls: 2
        automaticTransitionFromOpenToHalfOpenEnabled: true
```

Sandbox có endpoint server-fail để tạo lỗi server-side thật sự, không phải "user declined payment".

File: `src/backend/src/main/java/com/unihub/backend/core/service/impl/PaymentServiceImpl.java`

```java
for (int i = 0; i < 2; i++) {
    try {
        paymentGatewayClient.createPayment(failedRequest);
    } catch (Exception ignored) {
        // Expected: this demo endpoint intentionally records gateway failures.
    }
}
```

### Flow demo

1. Tạo registration có phí.
2. Vào trang payment.
3. Trong sandbox, bấm nút mô phỏng lỗi server thanh toán.
4. Backend gọi payment sandbox với `simulateGatewayFailure=true` để record failure.
5. Sau khi đạt ngưỡng, quay lại bấm pay: backend trả payment unavailable.
6. Mở danh sách workshop: vẫn load bình thường.

---

## 5. Idempotency Key chống đăng ký/thanh toán lặp

### Requirement liên quan

Client có thể retry do timeout. Hệ thống phải đảm bảo không tạo nhiều registration/transaction và không trừ tiền hai lần.

### Cách xử lý trong project

Frontend tạo `idempotencyKey` UUID cho request register/payment. Backend lưu key vào registration/transaction. Nếu cùng một key gửi lại, backend trả về kết quả cũ. Payment sandbox cũng lưu map `idempotencyKey -> paymentId`, nên retry request tạo payment không sinh payment session mới.

### Code minh hoạ

File: `src/app-web/src/services/registrationService.js`

```js
export function createIdempotencyKey() {
  if (globalThis.crypto?.randomUUID) {
    return globalThis.crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, ...);
}

export function registerWorkshop(workshopId, options = {}) {
  const idempotencyKey = options.idempotencyKey || createIdempotencyKey();
  return httpClient.post(
    API_ENDPOINTS.registrations.create,
    { workshopId, idempotencyKey },
  );
}
```

File: `src/backend/src/main/java/com/unihub/backend/core/service/impl/RegistrationServiceImpl.java`

```java
UUID idempotencyKey = request.getIdempotencyKey() == null
        ? UUID.randomUUID()
        : request.getIdempotencyKey();

if (request.getIdempotencyKey() != null) {
    Registration existingRegistration = registrationRepository
            .findByIdempotencyKey(idempotencyKey)
            .orElse(null);
    if (existingRegistration != null) {
        return toCreateResponse(existingRegistration);
    }
}
```

File: `src/service-payment/src/main/java/com/unihub/payment/service/PaymentSandboxService.java`

```java
private final Map<UUID, String> paymentIdByIdempotencyKey = new ConcurrentHashMap<>();

public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
    String existingPaymentId = paymentIdByIdempotencyKey.get(request.getIdempotencyKey());
    if (existingPaymentId != null) {
        return toResponse(paymentsById.get(existingPaymentId));
    }

    SandboxPayment payment = new SandboxPayment();
    payment.setPaymentId("sandbox_" + UUID.randomUUID());
    payment.setIdempotencyKey(request.getIdempotencyKey());

    paymentsById.put(payment.getPaymentId(), payment);
    paymentIdByIdempotencyKey.put(payment.getIdempotencyKey(), payment.getPaymentId());
    return toResponse(payment);
}
```

### Flow demo

1. Gửi hai request register cùng `idempotencyKey`.
2. Kết quả trả cùng registration.
3. Start payment hai lần cùng key.
4. Sandbox trả cùng payment session, không tạo transaction mới.

---

## 6. Check-in offline trên mobile app và đồng bộ lại

### Requirement liên quan

Nhân sự check-in ở khu vực mất mạng vẫn phải scan QR và ghi nhận check-in. Khi có mạng lại, dữ liệu phải đồng bộ lên backend, không mất.

### Cách xử lý trong project

Mobile app dùng Room DB lưu local registration và pending check-in events. QR scanner ưu tiên verify offline. Nếu local chưa có ticket, app thử fetch ticket từ backend để cache, sau đó check-in offline. Khi check-in thành công local, app tạo pending event và enqueue WorkManager sync.

### Code minh hoạ

File: `src/app-mobile/app/src/main/java/com/example/unihubworkshop/features/workshop/presentation/view/QRScannerView.java`

```java
String registrationId = extractRegistrationId(rawData);
if (registrationId == null) {
    Toast.makeText(this, "Invalid QR Code.", Toast.LENGTH_SHORT).show();
    return;
}

Executors.newSingleThreadExecutor().execute(() -> {
    WorkshopRepository repo = new WorkshopRepositoryImpl(QRScannerView.this);
    CheckinResult result = repo.verifyOfflineCheckinDetailed(registrationId, currentWorkshopId);

    if (result == CheckinResult.INVALID_TICKET) {
        result = fetchAndCacheRegistrationThenCheckIn(registrationId, currentWorkshopId, repo);
    }

    if (finalResult == CheckinResult.SUCCESS) {
        CheckinSyncWorker.enqueue(QRScannerView.this);
        finish();
    }
});
```

File: `src/app-mobile/app/src/main/java/com/example/unihubworkshop/features/workshop/data/repository/WorkshopRepositoryImpl.java`

```java
public CheckinResult verifyOfflineCheckinDetailed(String qrCode, String workshopId) {
    RegistrationEntity registration = db.registrationDao().findByQrCode(qrCode, workshopId);
    if (registration == null) return CheckinResult.INVALID_TICKET;
    if ("CHECKED_IN".equals(registration.status)) return CheckinResult.ALREADY_CHECKED_IN;
    if (!"SUCCESS".equals(registration.status)) return CheckinResult.INVALID_TICKET;

    db.registrationDao().markAsCheckedIn(registration.id);

    CheckinEventEntity event = new CheckinEventEntity(
            UUID.randomUUID().toString(),
            registration.studentId,
            workshopId,
            registration.id,
            registration.qrCode,
            staffId,
            deviceId,
            timestamp,
            System.currentTimeMillis()
    );
    db.checkinEventDao().insert(event);
    return CheckinResult.SUCCESS;
}
```

File: `src/app-mobile/app/src/main/java/com/example/unihubworkshop/features/workshop/data/sync/CheckinSyncWorker.java`

```java
List<CheckinEventEntity> pendingEntities = db.checkinEventDao().getPendingEvents();
Response<Void> response = api.syncCheckins(pending).execute();
if (response.isSuccessful()) {
    db.checkinEventDao().deleteSynced(pendingIds);
    return Result.success();
}
if (response.code() >= 500 || response.code() == 429) {
    return Result.retry();
}
```

Backend sync:

File: `src/backend/src/main/java/com/unihub/backend/core/service/impl/CheckinServiceImpl.java`

```java
public void syncCheckins(List<CheckinEvent> events) {
    for (CheckinEvent event : events) {
        findRegistration(event)
            .filter(r -> r.getStatus() == RegistrationStatus.SUCCESS)
            .ifPresentOrElse(r -> {
                r.setStatus(RegistrationStatus.CHECKED_IN);
                r.setCheckedInAt(event.getCheckinAt());
                registrationRepository.save(r);
            }, () -> log.warn("Ignored check-in event {}", event.getClientEventId()));
    }
}
```

### Flow demo

1. Đăng nhập app bằng STAFF/CHECKIN_STAFF.
2. Tắt mạng mobile/emulator.
3. Scan QR của registration `SUCCESS`.
4. App báo check-in recorded và lưu pending event.
5. Bật mạng lại, WorkManager sync.
6. Backend registration chuyển `CHECKED_IN`.

---

## 7. QR ticket chỉ hợp lệ sau khi registration thành công

### Requirement liên quan

Sinh viên nhận QR để check-in sau khi đăng ký thành công. Với workshop có phí, QR chỉ nên dùng được sau khi thanh toán thành công.

### Cách xử lý trong project

Registration lưu `qrCode` là UUID registration. Tuy nhiên backend chỉ cho download QR PNG khi status là `SUCCESS` hoặc `CHECKED_IN`. Email thanh toán pending không gửi QR. Sau webhook success, backend cập nhật registration thành `SUCCESS` và gửi notification có QR image.

### Code minh hoạ

File: `src/backend/src/main/java/com/unihub/backend/core/service/impl/RegistrationServiceImpl.java`

```java
private String generateQrCode(UUID registrationId) {
    return registrationId.toString();
}

public byte[] getRegistrationQrPng(UUID id, Authentication authentication) {
    Registration registration = registrationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Registration not found"));
    ensureCanViewRegistration(registration, authentication);

    if (registration.getStatus() != RegistrationStatus.SUCCESS
            && registration.getStatus() != RegistrationStatus.CHECKED_IN) {
        throw new InvalidWorkshopException("QR is only available after registration is confirmed");
    }

    return generateQrPng(registration.getQrCode());
}
```

File: `src/backend/src/main/java/com/unihub/backend/core/service/impl/PaymentServiceImpl.java`

```java
if ("SUCCESS".equalsIgnoreCase(request.getStatus())) {
    if (transaction.getStatus() != TransactionStatus.SUCCESS) {
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setPaidAt(request.getPaidAt() == null ? ZonedDateTime.now() : request.getPaidAt());
        registration.setStatus(RegistrationStatus.SUCCESS);
        registrationRepository.save(registration);
        sendPaymentSuccessNotification(registration);
    }
}
```

### Flow demo

1. Đăng ký paid workshop.
2. Ticket pending: QR không hiện/không cho tải.
3. Thanh toán success.
4. Ticket success: QR hiển thị và scan được.

---

## 8. Notification qua email và in-app notification

### Requirement liên quan

Sau khi đăng ký thành công, sinh viên nhận thông báo qua app và email. Hệ thống cần dễ mở rộng kênh mới như Telegram.

### Cách xử lý trong project

Backend tạo in-app notification trong DB và đẩy message email vào RabbitMQ. Notification consumer đọc message từ queue, render email HTML, và gửi email. Cách này tách kênh notification ra khỏi registration flow; nếu SMTP chậm, registration không bị block.

### Code minh hoạ

File: `src/backend/src/main/java/com/unihub/backend/core/service/impl/RegistrationServiceImpl.java`

```java
private void sendNotification(Workshop workshop, Student student, RegistrationStatus status, String qrCode) {
    boolean pendingPayment = status == RegistrationStatus.PENDING;
    if (!pendingPayment) {
        Notification inAppNotification = Notification.builder()
                .student(student)
                .workshop(workshop)
                .type(NotificationType.IN_APP)
                .content("You have successfully registered for the workshop: " + workshop.getName()
                        + ". Your QR ticket is ready.")
                .status(NotificationStatus.PENDING)
                .build();
        notificationRepository.save(inAppNotification);
    }

    NotificationData data = NotificationData.builder()
            .title(pendingPayment ? "REGISTER WORKSHOP ... PENDING PAYMENT" : "REGISTER WORKSHOP ... SUCCESS")
            .to(student.getEmail())
            .qrPayload(pendingPayment ? null : qrCode)
            .qrImageBase64(pendingPayment ? null : qrImageBase64(qrCode))
            .workshopTitle(workshop.getName())
            .workshopTime(formatWorkshopTime(workshop))
            .workshopRoom(workshop.getRoom())
            .workshopSpeaker(workshop.getSpeaker())
            .build();

    rabbitTemplate.convertAndSend(
            RabbitConfig.NOTIFICATION_EXCHANGE,
            RabbitConfig.NOTIFICATION_ROUTING_KEY,
            NotificationRequest.builder().type("EMAIL").data(data).build());
}
```

### Cách mở rộng Telegram

Thêm channel mới vào notification consumer:

```java
public interface NotificationChannel {
    boolean supports(String type);
    void send(NotificationData data);
}
```

Sau đó thêm `TelegramNotificationChannel implements NotificationChannel`, không cần sửa registration service; backend vẫn publish `NotificationRequest`.

### Flow demo

1. Register free workshop: in-app notification được tạo, email có QR image.
2. Register paid workshop: email pending payment, không có QR.
3. Payment success: email success có workshop detail và QR image.
4. Mobile polling notification endpoint để hiển thị thông báo mới.

---

## 9. AI Summary từ file PDF và hiển thị markdown trên frontend

### Requirement liên quan

Organizer upload PDF giới thiệu workshop. Hệ thống tách text, làm sạch, gửi sang AI model để tạo summary hiển thị ở trang chi tiết.

### Cách xử lý trong project

Backend lưu PDF và publish message vào RabbitMQ AI summary queue. `consumer-summary` đọc queue, lấy PDF, extract text bằng PDFBox, clean text, chunk nếu quá dài, gọi Spring AI `ChatModel`, sau đó update `summary_text` và `summary_status`.

Frontend render summary bằng component markdown nhỏ, hỗ trợ heading, bold, bullet list, và xử lý `\n`.

### Code minh hoạ

File: `src/consumer-summary/src/main/java/com/unihubworkshop/worker/AISummaryWorker.java`

```java
@RabbitListener(queues = RabbitConfig.AI_SUMMARY_QUEUE)
public void processPdf(String workshopId) throws InterruptedException {
    Workshop workshop = workshopRepository.findById(UUID.fromString(cleanId)).orElse(null);
    if (workshop == null || workshop.getPdfUrl() == null) {
        return;
    }

    updateStatus(workshop, SummaryStatus.PROCESSING);

    try {
        String summary = generateSummaryForWorkshop(workshop);
        workshop.setSummaryText(summary);
        workshop.setSummaryStatus(SummaryStatus.COMPLETED);
    } catch (Exception e) {
        workshop.setSummaryStatus(SummaryStatus.FAILED);
    }

    workshopRepository.save(workshop);
}
```

Extract và clean PDF:

```java
private String generateSummaryForWorkshop(Workshop workshop) throws IOException, InterruptedException {
    String extractedText = extractTextFromPdf(workshop.getPdfUrl());
    String cleanText = cleanText(extractedText);

    if (cleanText.length() <= TEXT_THRESHOLD) {
        return aiSummaryService.generateResponse(cleanText);
    } else {
        return processChunkedText(cleanText);
    }
}

private String cleanText(String text) {
    return text.replaceAll("(?m)^\\s*[0-9]+\\s*$", "")
            .replaceAll("(?i)trang\\s+[0-9]+/[0-9]+", "")
            .replaceAll("[\\r\\n]+", " ")
            .replaceAll("\\s{2,}", " ")
            .trim();
}
```

File: `src/consumer-summary/src/main/java/com/unihubworkshop/worker/AISummary/AISummaryService.java`

```java
public String generateResponse(String cleanedText) {
    String prompt = "Please provide a concise and professional summary of the following workshop introduction..."
            + cleanedText;
    return this.chatModel.call(prompt);
}
```

Frontend markdown:

File: `src/app-web/src/components/common/MarkdownContent.jsx`

```jsx
function normalizeMarkdown(value) {
  return String(value || '')
    .replace(/\\n/g, '\n')
    .replace(/\r\n/g, '\n')
    .trim();
}

const heading = line.match(/^(#{1,4})\s+(.+)$/);
if (heading) {
  blocks.push({ type: 'heading', level: heading[1].length, text: heading[2] });
}

const bullet = line.match(/^[-*]\s+(.+)$/);
if (bullet) {
  list.push(bullet[1]);
}
```

### Flow demo

1. Organizer tạo workshop và upload PDF.
2. Backend lưu file và đẩy job AI summary.
3. Consumer log `AI Summary completed`.
4. Student detail page hiển thị summary có heading/list/bold đúng format.

---

## 10. Đồng bộ sinh viên từ CSV một chiều

### Requirement liên quan

Hệ thống sinh viên cũ không có API, chỉ export CSV vào ban đêm. UniHub phải định kỳ import CSV, xử lý file lỗi, dữ liệu trùng, và không làm gián đoạn hệ thống đang chạy.

### Cách xử lý trong project

`service-sync` chạy riêng. Nó đọc CSV trong thư mục mount, parse bằng OpenCSV, upsert sinh viên vào PostgreSQL, và create/update user trong Keycloak. Lỗi từng dòng được log và tiếp tục xử lý các dòng khác.

### Code minh hoạ

File: `src/service-sync/src/main/java/com/unihub/sync/service/StudentSyncService.java`

```java
@Scheduled(cron = "${app.sync.cron}")
@Transactional
public void scheduleSync() {
    File folder = new File(csvPath);
    if (!folder.exists()) {
        log.warn("CSV path does not exist: {}", csvPath);
        return;
    }

    File[] files = folder.listFiles((dir, name) -> name.endsWith(".csv"));
    if (files == null || files.length == 0) {
        log.info("No CSV files found for processing.");
        return;
    }

    for (File file : files) {
        processCsvFile(file);
    }
}
```

Upsert sinh viên:

```java
Student student = studentRepository.findById(mssv)
        .orElse(new Student(mssv, email, name, "ACTIVE"));
student.setEmail(email);
student.setName(name);
student.setBirthday(birthday);
dbBatch.add(student);

keycloakIntegrationService.createOrUpdateUser(mssv, email, name, birthday);
```

File: `src/service-sync/src/main/java/com/unihub/sync/service/KeycloakIntegrationService.java`

```java
user.setUsername(username);
user.setEmail(email);
user.setFirstName(name);
user.setEnabled(true);
user.setAttributes(Map.of("studentId", List.of(username)));

CredentialRepresentation credential = new CredentialRepresentation();
credential.setType(CredentialRepresentation.PASSWORD);
credential.setValue(password);
credential.setTemporary(false);

assignRoleToUser(userId, "STUDENT");
```

### Flow demo

1. Đặt CSV vào `src/service-sync-data`.
2. Start `service-sync`.
3. Kiểm tra DB có student mới.
4. Kiểm tra Keycloak có user mới role `STUDENT` và claim `studentId`.
5. Thêm dòng lỗi vào CSV: service log lỗi nhưng không dừng cả job.

---

## 11. RBAC bằng Keycloak JWT + backend method security + frontend guard

### Requirement liên quan

Hệ thống có ba nhóm quyền:

- Sinh viên: xem workshop, đăng ký.
- Ban tổ chức: tạo/sửa/hủy workshop, xem thống kê.
- Nhân sự check-in: chỉ truy cập chức năng scan QR/check-in.

### Cách xử lý trong project

Keycloak cấp JWT có role trong `realm_access.roles` và claim `studentId`. Backend convert role thành `ROLE_*`, dùng `@PreAuthorize` trên endpoint. Frontend web dùng `RoleGuard` để điều hướng student/admin. Mobile đọc role trong JWT để vào màn hình student hoặc staff.

### Code minh hoạ

File: `src/backend/src/main/java/com/unihub/backend/core/security/JwtAuthConverter.java`

```java
public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
    Collection<GrantedAuthority> authorities = Stream.concat(
            jwtGrantedAuthoritiesConverter.convert(jwt).stream(),
            extractResourceRoles(jwt).stream()
    ).collect(Collectors.toSet());

    return new JwtAuthenticationToken(jwt, authorities, principalName(jwt));
}

private String principalName(Jwt jwt) {
    String studentId = jwt.getClaimAsString("studentId");
    if (studentId != null && !studentId.isBlank()) {
        return studentId;
    }
    return jwt.getClaimAsString("preferred_username");
}

private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {
    Map<String, Object> realmAccess = jwt.getClaim("realm_access");
    Collection<String> roles = (Collection<String>) realmAccess.get("roles");
    return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toSet());
}
```

File: `src/backend/src/main/java/com/unihub/backend/core/controller/RegistrationController.java`

```java
@PostMapping
@PreAuthorize("hasRole('STUDENT')")
public RegistrationResponse createRegistration(
        @RequestBody RegistrationRequest request,
        Authentication authentication) {
    return registrationService.createRegistration(request, authentication);
}
```

File: `src/backend/src/main/java/com/unihub/backend/core/controller/WorkshopController.java`

```java
@PostMapping("/admin/workshops")
@PreAuthorize("hasAnyRole('ORGANIZER')")
public WorkshopResponse createWorkshop(@RequestBody WorkshopRequest request) {
    return workshopService.createWorkshop(request);
}
```

File: `src/backend/src/main/java/com/unihub/backend/core/controller/CheckinController.java`

```java
@PostMapping
@PreAuthorize("hasAnyRole('CHECKIN_STAFF', 'ADMIN')")
public void syncCheckins(@RequestBody List<CheckinEvent> events) {
    checkinService.syncCheckins(events);
}
```

Frontend guard:

File: `src/app-web/src/guards/RoleGuard.jsx`

```jsx
export default function RoleGuard({ allowedRoles, children }) {
  const { currentUser, hasRole, isLoading } = useAuth();

  if (isLoading) return null;
  if (!currentUser) return <Navigate to="/login" replace />;
  if (!hasRole(allowedRoles)) return <Navigate to="/403" replace />;

  return children;
}
```

### Flow demo

1. Login student: vào `/student/workshops`, không vào được `/admin/dashboard`.
2. Login organizer: vào admin dashboard và workshop management.
3. Login check-in staff trên mobile: vào màn hình scan QR.
4. Gọi API check-in bằng student token: backend trả 403.
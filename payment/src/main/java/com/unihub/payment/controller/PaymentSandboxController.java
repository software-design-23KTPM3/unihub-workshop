package com.unihub.payment.controller;

import com.unihub.payment.model.*;
import com.unihub.payment.service.PaymentSandboxService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/sandbox")
public class PaymentSandboxController {

    private final PaymentSandboxService paymentSandboxService;

    public PaymentSandboxController(PaymentSandboxService paymentSandboxService) {
        this.paymentSandboxService = paymentSandboxService;
    }

    @PostMapping("/payments")
    public CreatePaymentResponse createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        try {
            return paymentSandboxService.createPayment(request);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ex);
        }
    }

    @GetMapping(value = "/checkout/{paymentId}", produces = MediaType.TEXT_HTML_VALUE)
    public String checkout(@PathVariable String paymentId) {
        SandboxPayment payment = paymentSandboxService.getPayment(paymentId);
        String amount = NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(payment.getAmount());
        boolean completed = payment.getStatus() != PaymentStatus.PENDING;
        String controls = completed
                ? "<p class='notice'>Giao dịch đã được xử lý.</p>"
                : """
                <form method="post" action="/sandbox/checkout/%s/success">
                  <button class="primary" type="submit">Xác nhận thanh toán</button>
                </form>
                <form method="post" action="/sandbox/checkout/%s/fail">
                  <button class="secondary" type="submit">Từ chối thanh toán</button>
                </form>
                <form method="post" action="/sandbox/checkout/%s/server-fail">
                  <button class="danger" type="submit">Đã trả tiền nhưng server thanh toán lỗi</button>
                </form>
                <a class="link" href="%s">Quay lại vé</a>
                """.formatted(paymentId, paymentId, paymentId, escape(payment.getReturnUrl()));

        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1" />
                  <title>UniHub Payment</title>
                  <style>
                    body{margin:0;font-family:Inter,Arial,sans-serif;background:#f5f7fb;color:#172033}
                    main{min-height:100vh;display:grid;place-items:center;padding:24px}
                    section{width:min(520px,100%%);background:#fff;border:1px solid #e5eaf2;border-radius:12px;padding:28px;box-shadow:0 18px 42px rgba(15,47,95,.12)}
                    .eyebrow{font-size:12px;font-weight:800;color:#1769e0;text-transform:uppercase}
                    h1{margin:8px 0 10px;font-size:28px}
                    dl{display:grid;grid-template-columns:140px 1fr;gap:10px;margin:22px 0}
                    dt{color:#667085} dd{margin:0;font-weight:700;word-break:break-word}
                    form{margin:10px 0}
                    button,.link{display:block;width:100%%;box-sizing:border-box;border-radius:8px;padding:12px 14px;text-align:center;font-weight:800;text-decoration:none}
                    button{border:0;cursor:pointer}.primary{background:#1677ff;color:#fff}.secondary{background:#fff1f0;color:#c41d1d;border:1px solid #ffa39e}.danger{background:#c41d1d;color:#fff}
                    .link{margin-top:12px;color:#344054;border:1px solid #d0d5dd}.notice{padding:12px;border-radius:8px;background:#fff7e6;color:#ad6800}
                  </style>
                </head>
                <body>
                  <main>
                    <section>
                      <div class="eyebrow">UniHub Payment</div>
                      <h1>Thanh toán workshop</h1>
                      <dl>
                        <dt>Mã giao dịch</dt><dd>%s</dd>
                        <dt>Số tiền</dt><dd>%s</dd>
                        <dt>Trạng thái</dt><dd>%s</dd>
                      </dl>
                      %s
                    </section>
                  </main>
                </body>
                </html>
                """.formatted(
                escape(payment.getPaymentId()),
                escape(amount),
                escape(payment.getStatus().name()),
                controls);
    }

    @PostMapping("/checkout/{paymentId}/success")
    public ResponseEntity<Void> success(@PathVariable String paymentId) {
        SandboxPayment payment = paymentSandboxService.complete(paymentId, PaymentStatus.SUCCESS);
        return ResponseEntity.status(HttpStatus.FOUND).header("Location", payment.getReturnUrl()).build();
    }

    @PostMapping("/checkout/{paymentId}/fail")
    public ResponseEntity<Void> fail(@PathVariable String paymentId) {
        SandboxPayment payment = paymentSandboxService.complete(paymentId, PaymentStatus.FAILED);
        return ResponseEntity.status(HttpStatus.FOUND).header("Location", payment.getReturnUrl()).build();
    }

    @PostMapping("/checkout/{paymentId}/server-fail")
    public ResponseEntity<Void> serverFail(@PathVariable String paymentId) {
        SandboxPayment payment = paymentSandboxService.failOnServer(paymentId);
        return ResponseEntity.status(HttpStatus.FOUND).header("Location", payment.getReturnUrl()).build();
    }

    @GetMapping("/admin/mode")
    public Map<String, String> getMode() {
        return Map.of("mode", paymentSandboxService.getMode().name());
    }

    @PostMapping("/admin/mode")
    public Map<String, String> setMode(@RequestBody Map<String, String> body) {
        GatewayMode mode = GatewayMode.valueOf(body.getOrDefault("mode", "NORMAL").toUpperCase(Locale.ROOT));
        return Map.of("mode", paymentSandboxService.setMode(mode).name());
    }

    private String escape(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

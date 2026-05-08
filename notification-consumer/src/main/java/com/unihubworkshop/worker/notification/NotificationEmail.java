package com.unihubworkshop.worker.notification;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class NotificationEmail implements NotificationChannel {
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailAddress;

    @Override
    public String getType() {
        return "EMAIL";
    }

    @Override
    public void send(NotificationData data) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            boolean hasQrAttachment = data.getQrImageBase64() != null && !data.getQrImageBase64().isBlank();
            MimeMessageHelper helper = new MimeMessageHelper(message, hasQrAttachment, "UTF-8");
            helper.setFrom(mailAddress);
            helper.setTo(data.getTo());
            helper.setSubject(data.getTitle());
            helper.setText(data.getMsg(), renderHtml(data, hasQrAttachment));

            if (hasQrAttachment) {
                helper.addAttachment("unihub-workshop-qr.png",
                        new ByteArrayResource(Base64.getDecoder().decode(data.getQrImageBase64())));
            }

            mailSender.send(message);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot send notification email", e);
        }
    }

    private String renderHtml(NotificationData data, boolean hasQrAttachment) {
        String title = escape(data.getTitle());
        String message = escape(data.getMsg());
        String workshopTitle = valueOrFallback(data.getWorkshopTitle(), "Workshop");
        String workshopTime = valueOrFallback(data.getWorkshopTime(), "Theo lịch đã công bố");
        String workshopRoom = valueOrFallback(data.getWorkshopRoom(), "Sẽ cập nhật");
        String workshopSpeaker = valueOrFallback(data.getWorkshopSpeaker(), "Sẽ cập nhật");
        String qrNote = hasQrAttachment
                ? "<p style=\"margin:16px 0 0;color:#344054;font-size:14px;line-height:1.6\">QR check-in đã được đính kèm trong email này. Vui lòng xuất trình QR tại cửa phòng.</p>"
                : "";

        return """
                <!doctype html>
                <html>
                <body style="margin:0;background:#f3f6fb;font-family:Arial,Helvetica,sans-serif;color:#172033">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f3f6fb;padding:28px 12px">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:640px;background:#ffffff;border:1px solid #e5eaf2;border-radius:14px;overflow:hidden">
                          <tr>
                            <td style="background:#1257c5;padding:24px 28px;color:#ffffff">
                              <div style="font-size:13px;font-weight:700;letter-spacing:.04em;text-transform:uppercase">UniHub Workshop</div>
                              <h1 style="margin:8px 0 0;font-size:24px;line-height:1.25">%s</h1>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:26px 28px">
                              <p style="margin:0;color:#344054;font-size:15px;line-height:1.7">%s</p>
                              <div style="margin:22px 0;padding:18px;border:1px solid #e6edf6;border-radius:12px;background:#f8fbff">
                                <div style="font-size:12px;font-weight:700;color:#1769e0;text-transform:uppercase;letter-spacing:.04em">Chi tiết workshop</div>
                                <h2 style="margin:8px 0 14px;color:#172033;font-size:20px;line-height:1.3">%s</h2>
                                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="font-size:14px;color:#344054">
                                  <tr><td style="padding:6px 0;width:110px;color:#667085">Thời gian</td><td style="padding:6px 0;font-weight:700">%s</td></tr>
                                  <tr><td style="padding:6px 0;color:#667085">Phòng</td><td style="padding:6px 0;font-weight:700">%s</td></tr>
                                  <tr><td style="padding:6px 0;color:#667085">Diễn giả</td><td style="padding:6px 0;font-weight:700">%s</td></tr>
                                </table>
                              </div>
                              %s
                              <p style="margin:22px 0 0;color:#667085;font-size:13px;line-height:1.6">Nếu có thay đổi về phòng hoặc thời gian, UniHub sẽ gửi thông báo cập nhật.</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                title,
                message,
                escape(workshopTitle),
                escape(workshopTime),
                escape(workshopRoom),
                escape(workshopSpeaker),
                qrNote);
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String escape(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

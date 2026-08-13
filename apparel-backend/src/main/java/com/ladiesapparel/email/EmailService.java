package com.ladiesapparel.email;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    private static final String WINE = "#7A2E38";
    private static final String IVORY = "#FAF7F2";
    private static final String BLUSH = "#F1DDE0";
    private static final String INK = "#2B2420";
    private static final String GOLD = "#C9A24B";

    @Async
    public void sendOtpEmail(String toEmail, String otpCode, String purposeLabel) {
        String body = """
                <p style="margin:0 0 16px;">Hi,</p>
                <p style="margin:0 0 24px;">Use the code below to complete <strong>%s</strong>. This code expires shortly, so don't wait too long.</p>
                <div style="text-align:center; margin: 32px 0;">
                  <span style="display:inline-block; font-family:Georgia,serif; font-size:36px; font-weight:700;
                               letter-spacing:8px; color:%s; background:%s; padding:18px 28px; border-radius:12px 4px 12px 4px;">
                    %s
                  </span>
                </div>
                <p style="margin:0; color:#777;">If you didn't request this, you can safely ignore this email — no changes will be made to your account.</p>
                """.formatted(escape(purposeLabel), WINE, BLUSH, otpCode);

        send(toEmail, "Your verification code for " + purposeLabel, wrap("Verification Code", body));
    }

    @Async
    public void sendOrderConfirmationEmail(String toEmail, String orderNumber, String totalAmount) {
        String body = """
                <p style="margin:0 0 16px;">Hi,</p>
                <p style="margin:0 0 24px;">Thank you for shopping with us! Your order has been placed successfully and we're getting it ready.</p>
                <table style="width:100%%; border-collapse:collapse; margin-bottom:24px;">
                  <tr>
                    <td style="padding:16px; background:%s; border-radius:12px 4px 12px 4px;">
                      <p style="margin:0 0 4px; font-size:12px; letter-spacing:1px; text-transform:uppercase; color:%s;">Order Number</p>
                      <p style="margin:0 0 16px; font-size:18px; font-weight:700; color:%s;">%s</p>
                      <p style="margin:0 0 4px; font-size:12px; letter-spacing:1px; text-transform:uppercase; color:%s;">Total</p>
                      <p style="margin:0; font-size:18px; font-weight:700; color:%s;">Rs. %s</p>
                    </td>
                  </tr>
                </table>
                <p style="margin:0; color:#777;">We'll email you again the moment it ships. You can also track it anytime from the Orders section of your account.</p>
                """.formatted(BLUSH, WINE, INK, escape(orderNumber), WINE, INK, escape(totalAmount));

        send(toEmail, "Order Confirmed - " + orderNumber, wrap("Order Confirmed", body));
    }

    @Async
    public void sendOrderStatusUpdateEmail(String toEmail, String orderNumber, String newStatus) {
        String body = """
                <p style="margin:0 0 16px;">Hi,</p>
                <p style="margin:0 0 24px;">There's an update on your order <strong>%s</strong>:</p>
                <div style="text-align:center; margin: 28px 0;">
                  <span style="display:inline-block; font-size:14px; font-weight:700; letter-spacing:1px; text-transform:uppercase;
                               color:%s; background:%s; padding:10px 22px; border-radius:999px;">
                    %s
                  </span>
                </div>
                <p style="margin:0; color:#777;">You can track full order details anytime from the Orders section of your account.</p>
                """.formatted(escape(orderNumber), WINE, BLUSH, escape(newStatus));

        send(toEmail, "Update on your order " + orderNumber, wrap("Order Update", body));
    }

    private void send(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    /** Shared branded wrapper (header + footer) so every email looks consistent, matching the storefront's palette. */
    private String wrap(String heading, String bodyHtml) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0; padding:0; background:#F5F5F3; font-family:Helvetica,Arial,sans-serif; color:%s;">
                  <table role="presentation" style="width:100%%; border-collapse:collapse;">
                    <tr>
                      <td align="center" style="padding:32px 16px;">
                        <table role="presentation" style="width:100%%; max-width:480px; background:%s; border-radius:16px; overflow:hidden;">
                          <tr>
                            <td style="background:%s; padding:28px 32px;">
                              <p style="margin:0; font-family:Georgia,serif; font-size:22px; color:%s; letter-spacing:0.5px;">Ladies Apparel</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <h1 style="margin:0 0 20px; font-family:Georgia,serif; font-size:22px; color:%s;">%s</h1>
                              <div style="font-size:15px; line-height:1.6;">%s</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:20px 32px; background:#F5F5F3; text-align:center;">
                              <p style="margin:0; font-size:12px; color:#999;">&copy; 2026 Ladies Apparel &middot; This is an automated message, please don't reply directly.</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(INK, IVORY, WINE, IVORY, INK, escape(heading), bodyHtml);
    }

    /** Minimal HTML-escaping for values interpolated into the templates above (defense against header/content injection). */
    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

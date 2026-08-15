package com.ladiesapparel.email;

import jakarta.mail.internet.InternetAddress;
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

  @Value("${app.mail.from:apparel@bhawesh.shop}")
  private String fromEmail;

  @Value("${app.mail.sender-name:Ladies Apparel}")
  private String senderName;

  // Theme Tokens
  private static final String WINE = "#7A2E38";
  private static final String WINE_DARK = "#5E2129";
  private static final String WINE_LIGHT = "#93414C";
  private static final String BLUSH = "#F1DDE0";
  private static final String IVORY = "#FAF7F2";
  private static final String INK = "#2B2420";
  private static final String GOLD = "#C9A24B";

  // Petal Border Radius (Signature UI Motif)
  private static final String RADIUS_PETAL = "28px 8px 28px 8px";
  private static final String RADIUS_PETAL_SM = "14px 4px 14px 4px";

  @Async
  public void sendOtpEmail(String toEmail, String otpCode, String purposeLabel) {
    String body = """
        <p style="margin:0 0 16px; font-size:15px; color:%s;">Hello,</p>
        <p style="margin:0 0 24px; font-size:15px; line-height:1.6; color:%s;">
          Use the verification code below to complete your <strong>%s</strong>. This code is valid for 10 minutes.
        </p>

        <div style="text-align:center; margin: 32px 0;">
          <div style="display:inline-block; font-family:'Fraunces', Georgia, 'Times New Roman', serif; font-size:36px; font-weight:700;
                      letter-spacing:10px; color:%s; background-color:%s; padding:18px 36px;
                      border-radius:%s; border:1px solid rgba(122,46,56,0.2); box-shadow:0 4px 16px -2px rgba(122,46,56,0.15);">
            %s
          </div>
        </div>

        <p style="margin:0; font-size:13px; color:%s; line-height:1.5;">
          If you did not initiate this request, you can safely ignore this email. No changes will be made to your account.
        </p>
        """
        .formatted(INK, INK, escape(purposeLabel), WINE, BLUSH, RADIUS_PETAL_SM, escape(otpCode), INK + "80");

    send(toEmail, "Your Verification Code — " + purposeLabel, wrap("Verification Code", "Authentication", body));
  }

  @Async
  public void sendOrderConfirmationEmail(String toEmail, String orderNumber, String totalAmount) {
    String body = """
        <p style="margin:0 0 16px; font-size:15px; color:%s;">Hello,</p>
        <p style="margin:0 0 24px; font-size:15px; line-height:1.6; color:%s;">
          Thank you for shopping with us! We have received your order and our artisans are currently preparing your package.
        </p>

        <table role="presentation" style="width:100%%; border-collapse:collapse; margin-bottom:28px;">
          <tr>
            <td style="padding:22px 24px; background-color:%s; border-radius:%s; border:1px solid rgba(122,46,56,0.12);">
              <table role="presentation" style="width:100%%; border-collapse:collapse;">
                <tr>
                  <td style="padding-bottom:14px;">
                    <p style="margin:0 0 4px; font-size:11px; letter-spacing:1.5px; text-transform:uppercase; font-weight:600; color:%s;">Order Reference</p>
                    <p style="margin:0; font-family:'Fraunces', Georgia, serif; font-size:20px; font-weight:700; color:%s;">#%s</p>
                  </td>
                </tr>
                <tr>
                  <td style="border-top:1px solid rgba(122,46,56,0.15); padding-top:14px;">
                    <p style="margin:0 0 4px; font-size:11px; letter-spacing:1.5px; text-transform:uppercase; font-weight:600; color:%s;">Grand Total</p>
                    <p style="margin:0; font-family:'Fraunces', Georgia, serif; font-size:24px; font-weight:700; color:%s;">₹%s</p>
                  </td>
                </tr>
              </table>
            </td>
          </tr>
        </table>

        <div style="text-align:center; margin: 28px 0 20px;">
          <a href="https://apparel.bhawesh.shop/orders/%s"
             style="display:inline-block; background-color:%s; color:%s; font-family:'Manrope', Arial, sans-serif; font-size:13px; font-weight:600;
                    letter-spacing:0.5px; text-decoration:none; padding:14px 28px; border-radius:%s; box-shadow:0 4px 16px -2px rgba(122,46,56,0.3);">
            Track Your Order &rarr;
          </a>
        </div>

        <p style="margin:20px 0 0; font-size:13px; color:%s; line-height:1.5; text-align:center;">
          We will send you another update the moment your package is dispatched.
        </p>
        """
        .formatted(INK, INK, BLUSH, RADIUS_PETAL_SM, WINE, INK, escape(orderNumber), WINE, WINE, escape(totalAmount),
            escape(orderNumber), WINE, IVORY, RADIUS_PETAL_SM, INK + "80");

    send(toEmail, "Order Confirmed — #" + orderNumber, wrap("Order Confirmed", "Thank you for your purchase", body));
  }

  @Async
  public void sendOrderStatusUpdateEmail(String toEmail, String orderNumber, String newStatus) {
    String body = """
        <p style="margin:0 0 16px; font-size:15px; color:%s;">Hello,</p>
        <p style="margin:0 0 24px; font-size:15px; line-height:1.6; color:%s;">
          The status of your order <strong>#%s</strong> has been updated:
        </p>

        <div style="text-align:center; margin: 32px 0;">
          <span style="display:inline-block; font-family:'Manrope', Arial, sans-serif; font-size:13px; font-weight:700;
                       letter-spacing:1.5px; text-transform:uppercase; color:%s; background-color:%s;
                       padding:10px 24px; border-radius:%s; border:1px solid rgba(122,46,56,0.2);">
            %s
          </span>
        </div>

        <div style="text-align:center; margin: 24px 0 20px;">
          <a href="https://apparel.bhawesh.shop/orders/%s"
             style="display:inline-block; background-color:%s; color:%s; font-family:'Manrope', Arial, sans-serif; font-size:13px; font-weight:600;
                    letter-spacing:0.5px; text-decoration:none; padding:12px 24px; border-radius:%s;">
            View Order Details
          </a>
        </div>

        <p style="margin:20px 0 0; font-size:13px; color:%s; line-height:1.5; text-align:center;">
          You can view full tracking history anytime in your account dashboard.
        </p>
        """
        .formatted(INK, INK, escape(orderNumber), WINE, BLUSH, RADIUS_PETAL_SM, escape(newStatus), escape(orderNumber),
            WINE, IVORY, RADIUS_PETAL_SM, INK + "80");

    send(toEmail, "Update on Order #" + orderNumber, wrap("Order Status Update", "Fulfillment Notice", body));
  }

  @Async
  public void sendWelcomeEmail(String toEmail, String fullName) {
    String body = """
        <p style="margin:0 0 16px; font-size:15px; color:%s;">Dear %s,</p>
        <p style="margin:0 0 20px; font-size:15px; line-height:1.6; color:%s;">
          Welcome to the Ladies Apparel family. We are thrilled to have you with us!
        </p>
        <p style="margin:0 0 24px; font-size:15px; line-height:1.6; color:%s;">
          Discover our signature collections of handcrafted Indian ethnic weaves and contemporary silhouettes designed to be lived in.
        </p>

        <div style="text-align:center; margin: 32px 0;">
          <a href="https://apparel.bhawesh.shop/products"
             style="display:inline-block; background-color:%s; color:%s; font-family:'Manrope', Arial, sans-serif; font-size:13px; font-weight:600;
                    letter-spacing:0.5px; text-decoration:none; padding:14px 30px; border-radius:%s; box-shadow:0 4px 16px -2px rgba(122,46,56,0.3);">
            Explore the Collection
          </a>
        </div>
        """
        .formatted(INK, escape(fullName), INK, INK, WINE, IVORY, RADIUS_PETAL_SM);

    send(toEmail, "Welcome to Ladies Apparel", wrap("Welcome to the Atelier", "Crafted for Elegance", body));
  }

  private void send(String toEmail, String subject, String htmlBody) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

      helper.setFrom(new InternetAddress(fromEmail, senderName));
      helper.setTo(toEmail);
      helper.setSubject(subject);
      helper.setText(htmlBody, true);

      mailSender.send(message);
      log.info("Email sent successfully to: {} with subject: {}", toEmail, subject);
    } catch (Exception e) {
      log.error("Failed to send email to {}: {}", toEmail, e.getMessage(), e);
    }
  }

  /**
   * Master Responsive Email Template Wrapper with locked color rendering
   */
  private String wrap(String heading, String eyebrow, String bodyHtml) {
    return """
        <!DOCTYPE html>
        <html lang="en" xmlns:v="urn:schemas-microsoft-com:vml" xmlns:o="urn:schemas-microsoft-com:office:office">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <meta name="color-scheme" content="light">
          <meta name="supported-color-schemes" content="light">
          <link rel="preconnect" href="https://fonts.googleapis.com">
          <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
          <link href="https://fonts.googleapis.com/css2?family=Fraunces:opsz,wght@9..144,600;700&family=Manrope:wght@400;500;600;700&display=swap" rel="stylesheet">
          <style>
            :root {
              color-scheme: light;
              supported-color-schemes: light;
            }
            body, table, td, p, a, h1, h2 {
              -webkit-text-size-adjust: 100%% !important;
              -ms-text-size-adjust: 100%% !important;
            }
          </style>
        </head>
        <body style="margin:0; padding:0; background-color:%s; font-family:'Manrope', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color:%s; -webkit-font-smoothing:antialiased; -moz-osx-font-smoothing:grayscale;">
          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color:%s; width:100%%; margin:0 auto; padding:32px 12px;">
            <tr>
              <td align="center">

                <!-- Main Card Container -->
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width:520px; width:100%%; background-color:#FFFFFF; border-radius:%s; overflow:hidden; box-shadow:0 8px 32px -4px rgba(43,36,32,0.08); border:1px solid rgba(43,36,32,0.06);">

                  <!-- Header Banner with Locked #5E2129 -->
                  <tr>
                    <td style="background-color:%s; background-image:linear-gradient(%s, %s); padding:36px 32px 32px; text-align:center;">
                      <p style="margin:0 0 6px; font-family:'Manrope', Arial, sans-serif; font-size:11px; letter-spacing:2px; text-transform:uppercase; color:%s; font-weight:600;">%s</p>
                      <h1 style="margin:0; font-family:'Fraunces', Georgia, serif; font-size:28px; line-height:1.2; font-weight:700; color:%s; letter-spacing:0.5px;">Ladies Apparel</h1>
                    </td>
                  </tr>

                  <!-- Subheader Title Bar -->
                  <tr>
                    <td style="padding:28px 32px 0;">
                      <h2 style="margin:0; font-family:'Fraunces', Georgia, serif; font-size:22px; line-height:1.3; font-weight:700; color:%s;">%s</h2>
                      <div style="width:36px; height:2px; background-color:%s; margin-top:8px; border-radius:2px;"></div>
                    </td>
                  </tr>

                  <!-- Dynamic Body Content -->
                  <tr>
                    <td style="padding:24px 32px 32px; font-size:15px; line-height:1.6; color:%s;">
                      %s
                    </td>
                  </tr>

                  <!-- Trust Footer Elements -->
                  <tr>
                    <td style="padding:24px 32px; background-color:%s; border-top:1px solid rgba(43,36,32,0.05); text-align:center;">
                      <p style="margin:0 0 8px; font-size:12px; font-weight:600; color:%s; letter-spacing:0.5px;">LADIES APPAREL &middot; JAIPUR, INDIA</p>
                      <p style="margin:0; font-size:11px; color:#8C827A; line-height:1.5;">
                        &copy; 2026 Ladies Apparel. All rights reserved.<br>
                        This is an automated operational notification. Please do not reply directly to this email.
                      </p>
                    </td>
                  </tr>

                </table>

              </td>
            </tr>
          </table>
        </body>
        </html>
        """
        .formatted(
            IVORY, INK, IVORY, RADIUS_PETAL,
            WINE_DARK, WINE_DARK, WINE_DARK, BLUSH, escape(eyebrow), IVORY,
            INK, escape(heading), WINE,
            INK, bodyHtml,
            IVORY, INK);
  }

  private String escape(String value) {
    if (value == null)
      return "";
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
  }
}
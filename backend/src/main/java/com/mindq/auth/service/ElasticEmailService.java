package com.mindq.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Email service using Elastic Email's transactional email HTTPS API.
 *
 * Environment variables:
 *   BREVO_API_KEY       - Brevo API key
 *   ELASTIC_EMAIL_SENDER  - Sender email address
 *   ELASTIC_EMAIL_SENDER_NAME - Sender display name (default: MindQ)
 *
 * NEVER log API keys or OTP values.
 */
@Slf4j
@Service
@Profile("!test")
public class ElasticEmailService implements EmailService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final String API_URL = "https://api.elasticemail.com/v4/emails/transactional";

    private final String apiKey;
    private final String senderEmail;
    private final String senderName;

    public ElasticEmailService(
            @Value("${app.elastic-email.api-key:}") String apiKey,
            @Value("${app.elastic-email.sender:}") String senderEmail,
            @Value("${app.elastic-email.sender-name:MindQ}") String senderName) {
        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
    }

    @Async
    @Override
    public void sendOtp(String to, String otpCode, String purpose) {
        String subject = "MindQ — Your verification code";
        String htmlBody = buildOtpEmailHtml(otpCode, purpose);
        sendHtmlEmail(to, subject, htmlBody);
        log.info("OTP email sent via Elastic Email to {} (purpose={})", to, purpose);
    }

    @Async
    @Override
    public void sendPasswordResetLink(String to, String link) {
        String subject = "MindQ — Reset your password";
        String htmlBody = buildPasswordResetHtml(link);
        sendHtmlEmail(to, subject, htmlBody);
        log.info("Password reset email sent via Elastic Email to {}", to);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            Map<String, Object> body = Map.of(
                "sender", Map.of("name", senderName, "email", senderEmail),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "htmlContent", htmlBody
            );

            String jsonBody = MAPPER.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("accept", "application/json")
                    .header("x-elasticemail-apikey", apiKey)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode json = MAPPER.readTree(response.body());
                String messageId = json.path("MessageID").asText("unknown");
                log.info("Elastic Email sent successfully - messageId={}", messageId);
            } else {
                log.error("Elastic Email API error: HTTP {} - {}", response.statusCode(), response.body());
                throw new RuntimeException("Failed to send email. Please try again later.");
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to send email via Elastic Email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email. Please try again later.", e);
        }
    }

    private String buildOtpEmailHtml(String otpCode, String purpose) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#F8FAFC;font-family:Inter,-apple-system,sans-serif;">
              <table width="100%" cellpadding="0" cellspacing="0" style="padding:40px 20px;">
                <tr><td align="center">
                  <table width="480" cellpadding="0" cellspacing="0" style="background:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.08);">
                    <!-- Header -->
                    <tr><td style="background:linear-gradient(135deg,#2563EB,#7C3AED);padding:32px 40px;">
                      <div style="font-size:24px;font-weight:700;color:#FFFFFF;">Mind<span style="opacity:0.8;">Q</span></div>
                      <div style="font-size:13px;color:rgba(255,255,255,0.7);margin-top:4px;">Sync Your Mind with AI</div>
                    </td></tr>
                    <!-- Body -->
                    <tr><td style="padding:40px;">
                      <h2 style="margin:0 0 8px;font-size:20px;font-weight:700;color:#0F172A;">Verify your email</h2>
                      <p style="margin:0 0 24px;font-size:14px;color:#64748B;line-height:1.6;">
                        Use the following code to complete your __PURPOSE__:
                      </p>
                      <!-- OTP Box -->
                      <table width="100%" cellpadding="0" cellspacing="0"><tr><td align="center" style="padding:0 0 24px;">
                        <div style="background:#F1F5F9;border-radius:12px;padding:20px 32px;display:inline-block;">
                          <span style="font-size:32px;font-weight:700;letter-spacing:8px;color:#0F172A;font-family:monospace;">__OTP__</span>
                        </div>
                      </td></tr></table>
                      <p style="margin:0 0 8px;font-size:13px;color:#64748B;line-height:1.6;">
                        This code expires in <strong>10 minutes</strong>.
                      </p>
                      <p style="margin:0;font-size:13px;color:#64748B;line-height:1.6;">
                        If you didn't request this, you can safely ignore this email.
                      </p>
                    </td></tr>
                    <!-- Footer -->
                    <tr><td style="padding:20px 40px;border-top:1px solid #E2E8F0;">
                      <p style="margin:0;font-size:12px;color:#94A3B8;text-align:center;">
                        MindQ — AI-Powered Learning Platform
                      </p>
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.replace("__OTP__", otpCode).replace("__PURPOSE__", purpose);
    }

    private String buildPasswordResetHtml(String link) {
        String html = """
<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"></head>
<body style="margin:0;padding:0;background:#F8FAFC;font-family:Inter,-apple-system,sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0" style="padding:40px 20px;">
    <tr><td align="center">
      <table width="480" cellpadding="0" cellspacing="0" style="background:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.08);">
        <tr><td style="background:linear-gradient(135deg,#2563EB,#7C3AED);padding:32px 40px;">
          <div style="font-size:24px;font-weight:700;color:#FFFFFF;">Mind<span style="opacity:0.8;">Q</span></div>
          <div style="font-size:13px;color:rgba(255,255,255,0.7);margin-top:4px;">Sync Your Mind with AI</div>
        </td></tr>
        <tr><td style="padding:40px;">
          <h2 style="margin:0 0 8px;font-size:20px;font-weight:700;color:#0F172A;">Reset your password</h2>
          <p style="margin:0 0 24px;font-size:14px;color:#64748B;line-height:1.6;">
            Click the button below to set a new password. This link expires in 30 minutes.
          </p>
          <table width="100%" cellpadding="0" cellspacing="0"><tr><td align="center" style="padding:0 0 24px;">
            <a href="__LINK__" style="display:inline-block;background:#2563EB;color:#FFFFFF;font-size:14px;font-weight:600;text-decoration:none;padding:12px 32px;border-radius:10px;">
              Reset Password
            </a>
          </td></tr></table>
          <p style="margin:0;font-size:13px;color:#64748B;line-height:1.6;">
            If you did not request this, you can safely ignore this email.
          </p>
        </td></tr>
        <tr><td style="padding:20px 40px;border-top:1px solid #E2E8F0;">
          <p style="margin:0;font-size:12px;color:#94A3B8;text-align:center;">
            MindQ — AI-Powered Learning Platform
          </p>
        </td></tr>
      </table>
    </td></tr>
  </table>
</body>
</html>""";
        return html.replace("__LINK__", link);
    }
}

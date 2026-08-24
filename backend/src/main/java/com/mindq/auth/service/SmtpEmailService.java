package com.mindq.auth.service;

import com.mindq.model.EmailLog;
import com.mindq.repository.EmailLogRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Email service using Spring JavaMailSender (SMTP).
 * Falls back to console logging when MAIL_USERNAME is empty.
 * Includes retry with exponential backoff and delivery status tracking.
 */
@Slf4j
@Service
@Profile("!test")
public class SmtpEmailService implements EmailService {

    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 1000;

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String fromName;
    private final EmailLogRepository emailLogRepository;
    private final boolean smtpEnabled;

    public SmtpEmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from:}") String fromEmail,
            @Value("${app.mail.from-name:MindQ}") String fromName,
            EmailLogRepository emailLogRepository,
            @Value("${spring.mail.username:}") String mailUsername) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.emailLogRepository = emailLogRepository;
        this.smtpEnabled = mailUsername != null && !mailUsername.isBlank();
        if (!this.smtpEnabled) {
            log.warn("MAIL_USERNAME is empty -- emails will be logged to console");
        }
    }

    @Async
    @Override
    public void sendOtp(String to, String otpCode, String purpose) {
        String subject = "MindQ -- Your verification code";
        String htmlBody = buildOtpEmailHtml(otpCode, purpose);
        if (smtpEnabled) {
            sendHtmlEmailWithRetry(to, subject, htmlBody, EmailLog.EmailType.OTP);
        } else {
            logToConsole(to, subject, "OTP: " + otpCode);
            logEmailSend(to, subject, EmailLog.EmailType.OTP, EmailLog.Status.SENT, null, 1, false);
        }
    }

    @Async
    @Override
    public void sendPasswordResetLink(String to, String link) {
        String subject = "MindQ -- Reset your password";
        String htmlBody = buildPasswordResetHtml(link);
        if (smtpEnabled) {
            sendHtmlEmailWithRetry(to, subject, htmlBody, EmailLog.EmailType.PASSWORD_RESET);
        } else {
            logToConsole(to, subject, "Reset Link: " + link);
            logEmailSend(to, subject, EmailLog.EmailType.PASSWORD_RESET, EmailLog.Status.SENT, null, 1, false);
        }
    }

    private void logToConsole(String to, String subject, String body) {
        log.warn("========================================");
        log.warn("  [DEV EMAIL] To: {} | Subject: {}", to, subject);
        log.warn("  {}", body);
        log.warn("========================================");
    }

    private void sendHtmlEmailWithRetry(String to, String subject, String htmlBody, EmailLog.EmailType emailType) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                sendHtmlEmail(to, subject, htmlBody);
                log.info("Email sent via SMTP to {} (attempt {}/{})", to, attempt, MAX_RETRIES);
                logEmailSend(to, subject, emailType, EmailLog.Status.SENT, null, attempt, attempt > 1);
                return;
            } catch (Exception e) {
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    long delay = BASE_DELAY_MS * (1L << (attempt - 1));
                    log.warn("SMTP send failed (attempt {}/{}), retrying in {}ms: {}",
                            attempt, MAX_RETRIES, delay, e.getMessage());
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        String errorMsg = lastException != null ? lastException.getMessage() : "unknown error";
        log.error("Failed to send email to {} after {} attempts: {}", to, MAX_RETRIES, errorMsg);
        logEmailSend(to, subject, emailType, EmailLog.Status.FAILED, errorMsg, MAX_RETRIES, MAX_RETRIES > 1);
        throw new RuntimeException("Failed to send email. Please try again later.", lastException);
    }

    @Transactional
    public void logEmailSend(String recipient, String subject, EmailLog.EmailType emailType,
                             EmailLog.Status status, String errorMessage, int attempts, boolean retryUsed) {
        try {
            EmailLog emailLog = EmailLog.builder()
                    .recipient(recipient)
                    .subject(subject)
                    .emailType(emailType)
                    .status(status)
                    .errorMessage(errorMessage)
                    .attempts(attempts)
                    .retryUsed(retryUsed)
                    .build();
            emailLogRepository.save(emailLog);
        } catch (Exception e) {
            log.error("Failed to log email send for {}: {}", recipient, e.getMessage());
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(message);
    }

    private String buildOtpEmailHtml(String otpCode, String purpose) {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head>"
            + "<body style=\"margin:0;padding:0;background:#F8FAFC;font-family:Inter,-apple-system,sans-serif;\">"
            + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"padding:40px 20px;\">"
            + "<tr><td align=\"center\">"
            + "<table width=\"480\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.08);\">"
            + "<tr><td style=\"background:linear-gradient(135deg,#2563EB,#7C3AED);padding:32px 40px;\">"
            + "<div style=\"font-size:24px;font-weight:700;color:#FFFFFF;\">Mind<span style=\"opacity:0.8;\">Q</span></div>"
            + "<div style=\"font-size:13px;color:rgba(255,255,255,0.7);margin-top:4px;\">Sync Your Mind with AI</div>"
            + "</td></tr>"
            + "<tr><td style=\"padding:40px;\">"
            + "<h2 style=\"margin:0 0 8px;font-size:20px;font-weight:700;color:#0F172A;\">Verify your email</h2>"
            + "<p style=\"margin:0 0 24px;font-size:14px;color:#64748B;line-height:1.6;\">Use the following code to complete your " + purpose + ":</p>"
            + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><td align=\"center\" style=\"padding:0 0 24px;\">"
            + "<div style=\"background:#F1F5F9;border-radius:12px;padding:20px 32px;display:inline-block;\">"
            + "<span style=\"font-size:32px;font-weight:700;letter-spacing:8px;color:#0F172A;font-family:monospace;\">" + otpCode + "</span>"
            + "</div></td></tr></table>"
            + "<p style=\"margin:0 0 8px;font-size:13px;color:#64748B;line-height:1.6;\">This code expires in <strong>10 minutes</strong>.</p>"
            + "<p style=\"margin:0;font-size:13px;color:#64748B;line-height:1.6;\">If you did not request this, you can safely ignore this email.</p>"
            + "</td></tr>"
            + "<tr><td style=\"padding:20px 40px;border-top:1px solid #E2E8F0;\">"
            + "<p style=\"margin:0;font-size:12px;color:#94A3B8;text-align:center;\">MindQ -- AI-Powered Learning Platform</p>"
            + "</td></tr></table></td></tr></table></body></html>";
    }

    private String buildPasswordResetHtml(String link) {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head>"
            + "<body style=\"margin:0;padding:0;background:#F8FAFC;font-family:Inter,-apple-system,sans-serif;\">"
            + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"padding:40px 20px;\">"
            + "<tr><td align=\"center\">"
            + "<table width=\"480\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.08);\">"
            + "<tr><td style=\"background:linear-gradient(135deg,#2563EB,#7C3AED);padding:32px 40px;\">"
            + "<div style=\"font-size:24px;font-weight:700;color:#FFFFFF;\">Mind<span style=\"opacity:0.8;\">Q</span></div>"
            + "<div style=\"font-size:13px;color:rgba(255,255,255,0.7);margin-top:4px;\">Sync Your Mind with AI</div>"
            + "</td></tr>"
            + "<tr><td style=\"padding:40px;\">"
            + "<h2 style=\"margin:0 0 8px;font-size:20px;font-weight:700;color:#0F172A;\">Reset your password</h2>"
            + "<p style=\"margin:0 0 24px;font-size:14px;color:#64748B;line-height:1.6;\">Click the button below to set a new password. This link expires in 30 minutes.</p>"
            + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><td align=\"center\" style=\"padding:0 0 24px;\">"
            + "<a href=\"" + link + "\" style=\"display:inline-block;background:#2563EB;color:#FFFFFF;font-size:14px;font-weight:600;text-decoration:none;padding:12px 32px;border-radius:10px;\">Reset Password</a>"
            + "</td></tr></table>"
            + "<p style=\"margin:0;font-size:13px;color:#64748B;line-height:1.6;\">If you did not request this, you can safely ignore this email.</p>"
            + "</td></tr>"
            + "<tr><td style=\"padding:20px 40px;border-top:1px solid #E2E8F0;\">"
            + "<p style=\"margin:0;font-size:12px;color:#94A3B8;text-align:center;\">MindQ -- AI-Powered Learning Platform</p>"
            + "</td></tr></table></td></tr></table></body></html>";
    }
}

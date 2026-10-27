package dev.codecompass;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MailDeliveryService {
    private static final Logger logger = LoggerFactory.getLogger(MailDeliveryService.class);
    private final JavaMailSender sender;
    private final String host;
    private final String username;
    private final String password;
    private final boolean authRequired;
    private final String from;
    private final String baseUrl;
    private final boolean devFallback;

    public MailDeliveryService(ObjectProvider<JavaMailSender> senderProvider,
                               @Value("${spring.mail.host:${MAIL_HOST:}}") String host,
                               @Value("${spring.mail.username:${MAIL_USERNAME:}}") String username,
                               @Value("${spring.mail.password:${MAIL_PASSWORD:}}") String password,
                               @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean authRequired,
                               @Value("${codecompass.mail.from:${spring.mail.username:no-reply@localhost}}") String from,
                               @Value("${codecompass.app.base-url:http://localhost:8080}") String baseUrl,
                               @Value("${codecompass.mail.dev-fallback:true}") boolean devFallback) {
        this.sender = senderProvider.getIfAvailable();
        this.host = host == null ? "" : host.trim();
        this.username = username == null ? "" : username.trim();
        this.password = password == null ? "" : password.trim();
        this.authRequired = authRequired;
        this.from = StringUtils.hasText(from) && !"no-reply@localhost".equalsIgnoreCase(from)
                ? from.trim()
                : (StringUtils.hasText(this.username) ? this.username : "no-reply@localhost");
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.devFallback = devFallback;
    }

    public boolean isConfigured() {
        return devFallback || isSmtpConfigured();
    }

    public boolean isSmtpConfigured() {
        return StringUtils.hasText(host) && sender != null;
    }

    public void requireConfigured() {
        if (!isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Email delivery is not configured. Set spring.mail.host / MAIL_HOST and SMTP credentials before using account email workflows.");
        }
    }

    public void sendVerification(String email, String rawToken) {
        send(email, "Verify your CodeCompass account",
                "Verify your email address by opening this one-time link:\n\n" + link("verify", rawToken)
                        + "\n\nThis link expires in 24 hours. If you did not create this account, ignore this message.");
    }

    public void sendPasswordReset(String email, String rawToken) {
        send(email, "Reset your CodeCompass password",
                "Reset your password using this one-time link:\n\n" + link("reset", rawToken)
                        + "\n\nThis link expires in 30 minutes. If you did not request a reset, ignore this message.");
    }

    private String link(String action, String token) {
        return baseUrl + "/?" + action + "Token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private void send(String to, String subject, String body) {
        requireConfigured();

        if (isSmtpConfigured()) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            try {
                sender.send(message);
                logger.info("Email delivered successfully via SMTP to {}", to);
                return;
            } catch (org.springframework.mail.MailException exception) {
                String category = failureCategory(exception);
                logger.warn("SMTP email delivery failed (category={}): {}", category, exception.getMessage());
                if (!devFallback) {
                    throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                            "Email delivery failed (" + category + "). Check the SMTP configuration and try again.");
                }
                logger.info("Falling back to console delivery because dev-fallback is active.");
            }
        }

        if (devFallback) {
            logger.info("""

                    ================================================================================
                    [CODECOMPASS EMAIL DELIVERY — LOCAL / DEV FALLBACK]
                    To: {}
                    From: {}
                    Subject: {}
                    Body:
                    {}
                    ================================================================================
                    """, to, from, subject, body);
            return;
        }

        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                "Email delivery is not configured. Set MAIL_HOST and SMTP credentials.");
    }

    private static String failureCategory(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            String type = cause.getClass().getSimpleName().toLowerCase(Locale.ROOT);
            String message = cause.getMessage() == null ? "" : cause.getMessage().toLowerCase(Locale.ROOT);
            if (type.contains("authentication") || message.contains("535")
                    || message.contains("username and password") || message.contains("authentication failed")) {
                return "authentication";
            }
            if (type.contains("unknownhost") || message.contains("unknown host") || message.contains("resolve")) {
                return "dns";
            }
            if (type.contains("ssl") || type.contains("certificate") || message.contains("starttls")
                    || message.contains("tls negotiation")) {
                return "tls";
            }
            if (type.contains("timeout") || message.contains("timed out") || message.contains("timeout")) {
                return "timeout";
            }
            if (type.contains("connect") || message.contains("could not connect") || message.contains("connection refused")) {
                return "connection";
            }
            if (type.contains("smtpsendfailed") || type.contains("smtprecipientfailed")
                    || message.contains("sender address rejected") || message.contains("recipient address rejected")) {
                return "smtp_rejected";
            }
        }
        return "mail_transport";
    }
}

package dev.codecompass;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {
    private final AppUserRepository users;
    private final AuthTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final MailDeliveryService mail;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(AppUserRepository users, AuthTokenRepository tokens,
                       PasswordEncoder passwordEncoder, MailDeliveryService mail) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.mail = mail;
    }

    @Transactional
    public void register(AuthDtos.RegisterRequest request) {
        mail.requireConfigured();
        String email = normalizeEmail(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        validateNewPassword(request.password());
        AppUser user = users.saveAndFlush(new AppUser(request.name().trim(), email,
                passwordEncoder.encode(request.password())));
        issueToken(user, AuthToken.Purpose.VERIFY_EMAIL, 24 * 60);
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        AuthToken token = validToken(rawToken, AuthToken.Purpose.VERIFY_EMAIL);
        token.getUser().setEnabled(true);
        token.consume();
    }

    @Transactional
    public void requestPasswordReset(String emailInput) {
        mail.requireConfigured();
        users.findByEmailIgnoreCase(normalizeEmail(emailInput)).ifPresent(user ->
                issueToken(user, AuthToken.Purpose.RESET_PASSWORD, 30));
    }

    @Transactional
    public void resendVerification(String emailInput) {
        mail.requireConfigured();
        users.findByEmailIgnoreCase(normalizeEmail(emailInput))
                .filter(user -> !user.isEnabled())
                .ifPresent(user -> issueToken(user, AuthToken.Purpose.VERIFY_EMAIL, 24 * 60));
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        validateNewPassword(newPassword);
        AuthToken token = validToken(rawToken, AuthToken.Purpose.RESET_PASSWORD);
        token.getUser().setPasswordHash(passwordEncoder.encode(newPassword));
        token.consume();
    }

    @Transactional(readOnly = true)
    public AppUser currentUser(String id) {
        return users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Please sign in again."));
    }

    @Transactional
    public AppUser updateName(String userId, String name) {
        AppUser user = currentUser(userId);
        user.setName(name.trim());
        return user;
    }

    @Transactional
    public void changePassword(String userId, String currentPassword, String newPassword) {
        AppUser user = currentUser(userId);
        if (currentPassword.getBytes(StandardCharsets.UTF_8).length > 72
                || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The current password is incorrect.");
        }
        validateNewPassword(newPassword);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    private void issueToken(AppUser user, AuthToken.Purpose purpose, int minutes) {
        tokens.deleteAllByUser_IdAndPurpose(user.getId(), purpose.name());
        byte[] random = new byte[32];
        secureRandom.nextBytes(random);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        String hash = sha256(raw);
        tokens.saveAndFlush(new AuthToken(user, purpose, hash,
                LocalDateTime.now(ZoneOffset.UTC).plusMinutes(minutes)));
        if (purpose == AuthToken.Purpose.VERIFY_EMAIL) mail.sendVerification(user.getEmail(), raw);
        else mail.sendPasswordReset(user.getEmail(), raw);
    }

    private AuthToken validToken(String rawToken, AuthToken.Purpose purpose) {
        if (!StringUtils.hasText(rawToken) || rawToken.length() > 256) throw invalidToken();
        return tokens.findByTokenHash(sha256(rawToken))
                .filter(t -> t.getPurpose() == purpose)
                .filter(t -> t.getConsumedAt() == null)
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC)))
                .orElseThrow(this::invalidToken);
    }

    private ApiException invalidToken() {
        return new ApiException(HttpStatus.BAD_REQUEST, "This link is invalid or expired. Request a new one.");
    }

    static String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }

    private static void validateNewPassword(String password) {
        if (password == null || password.length() < 6 || password.length() > 18
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must be between 6 and 18 characters.");
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}

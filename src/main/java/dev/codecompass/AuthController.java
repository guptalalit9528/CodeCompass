package dev.codecompass;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;
    private final SessionAuthenticationStrategy sessionStrategy;
    private final LoginRateLimiter rateLimiter;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager,
                          SecurityContextRepository contextRepository, SessionAuthenticationStrategy sessionStrategy,
                          LoginRateLimiter rateLimiter) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
        this.sessionStrategy = sessionStrategy;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/csrf")
    public AuthDtos.CsrfResponse csrf(CsrfToken token) { return new AuthDtos.CsrfResponse(token.getToken()); }

    @PostMapping("/register")
    public ResponseEntity<AuthDtos.RegisterResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        String otp = authService.register(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new AuthDtos.RegisterResponse("A 6-digit verification code has been sent to your email.",
                        request.email().trim().toLowerCase(java.util.Locale.ROOT), request.name().trim(), otp));
    }

    @PostMapping("/verify-otp")
    public AuthDtos.MessageResponse verifyOtp(@Valid @RequestBody AuthDtos.VerifyOtpRequest request) {
        authService.verifyOtp(request.email(), request.otp());
        return new AuthDtos.MessageResponse("Your email has been verified! You can now sign in.");
    }

    @PostMapping("/resend-otp")
    public AuthDtos.ResendOtpResponse resendOtp(@Valid @RequestBody AuthDtos.ResendOtpRequest request) {
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        String otp = authService.resendOtp(email);
        return new AuthDtos.ResendOtpResponse("A new verification code has been sent to your email.", email, otp);
    }

    @PostMapping("/login")
    public AuthDtos.UserResponse login(@Valid @RequestBody AuthDtos.LoginRequest request,
                                       HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        String key = servletRequest.getRemoteAddr();
        rateLimiter.check(key);
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            rateLimiter.failed(key);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password. Verify your email if you just registered.");
        }
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    AuthService.normalizeEmail(request.email()), request.password()));
        } catch (AuthenticationException exception) {
            rateLimiter.failed(key);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password. Verify your email if you just registered.");
        }
        rateLimiter.succeeded(key);
        servletRequest.getSession(true);
        sessionStrategy.onAuthentication(authentication, servletRequest, servletResponse);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, servletRequest, servletResponse);
        return AuthDtos.UserResponse.from(((UserPrincipal) authentication.getPrincipal()).getUser());
    }

    @PostMapping("/verify-email")
    public AuthDtos.MessageResponse verifyEmail(@Valid @RequestBody AuthDtos.TokenRequest request) {
        authService.verifyEmail(request.token());
        return new AuthDtos.MessageResponse("Your email is verified. You can now sign in.");
    }

    @PostMapping("/forgot-password")
    public AuthDtos.MessageResponse forgotPassword(@Valid @RequestBody AuthDtos.ForgotPasswordRequest request) {
        authService.requestPasswordReset(request.email());
        return new AuthDtos.MessageResponse("If an account exists for that address, a reset link has been sent.");
    }

    @PostMapping("/resend-verification")
    public AuthDtos.MessageResponse resendVerification(@Valid @RequestBody AuthDtos.ResendVerificationRequest request) {
        authService.resendVerification(request.email());
        return new AuthDtos.MessageResponse("If that address needs verification, a new link has been sent.");
    }

    @PostMapping("/reset-password")
    public AuthDtos.MessageResponse resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return new AuthDtos.MessageResponse("Password updated. You can now sign in with your new password.");
    }

    @GetMapping("/me")
    public AuthDtos.UserResponse me(@org.springframework.security.core.annotation.AuthenticationPrincipal UserPrincipal principal) {
        return AuthDtos.UserResponse.from(authService.currentUser(principal.getId()));
    }

    @PatchMapping("/profile")
    public AuthDtos.UserResponse updateProfile(@org.springframework.security.core.annotation.AuthenticationPrincipal UserPrincipal principal,
                                               @Valid @RequestBody AuthDtos.UpdateProfileRequest request) {
        return AuthDtos.UserResponse.from(authService.updateName(principal.getId(), request.name()));
    }

    @PostMapping("/change-password")
    public AuthDtos.MessageResponse changePassword(@org.springframework.security.core.annotation.AuthenticationPrincipal UserPrincipal principal,
                                                   @Valid @RequestBody AuthDtos.ChangePasswordRequest request) {
        authService.changePassword(principal.getId(), request.currentPassword(), request.newPassword());
        return new AuthDtos.MessageResponse("Password updated.");
    }
}

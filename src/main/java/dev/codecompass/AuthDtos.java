package dev.codecompass;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 6, max = 18, message = "Password must be between 6 and 18 characters.") String password) {
        @Override public String toString() { return "RegisterRequest[name=" + name + ", email=" + email + ", password=[REDACTED]]"; }
    }

    public record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
                               @NotBlank @Size(max = 72) String password) {
        @Override public String toString() { return "LoginRequest[email=" + email + ", password=[REDACTED]]"; }
    }

    public record TokenRequest(@NotBlank @Size(max = 256) String token) {
        @Override public String toString() { return "TokenRequest[token=[REDACTED]]"; }
    }
    public record VerifyOtpRequest(@NotBlank @Email @Size(max = 254) String email,
                                   @NotBlank @Size(min = 6, max = 6) String otp) {
        @Override public String toString() { return "VerifyOtpRequest[email=" + email + ", otp=[REDACTED]]"; }
    }
    public record ResendOtpRequest(@NotBlank @Email @Size(max = 254) String email) {}
    public record RegisterResponse(String message, String email, String name, String otp) {
        @Override public String toString() { return "RegisterResponse[email=" + email + ", name=" + name + ", otp=[REDACTED]]"; }
    }
    public record ResendOtpResponse(String message, String email, String otp) {
        @Override public String toString() { return "ResendOtpResponse[email=" + email + ", otp=[REDACTED]]"; }
    }
    public record ForgotPasswordRequest(@NotBlank @Email @Size(max = 254) String email) {}
    public record ResendVerificationRequest(@NotBlank @Email @Size(max = 254) String email) {}
    public record ResetPasswordRequest(@NotBlank @Size(max = 256) String token,
                                       @NotBlank @Size(min = 6, max = 18, message = "Password must be between 6 and 18 characters.") String newPassword) {
        @Override public String toString() { return "ResetPasswordRequest[token=[REDACTED], newPassword=[REDACTED]]"; }
    }
    public record UpdateProfileRequest(@NotBlank @Size(max = 100) String name) {}
    public record ChangePasswordRequest(@NotBlank @Size(max = 72) String currentPassword,
                                        @NotBlank @Size(min = 6, max = 18, message = "Password must be between 6 and 18 characters.") String newPassword) {
        @Override public String toString() { return "ChangePasswordRequest[currentPassword=[REDACTED], newPassword=[REDACTED]]"; }
    }
    public record UserResponse(String id, String name, String email, LocalDateTime createdAt) {
        static UserResponse from(AppUser user) {
            return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
        }
    }
    public record MessageResponse(String message) {}
    public record CsrfResponse(String token) {}
}

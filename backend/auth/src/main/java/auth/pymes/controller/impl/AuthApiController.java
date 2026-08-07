package auth.pymes.controller.impl;

import auth.pymes.common.models.dto.request.ForgotPasswordRequest;
import auth.pymes.common.models.dto.request.LoginRequest;
import auth.pymes.common.models.dto.request.RegisterRequest;
import auth.pymes.common.models.dto.request.ResendVerificationRequest;
import auth.pymes.common.models.dto.request.ResetPasswordRequest;
import auth.pymes.common.models.dto.request.TokenRefreshRequest;
import auth.pymes.common.models.dto.request.VerifyEmailRequest;
import auth.pymes.common.models.dto.response.ApiResponse;
import auth.pymes.common.models.dto.response.AuthResponse;
import auth.pymes.common.models.dto.response.LogoutResponse;
import auth.pymes.controller.AuthApi;
import auth.pymes.service.AuthService;
import auth.pymes.service.EmailVerificationService;
import auth.pymes.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthApiController implements AuthApi {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;

    @Override
    public ResponseEntity<ApiResponse<AuthResponse>> register(RegisterRequest request,
                                                               HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.ok(authService.register(request, httpRequest)));
    }

    @Override
    public ResponseEntity<ApiResponse<AuthResponse>> login(LoginRequest request,
                                                            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request, httpRequest)));
    }

    @Override
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.logout(request)));
    }

    @Override
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(TokenRefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refreshToken(request)));
    }

    @Override
    public ResponseEntity<ApiResponse<AuthResponse>> verifyEmail(VerifyEmailRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.ok(emailVerificationService.verifyEmail(request, httpRequest)));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> resendVerification(ResendVerificationRequest request) {
        emailVerificationService.resendVerificationToken(request.email());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> forgotPassword(ForgotPasswordRequest request) {
        passwordResetService.generateResetToken(request.email());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> resetPassword(ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Override
    public ResponseEntity<ApiResponse<AuthResponse>> exchange(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(authService.exchange(body.get("code"))));
    }
}
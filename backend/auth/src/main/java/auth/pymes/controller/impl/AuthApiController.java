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
import auth.pymes.utils.exception.CodigoError;
import auth.pymes.utils.exception.custom.InvalidInputException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public ResponseEntity<ApiResponse<AuthResponse>> register(RegisterRequest request,
                                                              HttpServletRequest httpRequest) {
        AuthResponse response = authService.register(request, httpRequest);
        // Si hay tokens, es registro completado → 201, si no → 200 (pending verification)
        if (response.accessToken() != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
        }
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    public ResponseEntity<ApiResponse<AuthResponse>> login(LoginRequest request,
                                                           HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String accessToken = null;

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        LogoutResponse response = authService.logout(accessToken);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(TokenRefreshRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    public ResponseEntity<ApiResponse<AuthResponse>> verifyEmail(VerifyEmailRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = emailVerificationService.verifyEmail(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(response));
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
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            throw new InvalidInputException(CodigoError.INVALID_INPUT, "Exchange code is required");
        }

        @SuppressWarnings("unchecked")
        Map<String, String> tokenData = (Map<String, String>) redisTemplate.opsForValue().get("oauth:code:" + code);
        if (tokenData == null) {
            throw new InvalidInputException(CodigoError.INVALID_INPUT, "Invalid or expired exchange code");
        }

        redisTemplate.delete("oauth:code:" + code);

        AuthResponse response = new AuthResponse(
                tokenData.get("accessToken"),
                tokenData.get("refreshToken"),
                null,
                null
        );
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
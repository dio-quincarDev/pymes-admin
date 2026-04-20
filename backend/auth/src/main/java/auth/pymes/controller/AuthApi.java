package auth.pymes.controller;

import auth.pymes.common.constants.ApiPathConstants;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Authentication", description = "Endpoints de autenticación y gestión de tokens")
@RequestMapping(ApiPathConstants.V1_ROUTE + ApiPathConstants.AUTH_ROUTE)
public interface AuthApi {

    @Operation(summary = "Registro de usuario local", description = "Crea un usuario, su empresa (plan FREE) y lo asigna como OWNER")
    @PostMapping(ApiPathConstants.AUTH_REGISTER)
    ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest);

    @Operation(summary = "Login de usuario local", description = "Autentica un usuario con email y contraseña")
    @PostMapping(ApiPathConstants.AUTH_LOGIN)
    ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest);

    @Operation(summary = "Logout", description = "Invalida los tokens y cierra la sesión del usuario")
    @PostMapping(ApiPathConstants.AUTH_LOGOUT)
    ResponseEntity<ApiResponse<LogoutResponse>> logout(HttpServletRequest request);

    @Operation(summary = "Refresh token", description = "Obtiene un nuevo access token usando el refresh token")
    @PostMapping(ApiPathConstants.AUTH_REFRESH)
    ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request);

    @Operation(summary = "Verify email", description = "Verifica el email del usuario usando el token enviado por correo")
    @PostMapping(ApiPathConstants.AUTH_VERIFY_EMAIL)
    ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request);

    @Operation(summary = "Resend verification email", description = "Reenvía un token de verificación al email del usuario")
    @PostMapping(ApiPathConstants.AUTH_RESEND_VERIFICATION)
    ResponseEntity<ApiResponse<Void>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request);

    @Operation(summary = "Forgot password", description = "Solicita un enlace de recuperación de contraseña al email del usuario")
    @PostMapping(ApiPathConstants.AUTH_FORGOT_PASSWORD)
    ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request);

    @Operation(summary = "Reset password", description = "Establece una nueva contraseña usando el token de recuperación recibido por email")
    @PostMapping(ApiPathConstants.AUTH_RESET_PASSWORD)
    ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request);

    @Operation(summary = "Create OAuth2 Intent", description = "Crea un intento de registro OAuth2 con datos de empresa para ser procesado tras el login")
    @PostMapping(ApiPathConstants.AUTH_OAUTH2_INTENT)
    ResponseEntity<ApiResponse<auth.pymes.common.models.dto.response.OAuth2IntentResponse>> createOAuth2Intent(
            @Valid @RequestBody auth.pymes.common.models.dto.request.OAuth2IntentRequest request);
}

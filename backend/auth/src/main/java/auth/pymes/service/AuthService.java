package auth.pymes.service;

import auth.pymes.common.models.dto.request.LoginRequest;
import auth.pymes.common.models.dto.request.RegisterRequest;
import auth.pymes.common.models.dto.request.TokenRefreshRequest;
import auth.pymes.common.models.dto.response.AuthResponse;
import auth.pymes.common.models.dto.response.LogoutResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Servicio de autenticación y gestión de tokens.
 */
public interface AuthService {

    // ==================== LOCAL AUTH ====================

    /**
     * Inicia el registro: valida datos y envía email de verificación.
     */
    AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest);

    /**
     * Completa el registro persistiendo en DB tras verificar email.
     */
    AuthResponse completeRegistration(RegisterRequest request, HttpServletRequest httpRequest);

    /**
     * Login con email y contraseña.
     */
    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest);

    // ==================== TOKENS ====================

    /**
     * Hace logout del usuario (invalida tokens).
     */
    LogoutResponse logout(HttpServletRequest request);

    /**
     * Refresca el access token usando el refresh token.
     */
    AuthResponse refreshToken(TokenRefreshRequest request);

    /**
     * Intercambia un código OAuth2 de un solo uso por tokens JWT.
     */
    AuthResponse exchange(String code);
}

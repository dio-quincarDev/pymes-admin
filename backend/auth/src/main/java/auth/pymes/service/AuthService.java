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
     * Registro atómico: crea usuario, tenant (plan FREE) y vínculo OWNER.
     */
    AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest);

    /**
     * Login con email y contraseña.
     */
    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest);

    // ==================== TOKENS ====================

    /**
     * Hace logout del usuario (invalida tokens).
     */
    LogoutResponse logout(String accessToken);

    /**
     * Refresca el access token usando el refresh token.
     */
    AuthResponse refreshToken(TokenRefreshRequest request);
}

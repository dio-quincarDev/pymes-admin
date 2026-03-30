package auth.pymes.service;

import auth.pymes.common.models.entities.UserEntity;
import java.util.UUID;

/**
 * Contrato oficial para el manejo de seguridad JWT en PyMes Admin.
 * Define la generación, extracción y validación de tokens multi-tenant.
 */
public interface JwtService {

    /**
     * Genera un Access Token con identidad global y contexto tenant.
     * @param user El usuario autenticado
     * @param tenantId El ID de la empresa activa
     * @param role El rol del usuario en esa empresa
     * @return El token JWT firmado
     */
    String generateAccessToken(UserEntity user, UUID tenantId, String role);

    /**
     * Genera un Refresh Token (Vida larga) para persistencia de sesión.
     * @param user El usuario autenticado
     * @return El token JWT firmado
     */
    String generateRefreshToken(UserEntity user);

    /**
     * Extrae el ID del usuario del token.
     */
    UUID extractUserId(String token);

    /**
     * Extrae el ID del tenant activo del token.
     */
    UUID extractTenantId(String token);

    /**
     * Extrae el email (subject) del token.
     */
    String extractEmail(String token);

    /**
     * Extrae el rol asociado al tenant en el token.
     */
    String extractRole(String token);

    /**
     * Verifica si el token es estructuralmente válido y no ha expirado.
     */
    boolean isTokenValid(String token);
    
    /**
     * Marca un token como revocado en Redis (Logout).
     */
    void revokeToken(String token);

    /**
     * Verifica si un token ha sido revocado en Redis.
     */
    boolean isTokenRevoked(String token);
}

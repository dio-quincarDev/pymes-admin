package auth.pymes.service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Servicio para gestionar la recuperación de contraseña mediante tokens en Redis.
 */
public interface PasswordResetService {

    /**
     * Genera un token de reset, lo guarda en Redis (TTL 15 min).
     * Si el email no existe, retorna silenciosamente (timing attack prevention).
     *
     * @param email Email del usuario que solicita el reset
     * @param httpRequest La request HTTP para rate limiting por IP
     * @return true si se generó el token (email existe), false si el email no existe
     */
    boolean generateResetToken(String email, HttpServletRequest httpRequest);

    /**
     * Valida el token, actualiza la contraseña y elimina el token de Redis.
     *
     * @param token       Token de reset recibido del usuario
     * @param newPassword Nueva contraseña a establecer
     */
    void resetPassword(String token, String newPassword);
}

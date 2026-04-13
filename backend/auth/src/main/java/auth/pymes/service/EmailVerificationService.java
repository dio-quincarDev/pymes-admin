package auth.pymes.service;

import auth.pymes.common.models.entities.UserEntity;

/**
 * Servicio para gestionar la verificación de email mediante tokens en Redis.
 */
public interface EmailVerificationService {

    /**
     * Genera un token de verificación, lo guarda en Redis (TTL 15 min)
     * y retorna el token para su envío por email.
     */
    String generateVerificationToken(UserEntity user);

    /**
     * Valida el token de verificación y marca el email como verificado.
     * @param token Token de verificación recibido del usuario
     */
    void verifyEmail(String token);

    /**
     * Reenvía un token de verificación a un email.
     * Si el usuario ya está verificado, lanza EMAIL_ALREADY_VERIFIED.
     */
    String resendVerificationToken(String email);

    void createAndSendVerificationEmail(UserEntity user);
}

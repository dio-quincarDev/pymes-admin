package auth.pymes.service;

import auth.pymes.common.models.dto.request.RegisterRequest;
import auth.pymes.common.models.dto.request.VerifyEmailRequest;
import auth.pymes.common.models.dto.response.AuthResponse;
import auth.pymes.common.models.entities.UserEntity;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Servicio para gestionar la verificación de email mediante tokens en Redis.
 */
public interface EmailVerificationService {

    /**
     * Inicia el flujo de registro pendiente guardando los datos en Redis y enviando email.
     */
    void generateAndSendPendingRegistrationEmail(RegisterRequest request);

    /**
     * Genera un token de verificación, lo guarda en Redis (TTL 15 min)
     * y retorna el token para su envío por email.
     */
    String generateVerificationToken(UserEntity user);

    /**
     * Valida el token de verificación y activa la cuenta (o completa el registro).
     * @param request Datos de verificación (token + email)
     * @param httpRequest Datos de la petición para auditoría
     */
    AuthResponse verifyEmail(VerifyEmailRequest request, HttpServletRequest httpRequest);

    /**
     * Reenvía un token de verificación a un email.
     * Si el usuario ya está verificado, lanza EMAIL_ALREADY_VERIFIED.
     */
    String resendVerificationToken(String email);

    void createAndSendVerificationEmail(UserEntity user);
}

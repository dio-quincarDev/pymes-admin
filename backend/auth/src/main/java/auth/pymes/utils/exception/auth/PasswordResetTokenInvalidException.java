package auth.pymes.utils.exception.auth;

import auth.pymes.utils.exception.CodigoError;

/**
 * Excepción para token de recuperación de contraseña inválido o expirado (400).
 */
public class PasswordResetTokenInvalidException extends AuthApiException {

    public PasswordResetTokenInvalidException() {
        super(CodigoError.RESET_TOKEN_INVALID);
    }

    public PasswordResetTokenInvalidException(String mensaje) {
        super(CodigoError.RESET_TOKEN_INVALID, mensaje);
    }
}

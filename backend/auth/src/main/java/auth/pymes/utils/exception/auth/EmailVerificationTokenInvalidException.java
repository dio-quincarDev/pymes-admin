package auth.pymes.utils.exception.auth;

import auth.pymes.utils.exception.CodigoError;

/**
 * Excepción para token de verificación de email inválido (400).
 */
public class EmailVerificationTokenInvalidException extends AuthApiException {

    public EmailVerificationTokenInvalidException() {
        super(CodigoError.VERIFICATION_TOKEN_INVALID);
    }

    public EmailVerificationTokenInvalidException(String mensaje) {
        super(CodigoError.VERIFICATION_TOKEN_INVALID, mensaje);
    }
}

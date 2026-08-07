package auth.pymes.utils.exception.token;

import auth.pymes.utils.exception.CodigoError;
import auth.pymes.utils.exception.auth.AuthApiException;

/**
 * Excepción para tokens JWT expirados (401).
 */
public class TokenExpiredException extends AuthApiException {

    public TokenExpiredException() {
        super(CodigoError.TOKEN_EXPIRED);
    }

    public TokenExpiredException(String mensaje) {
        super(CodigoError.TOKEN_EXPIRED, mensaje);
    }
}

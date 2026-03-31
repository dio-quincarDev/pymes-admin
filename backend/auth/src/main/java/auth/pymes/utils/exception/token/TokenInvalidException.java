package auth.pymes.utils.exception.token;

import auth.pymes.utils.exception.CodigoError;
import auth.pymes.utils.exception.auth.AuthApiException;

/**
 * Excepción para tokens JWT inválidos o malformados (401).
 */
public class TokenInvalidException extends AuthApiException {

    public TokenInvalidException() {
        super(CodigoError.TOKEN_INVALID);
    }

    public TokenInvalidException(String mensaje) {
        super(CodigoError.TOKEN_INVALID, mensaje);
    }
}

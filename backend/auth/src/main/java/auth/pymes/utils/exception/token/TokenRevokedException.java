package auth.pymes.utils.exception.token;

import auth.pymes.utils.exception.CodigoError;
import auth.pymes.utils.exception.auth.AuthApiException;

/**
 * Excepción para tokens JWT revocados (401).
 * Se usa cuando un usuario hace logout y el token está en blacklist.
 */
public class TokenRevokedException extends AuthApiException {

    public TokenRevokedException() {
        super(CodigoError.TOKEN_REVOKED);
    }

    public TokenRevokedException(String mensaje) {
        super(CodigoError.TOKEN_REVOKED, mensaje);
    }
}

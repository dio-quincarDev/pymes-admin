package auth.pymes.utils.exception.auth;

import auth.pymes.utils.exception.CodigoError;

/**
 * Excepción para errores de autorización (403).
 * Usar cuando el usuario no tiene permisos para una acción.
 */
public class AuthorizationException extends AuthApiException {

    public AuthorizationException(CodigoError codigoError) {
        super(codigoError);
    }

    public AuthorizationException(CodigoError codigoError, String mensaje) {
        super(codigoError, mensaje);
    }

    public AuthorizationException(CodigoError codigoError, Object... params) {
        super(codigoError, params);
    }
}

package auth.pymes.utils.exception.auth;

import auth.pymes.utils.exception.CodigoError;

/**
 * Excepción para errores de autenticación (401).
 * Usar cuando las credenciales son inválidas o el usuario no está autenticado.
 */
public class AuthenticationException extends AuthApiException {

    public AuthenticationException(CodigoError codigoError) {
        super(codigoError);
    }

    public AuthenticationException(CodigoError codigoError, String mensaje) {
        super(codigoError, mensaje);
    }

    public AuthenticationException(CodigoError codigoError, Object... params) {
        super(codigoError, params);
    }
}

package auth.pymes.utils.exception.auth;

import auth.pymes.utils.exception.CodigoError;

/**
 * Excepción genérica de la aplicación.
 * Usar cuando no haya una excepción específica para el caso.
 *
 * Nota: Preferir usar las excepciones específicas (ResourceNotFoundException,
 * DuplicateResourceException, etc.) para mejor claridad del código.
 */
public class TotalAuthException extends AuthApiException {

    public TotalAuthException(CodigoError codigoError) {
        super(codigoError);
    }

    public TotalAuthException(CodigoError codigoError, String mensaje) {
        super(codigoError, mensaje);
    }

    public TotalAuthException(CodigoError codigoError, Object... params) {
        super(codigoError, params);
    }
}

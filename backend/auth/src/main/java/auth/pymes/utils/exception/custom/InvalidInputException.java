package auth.pymes.utils.exception.custom;

import auth.pymes.utils.exception.CodigoError;
import auth.pymes.utils.exception.auth.AuthApiException;

/**
 * Excepción para datos de entrada inválidos (400).
 * Usar para validaciones manuales de negocio.
 */
public class InvalidInputException extends AuthApiException {

    public InvalidInputException(CodigoError codigoError) {
        super(codigoError);
    }

    public InvalidInputException(CodigoError codigoError, String mensaje) {
        super(codigoError, mensaje);
    }

    public InvalidInputException(CodigoError codigoError, Object... params) {
        super(codigoError, params);
    }
}

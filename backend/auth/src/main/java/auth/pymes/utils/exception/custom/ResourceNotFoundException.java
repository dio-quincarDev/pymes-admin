package auth.pymes.utils.exception.custom;

import auth.pymes.utils.exception.CodigoError;
import auth.pymes.utils.exception.auth.AuthApiException;

/**
 * Excepción para recursos no encontrados (404).
 * Usar cuando un usuario, tenant, invitación, etc. no existe.
 */
public class ResourceNotFoundException extends AuthApiException {

    public ResourceNotFoundException(CodigoError codigoError, Object... params) {
        super(codigoError, params);
    }

    public ResourceNotFoundException(CodigoError codigoError) {
        super(codigoError);
    }
}

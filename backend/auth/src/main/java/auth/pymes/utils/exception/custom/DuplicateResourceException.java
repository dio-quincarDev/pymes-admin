package auth.pymes.utils.exception.custom;

import auth.pymes.utils.exception.CodigoError;
import auth.pymes.utils.exception.auth.AuthApiException;

/**
 * Excepción para recursos duplicados (409 Conflict).
 * Usar cuando hay conflicto de unicidad (email, slug, etc.).
 */
public class DuplicateResourceException extends AuthApiException {

    public DuplicateResourceException(CodigoError codigoError, Object... params) {
        super(codigoError, params);
    }

    public DuplicateResourceException(CodigoError codigoError) {
        super(codigoError);
    }
}

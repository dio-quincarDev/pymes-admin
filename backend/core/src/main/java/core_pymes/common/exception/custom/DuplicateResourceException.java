package core_pymes.common.exception.custom;

import core_pymes.common.exception.CodigoError;
import core_pymes.common.exception.CoreApiException;

public class DuplicateResourceException extends CoreApiException {
    public DuplicateResourceException(String message) {
        super(CodigoError.DUPLICATE_RESOURCE, message);
    }
}

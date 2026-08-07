package core_pymes.common.exception.custom;

import core_pymes.common.exception.CodigoError;
import core_pymes.common.exception.CoreApiException;

public class ResourceNotFoundException extends CoreApiException {
    public ResourceNotFoundException(String message) {
        super(CodigoError.RESOURCE_NOT_FOUND, message);
    }
}

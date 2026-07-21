package core_pymes.common.exception.custom;

import core_pymes.common.exception.CodigoError;
import core_pymes.common.exception.CoreApiException;

public class InvalidInputException extends CoreApiException {
    public InvalidInputException(String message) {
        super(CodigoError.INVALID_INPUT, message);
    }
}

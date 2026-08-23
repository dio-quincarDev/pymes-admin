package core_pymes.common.exception;

import org.springframework.http.HttpStatus;

public class CoreApiException extends RuntimeException {
    private final CodigoError codigo;
    private final HttpStatus httpStatus;

    public CoreApiException(CodigoError codigo, String message) {
        this(codigo, message, toHttpStatus(codigo));
    }

    public CoreApiException(CodigoError codigo, String message, HttpStatus httpStatus) {
        super(message);
        this.codigo = codigo;
        this.httpStatus = httpStatus;
    }

    public CodigoError getCodigo() { return codigo; }
    public HttpStatus getHttpStatus() { return httpStatus; }

    private static HttpStatus toHttpStatus(CodigoError codigo) {
        return switch (codigo) {
            case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_INPUT, VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
case DUPLICATE_RESOURCE, CONSTRAINT_VIOLATION -> HttpStatus.CONFLICT;
			case INSUFFICIENT_PERMISSIONS -> HttpStatus.FORBIDDEN;
			case SEC_AUTH -> HttpStatus.UNAUTHORIZED;
            case SEC_FORBIDDEN -> HttpStatus.FORBIDDEN;
            case SEC_TOKEN_EXPIRED -> HttpStatus.UNAUTHORIZED;
            case INTERNAL_SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}

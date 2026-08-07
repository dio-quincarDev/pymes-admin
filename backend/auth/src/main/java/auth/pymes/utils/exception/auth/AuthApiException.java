package auth.pymes.utils.exception.auth;

import auth.pymes.utils.exception.CodigoError;
import org.springframework.http.HttpStatus;

/**
 * Excepción base para todas las excepciones del microservicio de Auth.
 * Usa CodigoError enum para consistencia en los códigos de error.
 */
public abstract class AuthApiException extends RuntimeException {

    private final CodigoError codigoError;

    public AuthApiException(CodigoError codigoError) {
        super(codigoError.getMensaje());
        this.codigoError = codigoError;
    }

    public AuthApiException(CodigoError codigoError, String mensaje) {
        super(mensaje);
        this.codigoError = codigoError;
    }

    public AuthApiException(CodigoError codigoError, Object... params) {
        super(codigoError.getMensaje(params));
        this.codigoError = codigoError;
    }

    public AuthApiException(CodigoError codigoError, Throwable cause) {
        super(codigoError.getMensaje(), cause);
        this.codigoError = codigoError;
    }

    public AuthApiException(CodigoError codigoError, String mensaje, Throwable cause) {
        super(mensaje, cause);
        this.codigoError = codigoError;
    }

    public String getCodigo() {
        return codigoError.getCodigo();
    }

    public CodigoError getCodigoError() {
        return codigoError;
    }

    public HttpStatus getHttpStatus() {
        return codigoError.getHttpStatus();
    }
}

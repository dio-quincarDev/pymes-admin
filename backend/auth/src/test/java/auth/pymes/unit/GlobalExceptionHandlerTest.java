package auth.pymes.unit;

import auth.pymes.common.models.dto.response.ErrorResponse;
import auth.pymes.utils.exception.CodigoError;
import auth.pymes.utils.exception.GlobalExceptionHandler;
import auth.pymes.utils.exception.custom.DuplicateResourceException;
import auth.pymes.utils.exception.custom.ResourceNotFoundException;
import auth.pymes.utils.exception.token.TokenExpiredException;
import auth.pymes.utils.exception.token.TokenInvalidException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request =
            new MockHttpServletRequest("POST", "/api/v1/auth/login");

    @Test
    @DisplayName("Credenciales inválidas: 401 AUTH001 (no 400)")
    void credencialesInvalidas_Devuelve401Auth001() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBadCredentials(new BadCredentialsException("bad"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().codigo()).isEqualTo(CodigoError.INVALID_CREDENTIALS.getCodigo());
    }

    @Test
    @DisplayName("Acceso denegado: 403 ROLE003")
    void accesoDenegado_Devuelve403Role003() {
        ResponseEntity<ErrorResponse> response =
                handler.handleAccessDenied(new AccessDeniedException("denied"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().codigo()).isEqualTo(CodigoError.INSUFFICIENT_PERMISSIONS.getCodigo());
    }

    @Test
    @DisplayName("Token inválido: 401 AUTH004")
    void tokenInvalido_Devuelve401Auth004() {
        ResponseEntity<ErrorResponse> response =
                handler.handleTokenInvalid(new TokenInvalidException(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().codigo()).isEqualTo(CodigoError.TOKEN_INVALID.getCodigo());
    }

    @Test
    @DisplayName("Token expirado: 401 AUTH003")
    void tokenExpirado_Devuelve401Auth003() {
        ResponseEntity<ErrorResponse> response =
                handler.handleTokenExpired(new TokenExpiredException("expirado"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().codigo()).isEqualTo(CodigoError.TOKEN_EXPIRED.getCodigo());
    }

    @Test
    @DisplayName("Recurso inexistente: 404 con el código propio de la excepción")
    void recursoNoEncontrado_Devuelve404() {
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(
                new ResourceNotFoundException(CodigoError.TENANT_NOT_FOUND, "abc"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().codigo()).isEqualTo(CodigoError.TENANT_NOT_FOUND.getCodigo());
    }

    @Test
    @DisplayName("Recurso duplicado: 409 TNT003")
    void recursoDuplicado_Devuelve409Tnt003() {
        ResponseEntity<ErrorResponse> response = handler.handleDuplicateResource(
                new DuplicateResourceException(CodigoError.TENANT_ALREADY_EXISTS, "mi-slug"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().codigo()).isEqualTo(CodigoError.TENANT_ALREADY_EXISTS.getCodigo());
    }

    @Test
    @DisplayName("Violación de unicidad: 409 con mensaje genérico, sin SQL crudo")
    void violacionDeUnicidad_Devuelve409SinExponerSql() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"tenants_slug_key\"");

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().codigo()).isEqualTo(CodigoError.CONSTRAINT_VIOLATION.getCodigo());
        assertThat(response.getBody().mensaje())
                .isEqualTo("A resource with the same unique key already exists")
                .doesNotContain("tenants_slug_key");
    }

    @Test
    @DisplayName("FK violada: 409 con mensaje de borrado referenciado")
    void foreignKeyViolada_Devuelve409() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "update or delete on table \"tenants\" violates foreign key constraint");

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().mensaje())
                .isEqualTo("The resource is referenced by another entity and cannot be deleted");
    }

    @Test
    @DisplayName("Excepción no manejada: 500 ERR999 sin filtrar el mensaje interno")
    void excepcionGenerica_Devuelve500SinFiltrarInterno() {
        ResponseEntity<ErrorResponse> response = handler.handleGeneralException(
                new IllegalStateException("detalle interno: /etc/shadow"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().codigo()).isEqualTo(CodigoError.INTERNAL_SERVER_ERROR.getCodigo());
        assertThat(response.getBody().mensaje())
                .doesNotContain("detalle interno")
                .doesNotContain("/etc/shadow");
    }

    @Test
    @DisplayName("Método HTTP no soportado: 405 RSC002")
    void metodoNoSoportado_Devuelve405() {
        ResponseEntity<ErrorResponse> response = handler.handleMethodNotSupported(
                new HttpRequestMethodNotSupportedException("PUT"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody().codigo()).isEqualTo(CodigoError.METHOD_NOT_ALLOWED.getCodigo());
    }

    @Test
    @DisplayName("Validación de campos: 400 VAL001 con detalle por campo")
    void validacion_Devuelve400ConDetallesPorCampo() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors())
                .thenReturn(List.of(new FieldError("registerRequest", "email", "no debe estar vacío")));

        ResponseEntity<ErrorResponse> response = handler.handleValidationExceptions(
                new MethodArgumentNotValidException(null, bindingResult), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().codigo()).isEqualTo(CodigoError.VALIDATION_ERROR.getCodigo());
        assertThat(response.getBody().detalles()).containsEntry("email", "no debe estar vacío");
    }
}

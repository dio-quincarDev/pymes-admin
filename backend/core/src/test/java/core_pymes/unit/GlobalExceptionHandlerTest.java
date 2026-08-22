package core_pymes.unit;

import core_pymes.common.dto.ErrorResponse;
import core_pymes.common.exception.CodigoError;
import core_pymes.common.exception.CoreApiException;
import core_pymes.common.exception.GlobalExceptionHandler;
import core_pymes.common.exception.custom.DuplicateResourceException;
import core_pymes.common.exception.custom.InvalidInputException;
import core_pymes.common.exception.custom.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @BeforeEach
    void setUp() {
        request.setRequestURI("/api/test");
    }

    @Test
    void resourceNotFound_returns404() {
        var ex = new ResourceNotFoundException("test resource");
        var response = handler.handleResourceNotFound(ex, request);
        assertResponse(response, HttpStatus.NOT_FOUND, "RES001", "test resource", 404);
    }

    @Test
    void invalidInput_returns400() {
        var ex = new InvalidInputException("bad input");
        var response = handler.handleInvalidInput(ex, request);
        assertResponse(response, HttpStatus.BAD_REQUEST, "INV001", "bad input", 400);
    }

    @Test
    void duplicateResource_returns409() {
        var ex = new DuplicateResourceException("dup");
        var response = handler.handleDuplicateResource(ex, request);
        assertResponse(response, HttpStatus.CONFLICT, "DUP001", "dup", 409);
    }

    @Test
    void coreApiException_usesDynamicHttpStatus() {
        var ex = new CoreApiException(CodigoError.CONSTRAINT_VIOLATION, "constraint fail");
        var response = handler.handleCoreApiException(ex, request);
        assertResponse(response, HttpStatus.CONFLICT, "CON001", "constraint fail", 409);
    }

    @Test
    void validation_returnsDetails() {
        var bindingResult = mock(BindingResult.class);
        var fieldErrors = List.of(
                new FieldError("obj", "name", "must not be blank"),
                new FieldError("obj", "email", "invalid format"));
        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);
        var ex = new MethodArgumentNotValidException(null, bindingResult);

        var response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.codigo()).isEqualTo("VAL001");
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.details()).containsEntry("name", "must not be blank");
        assertThat(body.details()).containsEntry("email", "invalid format");
    }

    @Test
    void malformedJson_returns400() {
        var ex = new HttpMessageNotReadableException("JSON parse error", new RuntimeException());
        var response = handler.handleMalformedJson(ex, request);
        assertResponse(response, HttpStatus.BAD_REQUEST, "INV001", "Malformed JSON request body", 400);
    }

    @Test
    void missingParam_returns400() {
        var ex = new MissingServletRequestParameterException("page", "int");
        var response = handler.handleMissingParam(ex, request);
        assertResponse(response, HttpStatus.BAD_REQUEST, "VAL001", "Required request parameter 'page' for method parameter type int is not present", 400);
    }

    @Test
    void dataIntegrity_duplicateKey_returnsConflict() {
        var cause = new java.sql.SQLException("ERROR: duplicate key value violates unique constraint");
        var ex = new DataIntegrityViolationException("duplicate", cause);
        var response = handler.handleDataIntegrity(ex, request);
        assertResponse(response, HttpStatus.CONFLICT, "CON001", "A resource with the same unique key already exists", 409);
    }

    @Test
    void dataIntegrity_foreignKey_returnsConflict() {
        var cause = new java.sql.SQLException("ERROR: insert or update on table violates foreign key constraint");
        var ex = new DataIntegrityViolationException("fk violation", cause);
        var response = handler.handleDataIntegrity(ex, request);
        assertResponse(response, HttpStatus.CONFLICT, "CON001", "The resource is referenced by another entity and cannot be deleted", 409);
    }

    @Test
    void dataIntegrity_generic_returnsConflict() {
        var ex = new DataIntegrityViolationException("some error");
        var response = handler.handleDataIntegrity(ex, request);
        assertResponse(response, HttpStatus.CONFLICT, "CON001", "Data integrity constraint violation", 409);
    }

    @Test
    void entityNotFound_returns404() {
        var ex = new EntityNotFoundException("not found");
        var response = handler.handleEntityNotFound(ex, request);
        assertResponse(response, HttpStatus.NOT_FOUND, "RES001", "not found", 404);
    }

    @Test
    void illegalArgument_returns400() {
        var ex = new IllegalArgumentException("bad arg");
        var response = handler.handleBadRequest(ex, request);
        assertResponse(response, HttpStatus.BAD_REQUEST, "INV001", "bad arg", 400);
    }

    @Test
    void accessDenied_returns403() {
        var ex = new AccessDeniedException("Access is denied");
        var response = handler.handleAccessDenied(ex, request);
        assertResponse(response, HttpStatus.FORBIDDEN, "ROLE003", "User does not have permission to perform this action", 403);
    }

    @Test
    void generic_returns500() {
        var ex = new RuntimeException("unexpected");
        var response = handler.handleGeneric(ex, request);
        assertResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "INT001", "Internal server error", 500);
    }

    private void assertResponse(ResponseEntity<ErrorResponse> response, HttpStatus expectedStatus,
                                String expectedCodigo, String expectedMessage, int expectedStatusValue) {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.codigo()).isEqualTo(expectedCodigo);
        assertThat(body.message()).isEqualTo(expectedMessage);
        assertThat(body.path()).isEqualTo("/api/test");
        assertThat(body.status()).isEqualTo(expectedStatusValue);
    }
}

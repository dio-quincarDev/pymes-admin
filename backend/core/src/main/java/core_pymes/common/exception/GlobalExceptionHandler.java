package core_pymes.common.exception;

import core_pymes.common.dto.ErrorResponse;
import core_pymes.common.exception.custom.DuplicateResourceException;
import core_pymes.common.exception.custom.InvalidInputException;
import core_pymes.common.exception.custom.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("ResourceNotFound en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getCodigo().getCodigo(), ex.getMessage(), request.getRequestURI(), HttpStatus.NOT_FOUND.value()));
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInput(InvalidInputException ex, HttpServletRequest request) {
        log.warn("InvalidInput en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getCodigo().getCodigo(), ex.getMessage(), request.getRequestURI(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("DuplicateResource en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getCodigo().getCodigo(), ex.getMessage(), request.getRequestURI(), HttpStatus.CONFLICT.value()));
    }

    @ExceptionHandler(CoreApiException.class)
    public ResponseEntity<ErrorResponse> handleCoreApiException(CoreApiException ex, HttpServletRequest request) {
        log.warn("CoreApiException [{}] en {}: {}", ex.getCodigo().getCodigo(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
                .body(new ErrorResponse(ex.getCodigo().getCodigo(), ex.getMessage(), request.getRequestURI(), ex.getHttpStatus().value()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var details = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        e -> e.getField(),
                        e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "invalid",
                        (a, b) -> a + "; " + b));
        log.warn("Validation error en {}: {}", request.getRequestURI(), details);
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(CodigoError.VALIDATION_ERROR.getCodigo(), "Validation failed", request.getRequestURI(), HttpStatus.BAD_REQUEST.value(), details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed JSON en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(CodigoError.INVALID_INPUT.getCodigo(), "Malformed JSON request body", request.getRequestURI(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Missing param en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(CodigoError.VALIDATION_ERROR.getCodigo(), ex.getMessage(), request.getRequestURI(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("ConstraintViolation en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(CodigoError.CONSTRAINT_VIOLATION.getCodigo(), ex.getMessage(), request.getRequestURI(), HttpStatus.CONFLICT.value()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        var root = ex.getRootCause() != null ? ex.getRootCause() : ex;
        var dbMsg = root.getMessage() != null ? root.getMessage() : ex.getMessage();
        log.warn("DataIntegrityViolation en {}: {}", request.getRequestURI(), dbMsg);
        String userMsg;
        if (dbMsg.toLowerCase().contains("duplicate key") || dbMsg.toLowerCase().contains("unique constraint")) {
            userMsg = "A resource with the same unique key already exists";
        } else if (dbMsg.toLowerCase().contains("foreign key")) {
            userMsg = "The resource is referenced by another entity and cannot be deleted";
        } else {
            userMsg = "Data integrity constraint violation";
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(CodigoError.CONSTRAINT_VIOLATION.getCodigo(), userMsg, request.getRequestURI(), HttpStatus.CONFLICT.value()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        log.warn("EntityNotFoundException en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(CodigoError.RESOURCE_NOT_FOUND.getCodigo(), ex.getMessage(), request.getRequestURI(), HttpStatus.NOT_FOUND.value()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("IllegalArgument en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(CodigoError.INVALID_INPUT.getCodigo(), ex.getMessage(), request.getRequestURI(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception en {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(CodigoError.INTERNAL_SERVER_ERROR.getCodigo(), CodigoError.INTERNAL_SERVER_ERROR.getMensaje(), request.getRequestURI(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
}

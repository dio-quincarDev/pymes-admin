package auth.pymes.utils.exception;

import auth.pymes.utils.exception.auth.AuthApiException;
import auth.pymes.utils.exception.auth.AuthenticationException;
import auth.pymes.utils.exception.auth.AuthorizationException;
import auth.pymes.utils.exception.custom.DuplicateResourceException;
import auth.pymes.utils.exception.custom.InvalidInputException;
import auth.pymes.utils.exception.custom.ResourceNotFoundException;
import auth.pymes.utils.exception.token.TokenExpiredException;
import auth.pymes.utils.exception.token.TokenInvalidException;
import auth.pymes.utils.exception.token.TokenRevokedException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para la API de Autenticación.
 * Captura todas las excepciones no manejadas y retorna respuestas estandarizadas.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ==================== APLICACIÓN (AuthApiException) ====================

    @ExceptionHandler(AuthApiException.class)
    public ResponseEntity<ErrorResponse> handleAuthApiException(
            AuthApiException ex, HttpServletRequest request) {
        log.warn("AuthApiException [{}] en {}: {}", ex.getCodigo(), request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getCodigo(),
                ex.getMessage(),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }

    // ==================== AUTHENTICATION ====================

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        log.warn("BadCredentialsException en {}: Credenciales inválidas", request.getRequestURI());
        
        ErrorResponse errorResponse = new ErrorResponse(
                CodigoError.INVALID_CREDENTIALS.getCodigo(),
                CodigoError.INVALID_CREDENTIALS.getMensaje(),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("AccessDeniedException en {}: Acceso denegado", request.getRequestURI());
        
        ErrorResponse errorResponse = new ErrorResponse(
                CodigoError.INSUFFICIENT_PERMISSIONS.getCodigo(),
                CodigoError.INSUFFICIENT_PERMISSIONS.getMensaje(),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    // ==================== JWT TOKEN EXCEPTIONS ====================

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwt(
            ExpiredJwtException ex, HttpServletRequest request) {
        log.warn("ExpiredJwtException en {}: Token expirado", request.getRequestURI());
        
        ErrorResponse errorResponse = new ErrorResponse(
                CodigoError.TOKEN_EXPIRED.getCodigo(),
                CodigoError.TOKEN_EXPIRED.getMensaje(),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJwt(
            MalformedJwtException ex, HttpServletRequest request) {
        log.warn("MalformedJwtException en {}: Token malformado", request.getRequestURI());
        
        ErrorResponse errorResponse = new ErrorResponse(
                CodigoError.TOKEN_INVALID.getCodigo(),
                CodigoError.TOKEN_INVALID.getMensaje(),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ErrorResponse> handleSignatureException(
            SignatureException ex, HttpServletRequest request) {
        log.warn("SignatureException en {}: Firma JWT inválida", request.getRequestURI());
        
        ErrorResponse errorResponse = new ErrorResponse(
                CodigoError.TOKEN_INVALID.getCodigo(),
                "La firma del token no es válida",
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpired(
            TokenExpiredException ex, HttpServletRequest request) {
        log.warn("TokenExpiredException en {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getCodigo(),
                ex.getMessage(),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(TokenRevokedException.class)
    public ResponseEntity<ErrorResponse> handleTokenRevoked(
            TokenRevokedException ex, HttpServletRequest request) {
        log.warn("TokenRevokedException en {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getCodigo(),
                ex.getMessage(),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(TokenInvalidException.class)
    public ResponseEntity<ErrorResponse> handleTokenInvalid(
            TokenInvalidException ex, HttpServletRequest request) {
        log.warn("TokenInvalidException en {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getCodigo(),
                ex.getMessage(),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    // ==================== VALIDATION EXCEPTIONS ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        // Capturar TODOS los errores de validación, no solo el primero
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Campo inválido",
                        (existing, replacement) -> existing + "; " + replacement
                ));
        
        String firstErrorMessage = fieldErrors.values().stream().findFirst().orElse("Validation failed");
        log.warn("Validation error en {}: {} - Details: {}", request.getRequestURI(), firstErrorMessage, fieldErrors);
        
        ErrorResponse errorResponse = new ErrorResponse(
                CodigoError.VALIDATION_ERROR.getCodigo(),
                "Validation failed: " + firstErrorMessage,
                request.getRequestURI(),
                fieldErrors
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInput(
            InvalidInputException ex, HttpServletRequest request) {
        log.warn("InvalidInputException en {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getCodigo(),
                ex.getMessage(),
                request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    // ==================== DATABASE EXCEPTIONS ====================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        
        Throwable root = ex.getRootCause() != null ? ex.getRootCause() : ex;
        String dbMessage = root.getMessage() != null ? root.getMessage() : ex.getMessage();
        
        log.warn("DataIntegrityViolationException en {}: {}", request.getRequestURI(), dbMessage);
        
        // Detectar si es violación de unicidad
        String userMessage;
        if (dbMessage.toLowerCase().contains("duplicate key") || 
            dbMessage.toLowerCase().contains("unique constraint")) {
            userMessage = "A resource with the same unique key already exists";
        } else if (dbMessage.toLowerCase().contains("foreign key")) {
            userMessage = "The resource is referenced by another entity and cannot be deleted";
        } else {
            userMessage = "Data integrity constraint violation";
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
                CodigoError.CONSTRAINT_VIOLATION.getCodigo(),
                userMessage,
                request.getRequestURI(),
                Map.of("details", dbMessage)
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    // ==================== RESOURCE NOT FOUND ====================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("ResourceNotFoundException en {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getCodigo(),
                ex.getMessage(),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // ==================== DUPLICATE RESOURCE ====================

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("DuplicateResourceException en {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getCodigo(),
                ex.getMessage(),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    // ==================== FALLBACK GENERAL ====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception en {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
                CodigoError.INTERNAL_SERVER_ERROR.getCodigo(),
                CodigoError.INTERNAL_SERVER_ERROR.getMensaje(),
                request.getRequestURI()
        );
        
        return ResponseEntity.internalServerError().body(errorResponse);
    }
}

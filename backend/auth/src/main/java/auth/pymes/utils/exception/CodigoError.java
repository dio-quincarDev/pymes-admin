package auth.pymes.utils.exception;

import org.springframework.http.HttpStatus;

import java.text.MessageFormat;

/**
 * Códigos de error estandarizados para la API de Autenticación.
 * Convención: XXX001, XXX002, etc. por categoría
 */
public enum CodigoError {
    
    // ==================== AUTHENTICATION (AUTH) ====================
    INVALID_CREDENTIALS("AUTH001", "Invalid username or password", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED_ACCESS("AUTH002", "User is not authenticated", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("AUTH003", "Access token has expired", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("AUTH004", "Access token is invalid or malformed", HttpStatus.UNAUTHORIZED),
    TOKEN_REVOKED("AUTH005", "Access token has been revoked (logout)", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED("AUTH006", "Refresh token has expired", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_INVALID("AUTH007", "Refresh token is invalid", HttpStatus.UNAUTHORIZED),
    OAUTH2_PROVIDER_ERROR("AUTH008", "Error connecting to OAuth2 provider: {0}", HttpStatus.BAD_GATEWAY),
    
    // ==================== USER (USR) ====================
    USER_NOT_FOUND_BY_ID("USR001", "User with ID {0} does not exist", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND_BY_EMAIL("USR002", "User with email {0} does not exist", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND_BY_PROVIDER("USR003", "User with provider {0} and ID {1} does not exist", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("USR004", "User with email {0} is already registered", HttpStatus.CONFLICT),
    USER_INACTIVE("USR005", "User account is inactive", HttpStatus.FORBIDDEN),
    USER_LOCKED("USR006", "User account is locked", HttpStatus.FORBIDDEN),
    
    // ==================== TENANT (TNT) ====================
    TENANT_NOT_FOUND("TNT001", "Tenant with ID {0} does not exist", HttpStatus.NOT_FOUND),
    TENANT_NOT_FOUND_BY_SLUG("TNT002", "Tenant with slug {0} does not exist", HttpStatus.NOT_FOUND),
    TENANT_ALREADY_EXISTS("TNT003", "Tenant with slug {0} already exists", HttpStatus.CONFLICT),
    TENANT_INACTIVE("TNT004", "Tenant is inactive", HttpStatus.FORBIDDEN),
    USER_NOT_IN_TENANT("TNT005", "User does not belong to tenant {0}", HttpStatus.FORBIDDEN),
    INVALID_TENANT_ACCESS("TNT006", "User does not have access to tenant {0}", HttpStatus.FORBIDDEN),
    MAX_USERS_REACHED("TNT007", "Tenant has reached maximum number of users ({0})", HttpStatus.CONFLICT),
    
    // ==================== ROLE & PERMISSION (ROLE) ====================
    ROLE_NOT_FOUND("ROLE001", "Role {0} does not exist", HttpStatus.NOT_FOUND),
    INVALID_ROLE("ROLE002", "Invalid role: {0}", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_PERMISSIONS("ROLE003", "User does not have permission to perform this action", HttpStatus.FORBIDDEN),
    OWNER_CANNOT_BE_REMOVED("ROLE004", "The owner of the tenant cannot be removed", HttpStatus.BAD_REQUEST),
    
    // ==================== INVITATION (INV) ====================
    INVITATION_NOT_FOUND("INV001", "Invitation with token {0} does not exist", HttpStatus.NOT_FOUND),
    INVITATION_EXPIRED("INV002", "Invitation has expired", HttpStatus.BAD_REQUEST),
    INVITATION_ALREADY_ACCEPTED("INV003", "Invitation has already been accepted", HttpStatus.CONFLICT),
    EMAIL_ALREADY_INVITED("INV004", "Email {0} has already been invited to this tenant", HttpStatus.CONFLICT),
    
    // ==================== VALIDATION & INTEGRITY (VAL) ====================
    VALIDATION_ERROR("VAL001", "Validation failed: {0}", HttpStatus.BAD_REQUEST),
    INVALID_INPUT("VAL002", "Invalid input data", HttpStatus.BAD_REQUEST),
    CONSTRAINT_VIOLATION("VAL003", "Data integrity constraint violation: {0}", HttpStatus.CONFLICT),
    DUPLICATE_RESOURCE("VAL004", "Resource already exists: {0}", HttpStatus.CONFLICT),
    
    // ==================== RESOURCE NOT FOUND (404) ====================
    RESOURCE_NOT_FOUND("RSC001", "The requested resource was not found", HttpStatus.NOT_FOUND),
    
    // ==================== INTERNAL ERRORS (500) ====================
    INTERNAL_SERVER_ERROR("ERR999", "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String codigo;
    private final String mensaje;
    private final HttpStatus httpStatus;

    CodigoError(String codigo, String mensaje, HttpStatus httpStatus) {
        this.codigo = codigo;
        this.mensaje = mensaje;
        this.httpStatus = httpStatus;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getMensaje(Object... params) {
        return MessageFormat.format(mensaje, params);
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}

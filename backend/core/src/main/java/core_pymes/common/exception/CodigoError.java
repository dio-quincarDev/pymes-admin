package core_pymes.common.exception;

public enum CodigoError {
    RESOURCE_NOT_FOUND("RES001", "Resource not found"),
    INVALID_INPUT("INV001", "Invalid input"),
    DUPLICATE_RESOURCE("DUP001", "Duplicate resource"),
    CONSTRAINT_VIOLATION("CON001", "Constraint violation"),
    VALIDATION_ERROR("VAL001", "Validation error"),
    INSUFFICIENT_PERMISSIONS("ROLE003", "User does not have permission to perform this action"),
    SEC_AUTH("SEC001", "Authentication required"),
    SEC_FORBIDDEN("SEC002", "Access denied"),
    SEC_TOKEN_EXPIRED("SEC003", "Token expired"),
    INTERNAL_SERVER_ERROR("INT001", "Internal server error"),
    ;

    private final String codigo;
    private final String mensaje;

    CodigoError(String codigo, String mensaje) {
        this.codigo = codigo;
        this.mensaje = mensaje;
    }

    public String getCodigo() { return codigo; }
    public String getMensaje() { return mensaje; }
}

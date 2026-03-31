package auth.pymes.common.models.dto.response;

import auth.pymes.utils.exception.ErrorResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Respuesta estandarizada para todos los endpoints de la API.
 *
 * @param <T> Tipo de dato del payload exitoso
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorResponse error
) {
    /**
     * Crea una respuesta exitosa con datos.
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /**
     * Crea una respuesta exitosa sin datos (ej: DELETE 204).
     */
    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(true, null, null);
    }

    /**
     * Crea una respuesta de error.
     */
    public static <T> ApiResponse<T> errorResponse(ErrorResponse error) {
        return new ApiResponse<>(false, null, error);
    }
}

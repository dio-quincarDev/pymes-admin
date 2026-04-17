package auth.pymes.common.models.dto.response;

import java.time.Instant;
import java.util.Map;

/**
 * Respuesta de error estandarizada para la API.
 * 
 * @param codigo     Código de error machine-readable (ej: "USER_NOT_FOUND")
 * @param mensaje    Mensaje descriptivo para humanos
 * @param path       Ruta de la petición que falló
 * @param timestamp  Instante en que ocurrió el error (ISO-8601)
 * @param detalles   Mapa de errores adicionales (ej: validación de campos)
 */
public record ErrorResponse(
        String codigo,
        String mensaje,
        String path,
        String timestamp,
        Map<String, String> detalles
) {
    // Constructor conveniente sin detalles
    public ErrorResponse(String codigo, String mensaje, String path) {
        this(codigo, mensaje, path, Instant.now().toString(), Map.of());
    }

    // Constructor conveniente con detalles
    public ErrorResponse(String codigo, String mensaje, String path, Map<String, String> detalles) {
        this(codigo, mensaje, path, Instant.now().toString(), detalles);
    }
}

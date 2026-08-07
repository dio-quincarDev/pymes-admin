package core_pymes.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String codigo,
        String message,
        String path,
        int status,
        Map<String, String> details
) {
    public ErrorResponse(String codigo, String message, String path, int status) {
        this(codigo, message, path, status, null);
    }
}

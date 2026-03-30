package auth.pymes.common.models.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    String name,

    @NotBlank(message = "El slug (identificador URL) es obligatorio")
    @Size(min = 3, max = 50, message = "El slug debe tener entre 3 y 50 caracteres")
    String slug,

    String industry
) {}

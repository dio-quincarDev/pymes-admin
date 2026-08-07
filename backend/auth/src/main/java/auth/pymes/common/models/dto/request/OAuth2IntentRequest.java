package auth.pymes.common.models.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record OAuth2IntentRequest(
        @NotBlank(message = "Company name is required")
        @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
        String companyName,

        @NotBlank(message = "Company slug is required")
        @Size(min = 3, max = 50, message = "Slug must be between 3 and 50 characters")
        String companySlug
) {}

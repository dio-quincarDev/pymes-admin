package auth.pymes.common.models.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OAuth2ExchangeRequest(
        @NotBlank(message = "Exchange code is required")
        @Size(min = 1, max = 128, message = "Exchange code must be between 1 and 128 characters")
        String code
) {}
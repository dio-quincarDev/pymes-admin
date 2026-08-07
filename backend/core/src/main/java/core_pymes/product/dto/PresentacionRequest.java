package core_pymes.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record PresentacionRequest(
        @NotBlank String name,
        @Positive int conversion
) {}

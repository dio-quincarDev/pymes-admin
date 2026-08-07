package core_pymes.costos.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConfigLaboralRequest(
        @NotNull @Min(1) @Max(31) Integer diasLaborales
) {}

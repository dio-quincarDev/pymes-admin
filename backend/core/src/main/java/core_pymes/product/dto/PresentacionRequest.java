package core_pymes.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PresentacionRequest(
        @NotBlank String name,
        // ponytail: conversion > 1 siempre; x1 es fila suelta, no presentacion
        @NotNull @DecimalMin(value = "1", inclusive = false) BigDecimal conversion
) {}

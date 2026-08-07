package core_pymes.prestamo.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PrestamoCreadoEvent(
        java.util.UUID tenantId,
        LocalDate fecha,
        BigDecimal monto
) {}

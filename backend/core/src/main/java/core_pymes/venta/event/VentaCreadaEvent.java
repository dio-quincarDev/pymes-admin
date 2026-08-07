package core_pymes.venta.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VentaCreadaEvent(
        java.util.UUID tenantId,
        LocalDate fecha,
        BigDecimal monto
) {}

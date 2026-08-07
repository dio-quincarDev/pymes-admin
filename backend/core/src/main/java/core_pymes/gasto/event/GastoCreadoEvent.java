package core_pymes.gasto.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GastoCreadoEvent(
        java.util.UUID tenantId,
        LocalDate fecha,
        BigDecimal monto
) {}

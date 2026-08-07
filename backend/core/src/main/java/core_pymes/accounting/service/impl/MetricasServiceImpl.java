package core_pymes.accounting.service.impl;

import core_pymes.accounting.domain.MetricasFinanciera;
import core_pymes.accounting.dto.MetricasResponse;
import core_pymes.accounting.repository.MetricasRepository;
import core_pymes.accounting.service.MetricasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricasServiceImpl implements MetricasService {

    private final MetricasRepository repository;
    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public MetricasFinanciera recalcular(UUID tenantId, String periodo) {
        var ym = YearMonth.parse(periodo);
        var start = ym.atDay(1);
        var end = ym.plusMonths(1).atDay(1);

        var metrics = computeMetrics(tenantId, start, end);

        var metricas = repository.findByTenantIdAndPeriod(tenantId, periodo)
                .orElse(MetricasFinanciera.builder()
                        .tenantId(tenantId)
                        .period(periodo)
                        .build());

        metricas.setTotalIncome(metrics.totalIncome);
        metricas.setCostOfGoods(metrics.costOfGoods);
        metricas.setOperatingExpenses(metrics.operatingExpenses);
        metricas.setLoanPayments(metrics.loanPayments);
        metricas.setTotalExpenses(metrics.totalExpenses);
        metricas.setGrossMargin(metrics.grossMargin);
        metricas.setGrossMarginPct(metrics.grossMarginPct);
        metricas.setOperatingMargin(metrics.operatingMargin);
        metricas.setOperatingMarginPct(metrics.operatingMarginPct);
        metricas.setNetMargin(metrics.netMargin);
        metricas.setNetMarginPct(metrics.netMarginPct);
        metricas.setCostoOperativoDiario(metrics.costoOperativoDiario);

        metricas = repository.save(metricas);
        log.debug("Metrics computed for tenant {} period {}: net margin {}%",
                tenantId, periodo, metrics.netMarginPct);
        return metricas;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MetricasResponse> consultar(UUID tenantId, String periodo) {
        if (periodo == null) periodo = YearMonth.now(ZoneOffset.UTC).toString();
        return repository.findByTenantIdAndPeriod(tenantId, periodo)
                .map(this::toResponse);
    }

    // -- Single consolidated query: 1 round-trip instead of 4 --

    private record MetricsResult(
            BigDecimal totalIncome, BigDecimal costOfGoods,
            BigDecimal operatingExpenses, BigDecimal loanPayments,
            BigDecimal totalExpenses, BigDecimal grossMargin, BigDecimal grossMarginPct,
            BigDecimal operatingMargin, BigDecimal operatingMarginPct,
            BigDecimal netMargin, BigDecimal netMarginPct, BigDecimal costoOperativoDiario) {}

    private MetricsResult computeMetrics(UUID tenantId, LocalDate start, LocalDate end) {
        var sql = """
                WITH sales AS (
                    SELECT COALESCE(SUM(gross_amount), 0) AS total
                    FROM core.daily_sales
                    WHERE tenant_id = ? AND sale_date >= ? AND sale_date < ?
                ),
                invoices_cost AS (
                    SELECT COALESCE(SUM(total), 0) AS total
                    FROM core.invoices
                    WHERE tenant_id = ? AND issue_date >= ? AND issue_date < ? AND type = 'FACTURA'
                ),
                invoices_opex AS (
                    SELECT COALESCE(SUM(total), 0) AS total
                    FROM core.invoices
                    WHERE tenant_id = ? AND issue_date >= ? AND issue_date < ?
                      AND type = 'GASTO_OPERATIVO' AND status = 'PAGADA'
                ),
                loan_pay AS (
                    SELECT COALESCE(SUM(lp.amount), 0) AS total
                    FROM core.loan_payments lp
                    JOIN core.loans l ON lp.loan_id = l.id
                    WHERE l.tenant_id = ? AND lp.payment_date >= ? AND lp.payment_date < ?
                ),
                costos AS (
                    SELECT
                        COALESCE((SELECT SUM(monto) FROM core.gastos_fijos_recurrentes
                                  WHERE tenant_id = ? AND activo = true), 0) AS costo_fijo_mensual,
                        COALESCE((SELECT SUM(
                            CASE tipo_pago
                                WHEN 'DIARIO' THEN monto * (SELECT COALESCE(dias_laborales, 26) FROM core.config_laboral WHERE tenant_id = ?)
                                WHEN 'SEMANAL' THEN monto * 4.33
                                WHEN 'QUINCENAL' THEN monto * 2
                                WHEN 'MENSUAL' THEN monto
                                ELSE monto
                            END
                        ) FROM core.collaboradores WHERE tenant_id = ? AND activo = true), 0) AS costo_salarios_mensual,
                        COALESCE((SELECT dias_laborales FROM core.config_laboral WHERE tenant_id = ?), 26) AS dias_laborales
                )
                SELECT s.total AS total_income,
                       ic.total AS cost_of_goods,
                       io.total AS operating_expenses,
                       l.total AS loan_payments,
                       (c.costo_fijo_mensual + c.costo_salarios_mensual) / c.dias_laborales AS costo_operativo_diario
                FROM sales s, invoices_cost ic, invoices_opex io, loan_pay l, costos c
                """;

        return jdbc.query(sql, rs -> {
            rs.next();
            var totalIncome = rs.getBigDecimal("total_income");
            var costOfGoods = rs.getBigDecimal("cost_of_goods");
            var opEx = rs.getBigDecimal("operating_expenses");
            var loanPay = rs.getBigDecimal("loan_payments");
            var costoOperativoDiario = rs.getBigDecimal("costo_operativo_diario");
            var totalExpenses = costOfGoods.add(opEx).add(loanPay);

            var grossMargin = totalIncome.subtract(costOfGoods);
            var grossMarginPct = safePct(grossMargin, totalIncome);
            var operatingMargin = grossMargin.subtract(opEx);
            var operatingMarginPct = safePct(operatingMargin, totalIncome);
            var netMargin = operatingMargin.subtract(loanPay);
            var netMarginPct = safePct(netMargin, totalIncome);

            return new MetricsResult(totalIncome, costOfGoods, opEx, loanPay,
                    totalExpenses, grossMargin, grossMarginPct,
                    operatingMargin, operatingMarginPct, netMargin, netMarginPct,
                    costoOperativoDiario);
        },
                tenantId, start, end,   // sales
                tenantId, start, end,   // invoices_cost
                tenantId, start, end,   // invoices_opex
                tenantId, start, end,   // loan_pay
                tenantId, tenantId, tenantId, tenantId);  // costos
    }

    private BigDecimal safePct(BigDecimal margin, BigDecimal base) {
        if (base == null || base.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return margin.multiply(BigDecimal.valueOf(100))
                .divide(base, 4, RoundingMode.HALF_UP);
    }

    private MetricasResponse toResponse(MetricasFinanciera m) {
        return new MetricasResponse(
                m.getId(), m.getTenantId(), m.getPeriod(),
                m.getTotalIncome(), m.getCostOfGoods(),
                m.getOperatingExpenses(), m.getLoanPayments(),
                m.getTotalExpenses(), m.getGrossMargin(), m.getGrossMarginPct(),
                m.getOperatingMargin(), m.getOperatingMarginPct(),
                m.getNetMargin(), m.getNetMarginPct(),
                m.getCostoOperativoDiario(), m.getCreatedAt());
    }
}

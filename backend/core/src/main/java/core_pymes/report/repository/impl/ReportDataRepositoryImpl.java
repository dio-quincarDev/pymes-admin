package core_pymes.report.repository.impl;

import core_pymes.common.exception.custom.InvalidInputException;
import core_pymes.report.dto.ReportData;
import core_pymes.report.dto.ReportOwner;
import core_pymes.report.repository.ReportDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ReportDataRepositoryImpl implements ReportDataRepository {

    private final JdbcTemplate jdbc;

    @Override
    public List<ReportOwner> findOwners() {
        // ponytail: cross-schema read (auth), mover a Feign si auth se separa de DB
        return jdbc.query(
                """
                SELECT ut.tenant_id, u.email, t.name, t.timezone, t.currency
                FROM auth.user_tenants ut
                JOIN auth.users u ON u.id = ut.user_id
                JOIN auth.tenants t ON t.id = ut.tenant_id
                WHERE ut.role = 'OWNER' AND ut.is_active
                  AND u.is_active AND u.deleted_at IS NULL
                  AND t.is_active AND t.deleted_at IS NULL
                """,
                (rs, row) -> new ReportOwner(
                        rs.getObject("tenant_id", UUID.class),
                        rs.getString("email"),
                        rs.getString("name"),
                        rs.getString("timezone"),
                        rs.getString("currency")));
    }

    @Override
    public ReportData loadMonthData(UUID tenantId, String periodo) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(periodo);
        } catch (DateTimeParseException e) {
            throw new InvalidInputException("Invalid period, expected YYYY-MM: " + periodo);
        }
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.plusMonths(1).atDay(1);

        Map<String, Object> ventas = jdbc.queryForMap(
                """
                SELECT COALESCE(SUM(gross_amount), 0) AS total,
                       COUNT(*) AS n,
                       COUNT(DISTINCT sale_date) AS dias
                FROM core.daily_sales
                WHERE tenant_id = ? AND is_active
                  AND sale_date >= ? AND sale_date < ?
                """,
                tenantId, from, to);
        Map<String, Object> compras = sumInvoices(tenantId, from, to, "FACTURA");
        Map<String, Object> opex = sumInvoices(tenantId, from, to, "GASTO_OPERATIVO");

        BigDecimal ingresos = (BigDecimal) ventas.get("total");
        BigDecimal costoCompras = (BigDecimal) compras.get("total");
        BigDecimal gastosOper = (BigDecimal) opex.get("total");

        List<ReportData.Semana> semanas = jdbc.query(
                """
                SELECT CASE WHEN EXTRACT(DAY FROM sale_date) <= 7 THEN '1-7'
                            WHEN EXTRACT(DAY FROM sale_date) <= 14 THEN '8-14'
                            WHEN EXTRACT(DAY FROM sale_date) <= 21 THEN '15-21'
                            ELSE '22-fin' END AS rango,
                       COALESCE(SUM(gross_amount), 0) AS total
                FROM core.daily_sales
                WHERE tenant_id = ? AND is_active
                  AND sale_date >= ? AND sale_date < ?
                GROUP BY rango ORDER BY MIN(sale_date)
                """,
                (rs, row) -> new ReportData.Semana(rs.getString("rango"), rs.getBigDecimal("total")),
                tenantId, from, to);

        List<ReportData.TopProveedor> top = jdbc.query(
                """
                SELECT COALESCE(p.name, 'Sin proveedor') AS nombre,
                       SUM(i.total) AS total, COUNT(*) AS facturas
                FROM core.invoices i
                LEFT JOIN core.providers p ON p.id = i.provider_id
                WHERE i.tenant_id = ? AND i.type = 'FACTURA' AND i.status = 'PAGADA'
                  AND i.is_active AND i.issue_date >= ? AND i.issue_date < ?
                GROUP BY nombre ORDER BY total DESC LIMIT 5
                """,
                (rs, row) -> new ReportData.TopProveedor(
                        rs.getString("nombre"), rs.getBigDecimal("total"), rs.getLong("facturas")),
                tenantId, from, to);

        return new ReportData(
                ym.toString(), ingresos, costoCompras, gastosOper,
                ingresos.subtract(costoCompras).subtract(gastosOper),
                ((Number) ventas.get("dias")).intValue(), ym.lengthOfMonth(),
                ((Number) ventas.get("n")).longValue(), ((Number) compras.get("n")).longValue(),
                semanas, top);
    }

    private Map<String, Object> sumInvoices(UUID tenantId, LocalDate from, LocalDate to, String type) {
        return jdbc.queryForMap(
                """
                SELECT COALESCE(SUM(total), 0) AS total, COUNT(*) AS n
                FROM core.invoices
                WHERE tenant_id = ? AND type = ? AND status = 'PAGADA'
                  AND is_active AND issue_date >= ? AND issue_date < ?
                """,
                tenantId, type, from, to);
    }

    @Override
    public boolean claim(UUID tenantId, String periodo, String format) {
        // ponytail: FAILED se retoma (reintento), SENT/SENDING/SKIPPED bloquean; sin contador extra (YAGNI)
        return jdbc.update(
                "INSERT INTO core.report_log(tenant_id, period, format, status) VALUES (?, ?, ?, 'SENDING') ON CONFLICT (tenant_id, period, format) DO UPDATE SET status = 'SENDING', error_msg = NULL WHERE report_log.status = 'FAILED'",
                tenantId, periodo, format) == 1;
    }

    @Override
    public void updateStatus(UUID tenantId, String periodo, String format, String status, String errorMsg) {
        jdbc.update(
                "UPDATE core.report_log SET status = ?, error_msg = ? WHERE tenant_id = ? AND period = ? AND format = ?",
                status, errorMsg, tenantId, periodo, format);
    }
}

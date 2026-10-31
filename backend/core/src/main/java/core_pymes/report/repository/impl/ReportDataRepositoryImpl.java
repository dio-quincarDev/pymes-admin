package core_pymes.report.repository.impl;

import core_pymes.common.exception.custom.InvalidInputException;
import core_pymes.report.dto.QuarterlyReportData;
import core_pymes.report.dto.ReportData;
import core_pymes.report.dto.ReportOwner;
import core_pymes.report.repository.ReportDataRepository;
import core_pymes.report.support.ReportLabels;
import core_pymes.report.support.RetornoInversion;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
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

        List<ReportData.TopProducto> productos = jdbc.query(
                """
                SELECT COALESCE(p.name, MAX(ii.product_name)) AS nombre,
                       SUM(ii.quantity * ii.conversion_factor) AS cantidad,
                       COALESCE((SELECT tu.name FROM core.template_units tu
                                 WHERE tu.id::text = MAX(p.base_unit) LIMIT 1), 'Unidad') AS unidad,
                       SUM(ii.subtotal) AS total
                FROM core.invoice_items ii
                JOIN core.invoices i ON i.id = ii.invoice_id
                LEFT JOIN core.products p ON p.id = ii.product_id
                WHERE i.tenant_id = ? AND i.type = 'FACTURA' AND i.status = 'PAGADA'
                  AND i.is_active AND i.issue_date >= ? AND i.issue_date < ?
                GROUP BY p.id, p.name, p.base_unit
                ORDER BY cantidad DESC, total DESC LIMIT 5
                """,
                (rs, row) -> new ReportData.TopProducto(
                        rs.getString("nombre"), rs.getBigDecimal("cantidad"),
                        rs.getString("unidad"), rs.getBigDecimal("total")),
                tenantId, from, to);

        List<ReportData.TopGasto> gastos = jdbc.query(
                """
                SELECT COALESCE(NULLIF(i.category, ''), 'Sin categoría') AS nombre,
                       SUM(i.total) AS total, COUNT(*) AS facturas
                FROM core.invoices i
                WHERE i.tenant_id = ? AND i.type = 'GASTO_OPERATIVO' AND i.status = 'PAGADA'
                  AND i.is_active AND i.issue_date >= ? AND i.issue_date < ?
                GROUP BY nombre ORDER BY total DESC LIMIT 3
                """,
                (rs, row) -> new ReportData.TopGasto(
                        rs.getString("nombre"), rs.getBigDecimal("total"), rs.getLong("facturas")),
                tenantId, from, to);

        List<ReportData.MetodoTotal> ingresosPorMetodo = jdbc.query(
                """
                SELECT dsp.metodo, SUM(dsp.monto) AS total
                FROM core.daily_sale_payments dsp
                JOIN core.daily_sales ds ON ds.id = dsp.sale_id
                WHERE ds.tenant_id = ? AND ds.is_active
                  AND ds.sale_date >= ? AND ds.sale_date < ?
                GROUP BY dsp.metodo ORDER BY total DESC
                """,
                (rs, row) -> new ReportData.MetodoTotal(
                        rs.getString("metodo"), rs.getBigDecimal("total")),
                tenantId, from, to);

        List<ReportData.MetodoTotal> egresosPorMetodo = jdbc.query(
                """
                SELECT COALESCE(NULLIF(payment_method, ''), 'Sin método') AS metodo,
                       SUM(total) AS total
                FROM core.invoices
                WHERE tenant_id = ? AND status = 'PAGADA' AND is_active
                  AND issue_date >= ? AND issue_date < ?
                GROUP BY metodo ORDER BY total DESC
                """,
                (rs, row) -> new ReportData.MetodoTotal(
                        rs.getString("metodo"), rs.getBigDecimal("total")),
                tenantId, from, to);

        BigDecimal capital;
        try {
            capital = jdbc.queryForObject(
                    "SELECT initial_capital FROM core.patrimony WHERE tenant_id = ?",
                    BigDecimal.class, tenantId);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            capital = null; // ponytail: sin capital registrado -> el bloque de retorno no aparece
        }

        return new ReportData(
                ym.toString(), ingresos, costoCompras, gastosOper,
                ingresos.subtract(costoCompras).subtract(gastosOper),
                ((Number) ventas.get("dias")).intValue(), ym.lengthOfMonth(),
                ((Number) ventas.get("n")).longValue(), ((Number) compras.get("n")).longValue(),
                semanas, top, productos, gastos, ingresosPorMetodo, egresosPorMetodo, capital);
    }

    @Override
    public QuarterlyReportData loadQuarterData(UUID tenantId, YearMonth fin) {
        YearMonth inicio = fin.minusMonths(2);
        LocalDate desde = inicio.atDay(1);
        LocalDate hasta = fin.plusMonths(1).atDay(1);

        Map<YearMonth, BigDecimal> ventasMes = new HashMap<>();
        Map<YearMonth, Long> ventasN = new HashMap<>();
        Map<YearMonth, Integer> diasMes = new HashMap<>();
        jdbc.query(
                """
                SELECT date_trunc('month', sale_date)::date AS mes,
                       COALESCE(SUM(gross_amount), 0) AS total,
                       COUNT(*) AS n,
                       COUNT(DISTINCT sale_date) AS dias
                FROM core.daily_sales
                WHERE tenant_id = ? AND is_active
                  AND sale_date >= ? AND sale_date < ?
                GROUP BY mes ORDER BY mes
                """,
                rs -> {
                    YearMonth ym = YearMonth.from(rs.getDate("mes").toLocalDate());
                    ventasMes.put(ym, rs.getBigDecimal("total"));
                    ventasN.put(ym, rs.getLong("n"));
                    diasMes.put(ym, rs.getInt("dias"));
                },
                tenantId, desde, hasta);

        Map<YearMonth, MesFacturas> comprasMes = new HashMap<>();
        Map<YearMonth, MesFacturas> gastosMes = new HashMap<>();
        jdbc.query(
                """
                SELECT date_trunc('month', issue_date)::date AS mes, type,
                       COALESCE(SUM(total), 0) AS total,
                       COUNT(*) AS n,
                       COALESCE(SUM(itbms_total), 0) AS itbms
                FROM core.invoices
                WHERE tenant_id = ? AND status = 'PAGADA' AND is_active
                  AND issue_date >= ? AND issue_date < ?
                GROUP BY mes, type ORDER BY mes
                """,
                rs -> {
                    YearMonth ym = YearMonth.from(rs.getDate("mes").toLocalDate());
                    MesFacturas valor = new MesFacturas(rs.getBigDecimal("total"),
                            rs.getLong("n"), rs.getBigDecimal("itbms"));
                    if ("FACTURA".equals(rs.getString("type"))) {
                        comprasMes.put(ym, valor);
                    } else if ("GASTO_OPERATIVO".equals(rs.getString("type"))) {
                        gastosMes.put(ym, valor);
                    }
                },
                tenantId, desde, hasta);

        List<ReportData.TopProveedor> topProveedores = jdbc.query(
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
                tenantId, desde, hasta);

        List<ReportData.TopProducto> topProductos = jdbc.query(
                """
                SELECT COALESCE(p.name, MAX(ii.product_name)) AS nombre,
                       SUM(ii.quantity * ii.conversion_factor) AS cantidad,
                       COALESCE((SELECT tu.name FROM core.template_units tu
                                 WHERE tu.id::text = MAX(p.base_unit) LIMIT 1), 'Unidad') AS unidad,
                       SUM(ii.subtotal) AS total
                FROM core.invoice_items ii
                JOIN core.invoices i ON i.id = ii.invoice_id
                LEFT JOIN core.products p ON p.id = ii.product_id
                WHERE i.tenant_id = ? AND i.type = 'FACTURA' AND i.status = 'PAGADA'
                  AND i.is_active AND i.issue_date >= ? AND i.issue_date < ?
                GROUP BY p.id, p.name, p.base_unit
                ORDER BY cantidad DESC, total DESC LIMIT 5
                """,
                (rs, row) -> new ReportData.TopProducto(
                        rs.getString("nombre"), rs.getBigDecimal("cantidad"),
                        rs.getString("unidad"), rs.getBigDecimal("total")),
                tenantId, desde, hasta);

        BigDecimal ingresos = suma(ventasMes);
        BigDecimal compras = sumaFacturas(comprasMes);
        BigDecimal gastos = sumaFacturas(gastosMes);
        BigDecimal itbms = comprasMes.values().stream()
                .map(MesFacturas::itbms).reduce(BigDecimal.ZERO, BigDecimal::add);
        long ventasCount = ventasN.values().stream().mapToLong(Long::longValue).sum();
        long facturasCount = comprasMes.values().stream().mapToLong(MesFacturas::n).sum()
                + gastosMes.values().stream().mapToLong(MesFacturas::n).sum();
        int diasCargados = diasMes.values().stream().mapToInt(Integer::intValue).sum();

        List<MesBase> base = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            YearMonth ym = inicio.plusMonths(i);
            BigDecimal ing = ventasMes.getOrDefault(ym, BigDecimal.ZERO);
            BigDecimal compra = valor(comprasMes.get(ym));
            BigDecimal gasto = valor(gastosMes.get(ym));
            long nVentas = ventasN.getOrDefault(ym, 0L);
            long nFacturas = (comprasMes.containsKey(ym) ? comprasMes.get(ym).n() : 0)
                    + (gastosMes.containsKey(ym) ? gastosMes.get(ym).n() : 0);
            base.add(new MesBase(ReportLabels.mesCorto(ym.toString()), ing, compra, gasto,
                    nVentas == 0 && nFacturas == 0));
        }
        BigDecimal maximo = base.stream()
                .flatMap(m -> java.util.stream.Stream.of(m.ingresos(), m.compras().add(m.gastos())))
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        List<QuarterlyReportData.Mes> meses = new ArrayList<>();
        for (MesBase m : base) {
            BigDecimal salidas = m.compras().add(m.gastos());
            meses.add(new QuarterlyReportData.Mes(m.etiqueta(), m.ingresos(), m.compras(),
                    m.gastos(), barra(m.ingresos(), maximo), barra(salidas, maximo), m.sinDatos()));
        }

        LocalDate prevDesde = desde.minusMonths(3);
        Map<String, Object> prevVentas = jdbc.queryForMap(
                """
                SELECT COALESCE(SUM(gross_amount), 0) AS total, COUNT(*) AS n
                FROM core.daily_sales
                WHERE tenant_id = ? AND is_active
                  AND sale_date >= ? AND sale_date < ?
                """,
                tenantId, prevDesde, desde);
        Map<String, Object> prevCompras = sumInvoices(tenantId, prevDesde, desde, "FACTURA");
        Map<String, Object> prevGastos = sumInvoices(tenantId, prevDesde, desde, "GASTO_OPERATIVO");
        long prevRegistros = ((Number) prevVentas.get("n")).longValue()
                + ((Number) prevCompras.get("n")).longValue()
                + ((Number) prevGastos.get("n")).longValue();
        BigDecimal prevIngresos = (BigDecimal) prevVentas.get("total");

        List<ReportData.MetodoTotal> ingresosPorMetodo = jdbc.query(
                """
                SELECT dsp.metodo, SUM(dsp.monto) AS total
                FROM core.daily_sale_payments dsp
                JOIN core.daily_sales ds ON ds.id = dsp.sale_id
                WHERE ds.tenant_id = ? AND ds.is_active
                  AND ds.sale_date >= ? AND ds.sale_date < ?
                GROUP BY dsp.metodo ORDER BY total DESC
                """,
                (rs, row) -> new ReportData.MetodoTotal(
                        rs.getString("metodo"), rs.getBigDecimal("total")),
                tenantId, desde, hasta);

        List<ReportData.MetodoTotal> egresosPorMetodo = jdbc.query(
                """
                SELECT COALESCE(NULLIF(payment_method, ''), 'Sin método') AS metodo,
                       SUM(total) AS total
                FROM core.invoices
                WHERE tenant_id = ? AND status = 'PAGADA' AND is_active
                  AND issue_date >= ? AND issue_date < ?
                GROUP BY metodo ORDER BY total DESC
                """,
                (rs, row) -> new ReportData.MetodoTotal(
                        rs.getString("metodo"), rs.getBigDecimal("total")),
                tenantId, desde, hasta);

        BigDecimal capital;
        try {
            capital = jdbc.queryForObject(
                    "SELECT initial_capital FROM core.patrimony WHERE tenant_id = ?",
                    BigDecimal.class, tenantId);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            capital = null;
        }
        BigDecimal recuperado = recuperar(capital, tenantId, fin);

        String periodo = "T" + fin.toString().replace("-", "");
        return new QuarterlyReportData(periodo, ingresos, compras, gastos,
                ingresos.subtract(compras).subtract(gastos), itbms,
                ingresosPorMetodo, egresosPorMetodo,
                diasCargados,
                ventasCount, facturasCount, meses, topProductos, topProveedores,
                List.of(
                        new QuarterlyReportData.Comparativa("Ingresos", ingresos, prevIngresos),
                        new QuarterlyReportData.Comparativa("Compras", compras, (BigDecimal) prevCompras.get("total")),
                        new QuarterlyReportData.Comparativa("Gastos", gastos, (BigDecimal) prevGastos.get("total"))),
                prevRegistros > 0, capital, recuperado);
    }

    @Override
    public boolean isQuarterDue(UUID tenantId, YearMonth fin) {
        LocalDate primera = jdbc.queryForObject(
                """
                SELECT LEAST(
                  (SELECT MIN(sale_date) FROM core.daily_sales WHERE tenant_id = ? AND is_active),
                  (SELECT MIN(issue_date) FROM core.invoices WHERE tenant_id = ? AND is_active)
                )
                """,
                LocalDate.class, tenantId, tenantId);
        if (primera == null) {
            return false;
        }
        YearMonth inicio = YearMonth.from(primera);
        long meses = ChronoUnit.MONTHS.between(inicio, fin);
        return meses >= 2 && meses % 3 == 2;
    }

    private BigDecimal recuperar(BigDecimal capital, UUID tenantId, YearMonth fin) {
        if (capital == null || capital.signum() <= 0) {
            return null;
        }
        List<Map<String, Object>> historial = jdbc.queryForList(
                """
                SELECT total_income, net_margin
                FROM core.tenant_financial_metrics
                WHERE tenant_id = ? AND period <= ?
                ORDER BY period
                """,
                tenantId, fin.toString());
        BigDecimal recuperado = BigDecimal.ZERO;
        for (Map<String, Object> fila : historial) {
            BigDecimal ingresos = (BigDecimal) fila.get("total_income");
            BigDecimal neto = (BigDecimal) fila.get("net_margin");
            recuperado = recuperado.add(RetornoInversion.aporte(capital, neto, ingresos));
        }
        return recuperado.min(capital);
    }

    private static BigDecimal valor(MesFacturas mes) {
        return mes == null ? BigDecimal.ZERO : mes.total();
    }

    private static BigDecimal suma(Map<YearMonth, BigDecimal> valores) {
        return valores.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumaFacturas(Map<YearMonth, MesFacturas> valores) {
        return valores.values().stream().map(MesFacturas::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Barra textual de 22 bloques; DejaVu Sans tiene el glifo. */
    private static String barra(BigDecimal valor, BigDecimal maximo) {
        if (valor == null || valor.signum() <= 0 || maximo == null || maximo.signum() <= 0) {
            return "";
        }
        int bloques = valor.multiply(BigDecimal.valueOf(22))
                .divide(maximo, 0, RoundingMode.HALF_UP).intValue();
        return "\u2588".repeat(Math.max(1, Math.min(22, bloques)));
    }

    private record MesFacturas(BigDecimal total, long n, BigDecimal itbms) {
    }

    private record MesBase(String etiqueta, BigDecimal ingresos, BigDecimal compras,
            BigDecimal gastos, boolean sinDatos) {
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

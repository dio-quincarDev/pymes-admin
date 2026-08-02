package core_pymes.analytics.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import core_pymes.accounting.domain.MetricasFinanciera;
import core_pymes.accounting.repository.MetricasRepository;
import core_pymes.accounting.service.MetricasService;
import core_pymes.analytics.domain.AnalisisGasto;
import core_pymes.analytics.repository.AnalisisGastoRepository;
import core_pymes.analytics.service.AnalyticsService;
import core_pymes.inversion.repository.PatrimonioRepository;
import core_pymes.product.domain.Producto;
import core_pymes.product.repository.ProductoRepository;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final JdbcTemplate jdbc;
    private final AnalisisGastoRepository repository;
    private final ObjectMapper objectMapper;
    private final MetricasService metricasService;
    private final MetricasRepository metricasRepository;
    private final ProductoRepository productoRepository;
    private final PatrimonioRepository patrimonioRepository;

    @Override
    @Transactional
    public AnalisisGasto ejecutarCompleto(UUID tenantId, String periodo) {
        var ym = YearMonth.parse(periodo);
        var start = ym.atDay(1);
        var end = ym.plusMonths(1).atDay(1);

        var analisis = repository.findByTenantIdAndPeriod(tenantId, periodo)
                .orElse(AnalisisGasto.builder()
                        .tenantId(tenantId)
                        .period(periodo)
                        .build());

        var abc = analisisABC(tenantId, start, end);
        var trend = analisisTendencia(tenantId, start, end);
        var margin = analisisMargen(tenantId, start, end);
        var opex = analisisGastoVariable(tenantId, start, end, periodo);
        var projection = analisisProyeccion(tenantId, start, end);
        var alerts = analisisAlertas(tenantId, start, end);
        var comparativa = analisisComparativaProveedores(tenantId, start, end);
        var recomendaciones = analisisRecomendacionProveedor(tenantId, start, end);
        var predicciones = analisisProyeccionPrecios(tenantId, start, end);

        analisis.setAbc(toJson(abc));
        analisis.setTrend(toJson(trend));
        analisis.setMargin(toJson(margin));
        analisis.setOpexPct(toJson(opex));
        analisis.setProjection(toJson(projection));
        analisis.setAlerts(toJson(alerts));
        analisis.setSupplierComparison(toJson(comparativa));
        analisis.setSupplierRecommendations(toJson(recomendaciones));
        analisis.setPricePrediction(toJson(predicciones));
        analisis.setFinancialHealth(toJson(analisisSaludFinanciera(tenantId, periodo, start, end, abc, comparativa, alerts)));

        repository.save(analisis);
        log.debug("Analytics computed for tenant {} period {}", tenantId, periodo);
        return analisis;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalisisGasto> consultar(UUID tenantId, String periodo) {
        if (periodo == null) periodo = YearMonth.now(ZoneOffset.UTC).toString();
        return repository.findByTenantIdAndPeriod(tenantId, periodo);
    }

    // -- 6 motores analíticos --

    List<Map<String, Object>> analisisABC(UUID tenantId, LocalDate start, LocalDate end) {
        var sql = """
                WITH product_spend AS (
                    SELECT ii.product_id, p.name AS product_name,
                           SUM(ii.subtotal) AS total_spend
                    FROM core.invoice_items ii
                    JOIN core.invoices i ON ii.invoice_id = i.id
                    JOIN core.products p ON ii.product_id = p.id
                    WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
                    GROUP BY ii.product_id, p.name
                ), ranked AS (
                    SELECT *, SUM(total_spend) OVER (ORDER BY total_spend DESC) AS running_total,
                           SUM(total_spend) OVER () AS grand_total
                    FROM product_spend
                )
                SELECT product_id, product_name, total_spend,
                       CASE WHEN grand_total > 0
                            THEN ROUND(total_spend * 100.0 / grand_total, 2)
                            ELSE 0 END AS pct,
                       CASE
                           WHEN grand_total > 0 AND SUM(total_spend) OVER (ORDER BY total_spend DESC) / grand_total <= 0.80 THEN 'A'
                           WHEN grand_total > 0 AND SUM(total_spend) OVER (ORDER BY total_spend DESC) / grand_total <= 0.95 THEN 'B'
                           ELSE 'C'
                       END AS category
                FROM ranked
                ORDER BY total_spend DESC
                """;
        return jdbc.query(sql, (rs, row) -> Map.<String, Object>of(
                "productId", rs.getObject("product_id").toString(),
                "productName", rs.getString("product_name"),
                "totalSpend", rs.getBigDecimal("total_spend"),
                "pct", rs.getBigDecimal("pct"),
                "category", rs.getString("category")
        ), tenantId, start, end);
    }

    List<Map<String, Object>> analisisTendencia(UUID tenantId, LocalDate start, LocalDate end) {
        var ninetyDaysAgo = start.minusDays(90);
        var sql = """
                WITH current_prices AS (
                    SELECT ii.product_id, p.name AS product_name,
                           AVG(ii.unit_price / NULLIF(ii.conversion_factor, 0)) AS current_avg_price
                    FROM core.invoice_items ii
                    JOIN core.invoices i ON ii.invoice_id = i.id
                    JOIN core.products p ON ii.product_id = p.id
                    WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
                    GROUP BY ii.product_id, p.name
                ), moving_avg AS (
                    SELECT ii.product_id,
                           AVG(ii.unit_price / NULLIF(ii.conversion_factor, 0)) AS moving_avg_90d
                    FROM core.invoice_items ii
                    JOIN core.invoices i ON ii.invoice_id = i.id
                    WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
                    GROUP BY ii.product_id
                )
                SELECT cp.product_id, cp.product_name,
                       ROUND(cp.current_avg_price, 4) AS current_avg_price,
                       COALESCE(ROUND(ma.moving_avg_90d, 4), 0) AS moving_avg_90d
                FROM current_prices cp
                LEFT JOIN moving_avg ma ON cp.product_id = ma.product_id
                ORDER BY cp.product_name
                """;
        return jdbc.query(sql, (rs, row) -> {
            var current = rs.getBigDecimal("current_avg_price");
            var moving = rs.getBigDecimal("moving_avg_90d");
            BigDecimal change = moving.compareTo(BigDecimal.ZERO) > 0
                    ? current.subtract(moving).multiply(BigDecimal.valueOf(100)).divide(moving, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            return Map.<String, Object>of(
                    "productId", rs.getObject("product_id").toString(),
                    "productName", rs.getString("product_name"),
                    "currentAvgPrice", current,
                    "movingAvg90d", moving,
                    "pctChange", change
            );
        }, tenantId, start, end, tenantId, ninetyDaysAgo, start);
    }

    List<Map<String, Object>> analisisMargen(UUID tenantId, LocalDate start, LocalDate end) {
        var prevStart = start.minusMonths(1);
        var prevEnd = start;
        var sql = """
                WITH current_prices AS (
                    SELECT ii.product_id, p.name AS product_name,
                           AVG(ii.unit_price / NULLIF(ii.conversion_factor, 0)) AS avg_price
                    FROM core.invoice_items ii
                    JOIN core.invoices i ON ii.invoice_id = i.id
                    JOIN core.products p ON ii.product_id = p.id
                    WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
                    GROUP BY ii.product_id, p.name
                ), previous_prices AS (
                    SELECT ii.product_id,
                           AVG(ii.unit_price / NULLIF(ii.conversion_factor, 0)) AS avg_price
                    FROM core.invoice_items ii
                    JOIN core.invoices i ON ii.invoice_id = i.id
                    WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
                    GROUP BY ii.product_id
                )
                SELECT cp.product_id, cp.product_name,
                       cp.avg_price AS current_price,
                       pp.avg_price AS previous_price,
                       CASE WHEN pp.avg_price > 0
                           THEN ROUND((cp.avg_price - pp.avg_price) / pp.avg_price * 100, 2)
                           ELSE 0 END AS pct_change
                FROM current_prices cp
                LEFT JOIN previous_prices pp ON cp.product_id = pp.product_id
                ORDER BY pct_change DESC
                """;
        return jdbc.query(sql, (rs, row) -> {
            var prevPrice = rs.getBigDecimal("previous_price");
            return Map.<String, Object>of(
                    "productId", rs.getObject("product_id").toString(),
                    "productName", rs.getString("product_name"),
                    "currentPrice", rs.getBigDecimal("current_price"),
                    "previousPrice", prevPrice != null ? prevPrice : BigDecimal.ZERO,
                    "pctChange", rs.getBigDecimal("pct_change")
            );
        }, tenantId, start, end, tenantId, prevStart, prevEnd);
    }

    List<Map<String, Object>> analisisGastoVariable(UUID tenantId, LocalDate start, LocalDate end, String period) {
        var sql = """
                WITH items_spend AS (
                    SELECT i.id, i.issue_date, ii.product_id, i.provider_id, ii.subtotal
                    FROM core.invoices i
                    JOIN core.invoice_items ii ON ii.invoice_id = i.id
                    WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
                      AND (i.type = 'FACTURA'
                           OR (i.type = 'GASTO_OPERATIVO' AND i.status = 'PAGADA'))
                ),
                header_spend AS (
                    SELECT i.id, i.issue_date, CAST(NULL AS UUID) AS product_id, i.provider_id, i.total AS subtotal
                    FROM core.invoices i
                    WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
                      AND i.type = 'GASTO_OPERATIVO' AND i.status = 'PAGADA'
                      AND NOT EXISTS (SELECT 1 FROM core.invoice_items ii WHERE ii.invoice_id = i.id)
                ),
                period_data AS (
                    SELECT * FROM items_spend
                    UNION ALL
                    SELECT * FROM header_spend
                )
                SELECT COUNT(DISTINCT id) AS invoice_count,
                       COUNT(DISTINCT product_id) AS product_count,
                       COUNT(DISTINCT provider_id) AS provider_count,
                       COALESCE(SUM(subtotal), 0) AS total_spend,
                       COALESCE((
                           SELECT AVG(daily_total)
                           FROM (SELECT SUM(subtotal) AS daily_total FROM period_data GROUP BY issue_date) d
                       ), 0) AS avg_daily_spend
                FROM period_data
                """;

        // ponytail: query fixedDailyCost directly from DB using JDBC to bypass Hibernate first-level session cache
        var fixedDailyCostSql = """
                SELECT COALESCE(costo_operativo_diario, 0)
                FROM core.tenant_financial_metrics
                WHERE tenant_id = ? AND period = ?
                """;
        BigDecimal fixedDailyCostTemp;
        try {
            fixedDailyCostTemp = jdbc.queryForObject(fixedDailyCostSql, BigDecimal.class, tenantId, period);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // ponytail: force recalculate of metrics if record does not exist
            fixedDailyCostTemp = metricasService.recalcular(tenantId, period).getCostoOperativoDiario();
        } catch (Exception e) {
            fixedDailyCostTemp = BigDecimal.ZERO;
        }
        final BigDecimal fixedDailyCost = fixedDailyCostTemp != null ? fixedDailyCostTemp : BigDecimal.ZERO;

        return jdbc.query(sql, (rs, row) -> {
            var total = rs.getBigDecimal("total_spend");
            var dailyAvg = rs.getBigDecimal("avg_daily_spend");
            var daysInMonth = start.lengthOfMonth();
            return Map.of(
                    "invoiceCount", rs.getInt("invoice_count"),
                    "productCount", rs.getInt("product_count"),
                    "providerCount", rs.getInt("provider_count"),
                    "totalSpend", total,
                    "avgDailySpend", dailyAvg,
                    "variableDailySpend", dailyAvg,
                    "fixedDailyCost", fixedDailyCost,
                    "projectedMonthly", dailyAvg.multiply(BigDecimal.valueOf(daysInMonth))
            );
        }, tenantId, start, end, tenantId, start, end);
    }

    List<Map<String, Object>> analisisProyeccion(UUID tenantId, LocalDate start, LocalDate end) {
        var sql = """
                SELECT COALESCE(SUM(ii.subtotal) / COUNT(DISTINCT i.issue_date), 0) AS avg_daily
                FROM core.invoices i
                JOIN core.invoice_items ii ON ii.invoice_id = i.id
                WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
                """;
        var avgDaily = jdbc.query(sql, rs -> {
            if (rs.next()) return rs.getBigDecimal("avg_daily");
            return BigDecimal.ZERO;
        }, tenantId, start, end);

        return List.of(Map.<String, Object>of(
                "avgDailySpend", avgDaily,
                "projection30d", avgDaily.multiply(BigDecimal.valueOf(30)),
                "projection60d", avgDaily.multiply(BigDecimal.valueOf(60)),
                "projection90d", avgDaily.multiply(BigDecimal.valueOf(90))
        ));
    }

    List<Map<String, Object>> analisisAlertas(UUID tenantId, LocalDate start, LocalDate end) {
        var alerts = new ArrayList<Map<String, Object>>();

        // -- Price variation alerts (existing) --
        var variationSql = """
                WITH stats AS (
                    SELECT ii.product_id, p.name AS product_name,
                           AVG(ii.unit_price / NULLIF(ii.conversion_factor, 0)) AS avg_price,
                           STDDEV(ii.unit_price / NULLIF(ii.conversion_factor, 0)) AS stddev_price,
                           COUNT(*) AS purchases,
                           COUNT(DISTINCT i.provider_id) AS provider_count
                    FROM core.invoice_items ii
                    JOIN core.invoices i ON ii.invoice_id = i.id
                    JOIN core.products p ON ii.product_id = p.id
                    WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
                    GROUP BY ii.product_id, p.name
                )
                SELECT product_id, product_name, avg_price, stddev_price, purchases, provider_count,
                       ROUND(stddev_price / NULLIF(avg_price, 0) * 100, 2) AS cv_pct
                FROM stats
                WHERE stddev_price IS NOT NULL
                  AND (stddev_price / NULLIF(avg_price, 0)) > 0.15
                ORDER BY cv_pct DESC
                """;
        alerts.addAll(jdbc.query(variationSql, (rs, row) -> Map.<String, Object>of(
                "productId", rs.getObject("product_id").toString(),
                "productName", rs.getString("product_name"),
                "avgPrice", rs.getBigDecimal("avg_price"),
                "cvPct", rs.getBigDecimal("cv_pct"),
                "type", "PRICE_VARIATION"
        ), tenantId, start, end));

        // -- Supplier premium alerts (>15% above product average across suppliers) --
        var premiumSql = """
                WITH product_avg AS (
                    SELECT ii.product_id, p.name AS product_name,
                           AVG(ii.unit_price / NULLIF(ii.conversion_factor, 0)) AS product_avg_price
                    FROM core.invoice_items ii
                    JOIN core.invoices i ON ii.invoice_id = i.id
                    JOIN core.products p ON ii.product_id = p.id
                    WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
                    GROUP BY ii.product_id, p.name
                ),
                supplier_prices AS (
                    SELECT ii.product_id, p.name AS product_name,
                           pr.id AS provider_id, pr.name AS provider_name,
                           AVG(ii.unit_price / NULLIF(ii.conversion_factor, 0)) AS avg_price,
                           COUNT(*) AS purchases
                    FROM core.invoice_items ii
                    JOIN core.invoices i ON ii.invoice_id = i.id
                    JOIN core.products p ON ii.product_id = p.id
                    JOIN core.providers pr ON i.provider_id = pr.id
                    WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
                    GROUP BY ii.product_id, p.name, pr.id, pr.name
                )
                SELECT sp.product_id, sp.product_name,
                       sp.provider_id, sp.provider_name,
                       sp.avg_price, pa.product_avg_price,
                       ROUND((sp.avg_price - pa.product_avg_price) / pa.product_avg_price * 100, 2) AS premium_pct
                FROM supplier_prices sp
                JOIN product_avg pa ON sp.product_id = pa.product_id
                WHERE sp.avg_price > pa.product_avg_price * 1.15
                ORDER BY premium_pct DESC
                """;
        alerts.addAll(jdbc.query(premiumSql, (rs, row) -> Map.<String, Object>of(
                "productId", rs.getObject("product_id").toString(),
                "productName", rs.getString("product_name"),
                "providerId", rs.getObject("provider_id").toString(),
                "providerName", rs.getString("provider_name"),
                "currentPrice", rs.getBigDecimal("avg_price"),
                "avgPrice", rs.getBigDecimal("product_avg_price"),
                "premiumPct", rs.getBigDecimal("premium_pct"),
                "type", "SUPPLIER_PREMIUM"
        ), tenantId, start, end, tenantId, start, end));

        if (alerts.isEmpty()) {
            return List.of(Map.<String, Object>of("message", "No significant anomalies detected"));
        }
        return alerts;
    }

    // -- 3 nuevos motores: proveedores, recomendación, predicción --

    List<Map<String, Object>> analisisComparativaProveedores(UUID tenantId, LocalDate start, LocalDate end) {
        var sql = """
                SELECT p.id AS product_id, p.name AS product_name,
                       pr.id AS provider_id, pr.name AS provider_name,
                       COUNT(*) AS purchase_count,
                       ROUND(AVG(ii.unit_price / NULLIF(ii.conversion_factor, 0)), 4) AS avg_price,
                       ROUND(MIN(ii.unit_price / NULLIF(ii.conversion_factor, 0)), 4) AS min_price,
                       ROUND(MAX(ii.unit_price / NULLIF(ii.conversion_factor, 0)), 4) AS max_price,
                       COALESCE(ROUND(STDDEV(ii.unit_price / NULLIF(ii.conversion_factor, 0)), 4), 0) AS price_stddev
                FROM core.invoice_items ii
                JOIN core.invoices i ON ii.invoice_id = i.id
                JOIN core.products p ON ii.product_id = p.id
                JOIN core.providers pr ON i.provider_id = pr.id
                WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
                GROUP BY p.id, p.name, pr.id, pr.name
                ORDER BY p.name, avg_price
                """;
        return jdbc.query(sql, (rs, row) -> Map.<String, Object>of(
                "productId", rs.getObject("product_id").toString(),
                "productName", rs.getString("product_name"),
                "providerId", rs.getObject("provider_id").toString(),
                "providerName", rs.getString("provider_name"),
                "purchaseCount", rs.getInt("purchase_count"),
                "avgPrice", rs.getBigDecimal("avg_price"),
                "minPrice", rs.getBigDecimal("min_price"),
                "maxPrice", rs.getBigDecimal("max_price"),
                "priceStddev", rs.getBigDecimal("price_stddev")
        ), tenantId, start, end);
    }

    List<Map<String, Object>> analisisRecomendacionProveedor(UUID tenantId, LocalDate start, LocalDate end) {
        var comparativa = analisisComparativaProveedores(tenantId, start, end);
        var byProduct = new LinkedHashMap<String, List<Map<String, Object>>>();
        for (var entry : comparativa) {
            var key = (String) entry.get("productId");
            byProduct.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
        }

        var recomendaciones = new ArrayList<Map<String, Object>>();
        for (var entry : byProduct.entrySet()) {
            var suppliers = entry.getValue();
            if (suppliers.size() < 2) continue;

            var cheapest = suppliers.stream().min(Comparator.comparing(s -> (BigDecimal) s.get("avgPrice"))).orElseThrow();
            var mostExpensive = suppliers.stream().max(Comparator.comparing(s -> (BigDecimal) s.get("avgPrice"))).orElseThrow();
            var cheapestPrice = (BigDecimal) cheapest.get("avgPrice");
            var expensivePrice = (BigDecimal) mostExpensive.get("avgPrice");
            var savings = expensivePrice.subtract(cheapestPrice);
            var savingsPct = expensivePrice.compareTo(BigDecimal.ZERO) > 0
                    ? savings.multiply(BigDecimal.valueOf(100)).divide(expensivePrice, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            recomendaciones.add(Map.<String, Object>of(
                    "productId", cheapest.get("productId"),
                    "productName", cheapest.get("productName"),
                    "recommendedProviderId", cheapest.get("providerId"),
                    "recommendedProviderName", cheapest.get("providerName"),
                    "recommendedPrice", cheapestPrice,
                    "currentAvgPrice", expensivePrice,
                    "savingsPerUnit", savings,
                    "savingsPct", savingsPct,
                    "supplierCount", suppliers.size()
            ));
        }
        recomendaciones.sort((a, b) -> ((BigDecimal) b.get("savingsPct")).compareTo((BigDecimal) a.get("savingsPct")));
        return recomendaciones;
    }

    List<Map<String, Object>> analisisProyeccionPrecios(UUID tenantId, LocalDate start, LocalDate end) {
        var lookbackStart = start.minusMonths(6);
        var sql = """
                WITH daily_prices AS (
                    SELECT ii.product_id, i.issue_date,
                           AVG(ii.unit_price / NULLIF(ii.conversion_factor, 0)) AS unit_price
                    FROM core.invoice_items ii
                    JOIN core.invoices i ON ii.invoice_id = i.id
                    WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
                    GROUP BY ii.product_id, i.issue_date
                ),
                ranked AS (
                    SELECT dp.product_id,
                           ROW_NUMBER() OVER (PARTITION BY dp.product_id ORDER BY dp.issue_date) AS rn,
                           dp.unit_price,
                           COUNT(*) OVER (PARTITION BY dp.product_id) AS data_points
                    FROM daily_prices dp
                )
                SELECT r.product_id, p.name AS product_name,
                       REGR_SLOPE(r.unit_price, r.rn) AS slope,
                       REGR_INTERCEPT(r.unit_price, r.rn) AS intercept,
                       COALESCE(REGR_R2(r.unit_price, r.rn), 0) AS r_squared,
                       MAX(r.data_points) AS data_points,
                       (ARRAY_AGG(r.unit_price ORDER BY r.rn DESC))[1] AS last_price
                FROM ranked r
                JOIN core.products p ON r.product_id = p.id
                GROUP BY r.product_id, p.name
                HAVING MAX(r.data_points) >= 3
                ORDER BY r.product_id
                """;
        var rows = jdbc.query(sql, (rs, row) -> new Object[]{
                rs.getObject("product_id").toString(),
                rs.getString("product_name"),
                rs.getBigDecimal("slope"),
                rs.getBigDecimal("intercept"),
                rs.getBigDecimal("r_squared"),
                rs.getInt("data_points"),
                rs.getBigDecimal("last_price")
        }, tenantId, lookbackStart, end);

        var resultados = new ArrayList<Map<String, Object>>();
        for (var r : rows) {
            var slope = r[2] == null ? BigDecimal.ZERO : (BigDecimal) r[2];
            var intercept = r[3] == null ? BigDecimal.ZERO : (BigDecimal) r[3];
            var rSquared = r[4] == null ? BigDecimal.ZERO : (BigDecimal) r[4];
            int n = (Integer) r[5];
            var lastPrice = (BigDecimal) r[6];

            // x (rn) es 1-based en SQL; el próximo punto observado es rn = n+1
            var predicted = slope.multiply(BigDecimal.valueOf(n + 1)).add(intercept)
                    .setScale(4, RoundingMode.HALF_UP);
            var change = lastPrice.compareTo(BigDecimal.ZERO) > 0
                    ? predicted.subtract(lastPrice).multiply(BigDecimal.valueOf(100)).divide(lastPrice, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            var confidence = BigDecimal.valueOf(Math.max(0, Math.min(100, rSquared.doubleValue() * 100)))
                    .setScale(1, RoundingMode.HALF_UP);

            resultados.add(Map.<String, Object>of(
                    "productId", (String) r[0],
                    "productName", (String) r[1],
                    "lastPrice", lastPrice,
                    "predictedPrice", predicted,
                    "pctChange", change,
                    "confidence", confidence,
                    "dataPoints", n
            ));
        }
        resultados.sort((a, b) -> ((BigDecimal) b.get("pctChange")).compareTo((BigDecimal) a.get("pctChange")));
        return resultados;
    }

    // -- Motor #10: Financial Health Engine (motor compuesto, cruza motores 1-9 + accounting + inventario) --

    Map<String, Object> analisisSaludFinanciera(UUID tenantId, String periodo, LocalDate start, LocalDate end,
                                                List<Map<String, Object>> abc,
                                                List<Map<String, Object>> comparativa,
                                                List<Map<String, Object>> alerts) {
        var metricas = metricasRepository.findByTenantIdAndPeriodLessThanEqualOrderByPeriodDesc(tenantId, periodo);
        var criticals = new ArrayList<Map<String, Object>>();
        var investments = new ArrayList<Map<String, Object>>();
        var expansions = new ArrayList<Map<String, Object>>();
        var recommendations = new ArrayList<String>();
        var requirements = new ArrayList<Map<String, Object>>();

        if (metricas.isEmpty()) {
            return Map.of(
                    "overallHealth", 0,
                    "breakdown", Map.of(),
                    "criticalAlerts", criticals,
                    "investmentSignals", investments,
                    "expansionReadiness", Map.of("score", 0, "status", "SIN_DATOS", "requirements", requirements),
                    "recommendations", List.of("Aún no hay métricas para este período — registra ventas y gastos para obtener tu salud financiera.")
            );
        }

        var current = metricas.get(0);
        var revenue = nz(current.getTotalIncome());
        var opex = nz(current.getOperatingExpenses());
        var opMargin = nz(current.getOperatingMargin());
        var opMarginPct = nz(current.getOperatingMarginPct());
        var grossMarginPct = nz(current.getGrossMarginPct());
        var netMarginPct = nz(current.getNetMarginPct());
        var loanPayments = nz(current.getLoanPayments());

        var supplierSpend = supplierSpend(tenantId, start, end);
        var supplierShare = maxShare(supplierSpend);
        var productShare = maxShare(abc);
        var hasPriceVariation = alerts.stream().anyMatch(a -> "PRICE_VARIATION".equals(a.get("type")));

        // ---- 🔴 Señales críticas ----
        if (opMarginPct.signum() < 0) {
            criticals.add(alert("NEGATIVE_OPERATING_MARGIN", "Margen Operativo Negativo",
                    "Tu negocio pierde dinero por cada venta. A este ritmo el capital se agota.",
                    opMarginPct, BigDecimal.ZERO, "Revisa tu estructura de precios o reduce gastos operativos."));
            recommendations.add("Tu margen operativo es negativo — ajusta precios y costos antes de escalar.");
        }

        var gross3m = firstN(metricas, 3, m -> nz(m.getGrossMarginPct()));
        if (gross3m.size() >= 3 && gross3m.get(0).signum() > 0 && gross3m.get(2).signum() >= 0
                && gross3m.get(0).compareTo(gross3m.get(2).multiply(BigDecimal.valueOf(0.85))) < 0) {
            criticals.add(alert("MARGIN_EROSION", "Erosión de Margen Bruto",
                    "El margen bruto cayó más de 15% en 3 meses. Pierdes poder de fijación de precios.",
                    gross3m.get(0), gross3m.get(2), "Reevalúa precios de venta y negocia costos de insumos."));
            recommendations.add("Margen bruto en caída >15% — negocia costos de insumos o sube precios.");
        }

        if (supplierShare.get("maxPct") != null && ((BigDecimal) supplierShare.get("maxPct")).compareTo(new BigDecimal("50")) > 0) {
            criticals.add(alert("SUPPLIER_CONCENTRATION", "Concentración de Proveedor",
                    "Un solo proveedor concentra más del 50% del gasto. Riesgo de interrupción.",
                    (BigDecimal) supplierShare.get("maxPct"), new BigDecimal("50"),
                    "Busca al menos 2 alternativas para el proveedor dominante."));
            recommendations.add("El proveedor " + supplierShare.get("name") + " concentra el " + supplierShare.get("maxPct")
                    + "% del gasto — negocia con 2 alternativas.");
        }

        if (opMargin.signum() > 0 && loanPayments.signum() > 0) {
            var ratio = loanPayments.divide(opMargin, 4, RoundingMode.HALF_UP);
            if (ratio.compareTo(new BigDecimal("0.30")) > 0) {
                criticals.add(alert("OVER_LEVERAGED", "Sobreendeudamiento",
                        "La deuda consume más del 30% del margen operativo.",
                        ratio, new BigDecimal("0.30"), "Reestructura pagos o reduce nueva deuda."));
                recommendations.add("La deuda consume >30% del margen operativo — reestructura pagos.");
            } else if (ratio.compareTo(new BigDecimal("0.15")) < 0) {
                investments.add(signal("DEBT_CAPACITY", "Capacidad de Endeudamiento", "Pagos de deuda", ratio, "<15% del margen operativo"));
            }
        }

        var revenue3m = firstN(metricas, 3, m -> nz(m.getTotalIncome()));
        var opex3m = firstN(metricas, 3, m -> nz(m.getOperatingExpenses()));
        if (revenue3m.size() >= 3 && opex3m.size() >= 3 && revenue3m.get(0).signum() > 0 && revenue3m.get(2).signum() > 0) {
            var revGrowth = growthPct(revenue3m.get(0), revenue3m.get(2));
            var opexGrowth = growthPct(opex3m.get(0), opex3m.get(2));
            if (opexGrowth.compareTo(revGrowth) > 0) {
                criticals.add(alert("OPEX_CREEP", "Crecimiento de Gastos Operativos",
                        "Los gastos operativos crecen más rápido que los ingresos. Ineficiencia creciente.",
                        opexGrowth, revGrowth, "Revisa partidas de gasto operativo y presupuesto."));
                recommendations.add("El gasto operativo crece más rápido que los ingresos — audita partidas fijas.");
            } else if (opexGrowth.compareTo(revGrowth) < 0) {
                expansions.add(signal("OPERATING_LEVERAGE", "Apalancamiento Operativo", "OpEx vs Revenue",
                        revGrowth.subtract(opexGrowth), "OpEx creciendo más lento que revenue"));
            }
        }

        var products = productoRepository.findByTenantId(tenantId);
        var dead = products.stream()
                .filter(p -> nz(p.getTotalInvestment()).signum() > 0)
                .filter(p -> p.getLastPurchaseDate() == null || p.getLastPurchaseDate().isBefore(LocalDate.now(ZoneOffset.UTC).minusDays(60)))
                .sorted(Comparator.comparing(Producto::getTotalInvestment).reversed())
                .limit(5)
                .toList();
        if (!dead.isEmpty()) {
            criticals.add(alert("DEAD_INVENTORY", "Inventario Muerto",
                    dead.size() + " productos con inversión alta y sin compras en 60+ días. Capital estancado.",
                    dead.stream().map(p -> nz(p.getTotalInvestment())).reduce(BigDecimal.ZERO, BigDecimal::add),
                    BigDecimal.ZERO, "Liquida o negocia devoluciones para liberar capital."));
            recommendations.add("Capital estancado en " + dead.size() + " productos sin compra en 60+ días — liquida inventario.");
        }

        // ---- 💸 Costo operativo diario vs ingreso diario ----
        var costPerDay = nz(current.getCostoOperativoDiario());
        var daysInMonth = start.lengthOfMonth();
        var revenuePerDay = revenue.divide(BigDecimal.valueOf(daysInMonth), 4, RoundingMode.HALF_UP);
        if (costPerDay.signum() > 0 && revenuePerDay.signum() > 0) {
            if (costPerDay.compareTo(revenuePerDay.multiply(new BigDecimal("1.2"))) > 0) {
                criticals.add(alert("DAILY_COST_CONTROL", "Costo Diario Sobre Ingreso Diario",
                        "Tu costo operativo diario supera en 20%+ el ingreso diario. Estás quemando capital cada día.",
                        costPerDay, revenuePerDay, "Reduce costos fijos o sube el ticket promedio de venta."));
                recommendations.add("El costo operativo diario supera el ingreso diario en 20%+ — recorta gastos fijos ya.");
            } else if (costPerDay.compareTo(revenuePerDay.multiply(new BigDecimal("0.8"))) < 0) {
                expansions.add(signal("DAILY_COST_COVERED", "Costo Diario Cubierto",
                        "Costo diario vs ingreso diario", costPerDay, "costo < 80% del ingreso diario"));
            }
        }

        // ---- 💰 Capital de respaldo (Patrimonio vs burn) ----
        var patrimonio = patrimonioRepository.findByTenantId(tenantId).orElse(null);
        if (patrimonio != null && patrimonio.getInitialCapital() != null && costPerDay.signum() > 0) {
            var monthlyBurn = costPerDay.multiply(BigDecimal.valueOf(30));
            var monthsCovered = patrimonio.getInitialCapital().divide(monthlyBurn, 2, RoundingMode.HALF_UP);
            if (monthsCovered.compareTo(BigDecimal.ONE) < 0 && netMarginPct.signum() < 0) {
                criticals.add(alert("CAPITAL_BURN", "Quema de Capital",
                        "El capital inicial cubre menos de 1 mes de costos operativos y el margen neto es negativo.",
                        monthsCovered, BigDecimal.ONE, "Inyecta capital o detén la quema recortando costos de inmediato."));
                recommendations.add("Tu capital cubre menos de 1 mes de operación con margen neto negativo — urgen medidas de costos.");
            } else if (monthsCovered.compareTo(new BigDecimal("3")) >= 0 && netMarginPct.signum() > 0) {
                expansions.add(signal("CAPITAL_READINESS", "Capital de Respaldo",
                        "Meses cubiertos por capital inicial", monthsCovered, "3+ meses con margen neto positivo"));
            }
        }

        // ---- 🟢 Señales de inversión ----
        if (grossMarginPct.compareTo(new BigDecimal("30")) > 0
                && opMarginPct.compareTo(new BigDecimal("10")) > 0
                && netMarginPct.compareTo(new BigDecimal("5")) > 0) {
            investments.add(signal("HEALTHY_MARGIN_STACK", "Pila de Márgenes Saludable",
                    "Bruto/Operativo/Neto", grossMarginPct, ">30% / >10% / >5%"));
        }

        if (isPositiveForN(metricas, 3, m -> nz(m.getOperatingMargin()))) {
            investments.add(signal("POSITIVE_CASH_FLOW", "Flujo de Caja Positivo",
                    "Margen operativo positivo", opMarginPct, "3+ meses consecutivos"));
        }

        if ((supplierShare.get("maxPct") == null || ((BigDecimal) supplierShare.get("maxPct")).compareTo(new BigDecimal("30")) < 0)
                && (productShare.get("maxPct") == null || ((BigDecimal) productShare.get("maxPct")).compareTo(new BigDecimal("20")) < 0)) {
            investments.add(signal("LOW_CONCENTRATION", "Concentración Baja",
                    "Proveedor/producto dominante", (BigDecimal) supplierShare.getOrDefault("maxPct", BigDecimal.ZERO), "ninguno >30% proveedor / >20% producto"));
        }

        // ---- 🟣 Señales de expansión ----
        if (isPositiveForN(metricas, 6, m -> nz(m.getNetMarginPct()))) {
            expansions.add(signal("SUSTAINED_PROFITABILITY", "Rentabilidad Sostenida",
                    "Margen neto positivo", netMarginPct, "6+ meses"));
        }

        var abcAProducts = abc.stream().filter(a -> "A".equals(a.get("category")))
                .map(a -> a.get("productId").toString()).collect(Collectors.toSet());
        var matureSuppliers = comparativa.stream()
                .filter(c -> abcAProducts.contains(c.get("productId").toString()))
                .map(c -> c.get("providerId").toString())
                .collect(Collectors.toSet());
        if (matureSuppliers.size() >= 3) {
            expansions.add(signal("SUPPLIER_MATURITY", "Madurez de Proveedores",
                    "Proveedores en categorías ABC-A", BigDecimal.valueOf(matureSuppliers.size()), "3+"));
        }

        if (revenue.signum() > 0 && loanPayments.signum() > 0
                && loanPayments.divide(revenue, 4, RoundingMode.HALF_UP).compareTo(new BigDecimal("0.10")) < 0) {
            expansions.add(signal("DEBT_CUSHION", "Colchón de Deuda",
                    "Deuda vs ingresos", loanPayments.divide(revenue, 4, RoundingMode.HALF_UP), "<10% de revenue"));
        }

        // ---- Scoring compuesto (0-100): profitability 35% + efficiency 25% + stability 25% + growth 15% ----
        // ponytail: normalización heurística por umbral (bruto 40%, operativo 15%, neto 10%); opexRatio 0-50% mapea a 100-0
        var grossScore = clampScore(nz(grossMarginPct).divide(new BigDecimal("40"), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
        var opScore = clampScore(nz(opMarginPct).divide(new BigDecimal("15"), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
        var netScore = clampScore(nz(netMarginPct).divide(new BigDecimal("10"), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
        var profitability = (int) Math.round(grossScore * 0.40 + opScore * 0.35 + netScore * 0.25);

        var opexRatio = revenue.signum() > 0 ? nz(opex).divide(revenue, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        var efficiency = clampScore(BigDecimal.ONE.subtract(opexRatio.divide(new BigDecimal("0.5"), 4, RoundingMode.HALF_UP)).multiply(BigDecimal.valueOf(100)));

        var supplierMax = (BigDecimal) supplierShare.getOrDefault("maxPct", BigDecimal.ZERO);
        var supplierScore = clampScore(BigDecimal.valueOf(100).subtract(supplierMax.divide(new BigDecimal("0.5"), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))));
        var priceScore = hasPriceVariation ? 60 : 100;
        var stability = (int) Math.round(supplierScore * 0.5 + priceScore * 0.5);

        var revenueGrowth = revenue3m.size() >= 3 && revenue3m.get(2).signum() > 0
                ? growthPct(revenue3m.get(0), revenue3m.get(2)).doubleValue() : 0.0;
        var growth = clampScore(BigDecimal.valueOf(50 + revenueGrowth * 5));

        var overallHealth = (int) Math.round(profitability * 0.35 + efficiency * 0.25 + stability * 0.25 + growth * 0.15);

        // ---- Expansion readiness ----
        for (var sig : expansions) {
            requirements.add(Map.of(
                    "met", true,
                    "label", sig.get("label"),
                    "current", String.valueOf(sig.get("current"))));
        }
        var expScore = expansions.isEmpty() ? 0 : (int) Math.round(expansions.size() * 100.0 / 4);
        var status = expScore >= 75 ? "LISTO" : expScore >= 50 ? "EN_DESARROLLO" : "PREPARACION";

        if (recommendations.isEmpty() && !criticals.isEmpty()) recommendations.add("Atiende las alertas críticas antes de invertir.");
        if (recommendations.isEmpty()) recommendations.add("Tu salud financiera es estable — evalúa las señales de inversión para crecer.");

        var out = new LinkedHashMap<String, Object>();
        out.put("overallHealth", overallHealth);
        out.put("breakdown", Map.of(
                "profitability", Map.of("score", profitability, "drivers", List.of(
                        "grossMarginPct: " + grossMarginPct + "%", "operatingMarginPct: " + opMarginPct + "%", "netMarginPct: " + netMarginPct + "%")),
                "efficiency", Map.of("score", efficiency, "drivers", List.of(
                        "opexRatio: " + opexRatio.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP) + "%")),
                "stability", Map.of("score", stability, "drivers", List.of(
                        "supplierConcentration: " + supplierMax + "%", "priceStability: " + (hasPriceVariation ? 0.6 : 0.92))),
                "growth", Map.of("score", growth, "drivers", List.of(
                        "revenueTrend: " + BigDecimal.valueOf(revenueGrowth).setScale(1, RoundingMode.HALF_UP) + "%", "inventoryTurnover: " + dead.size()))));
        out.put("criticalAlerts", criticals);
        out.put("investmentSignals", investments);
        out.put("expansionReadiness", Map.of("score", expScore, "status", status, "requirements", requirements));
        out.put("recommendations", recommendations);
        return out;
    }

    private List<Map<String, Object>> supplierSpend(UUID tenantId, LocalDate start, LocalDate end) {
        var sql = """
                SELECT i.provider_id, pr.name AS provider_name, SUM(ii.subtotal) AS total_spend
                FROM core.invoice_items ii
                JOIN core.invoices i ON ii.invoice_id = i.id
                JOIN core.providers pr ON i.provider_id = pr.id
                WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
                GROUP BY i.provider_id, pr.name
                """;
        return jdbc.query(sql, (rs, row) -> {
            var m = new LinkedHashMap<String, Object>();
            m.put("providerId", rs.getObject("provider_id").toString());
            m.put("providerName", rs.getString("provider_name"));
            m.put("totalSpend", rs.getBigDecimal("total_spend"));
            return m;
        }, tenantId, start, end);
    }

    private Map<String, Object> maxShare(List<Map<String, Object>> rows) {
        BigDecimal grand = BigDecimal.ZERO;
        for (var r : rows) grand = grand.add(nz((BigDecimal) r.getOrDefault("totalSpend", BigDecimal.ZERO)));
        Map<String, Object> best = new LinkedHashMap<>();
        if (grand.signum() <= 0) return best;
        for (var r : rows) {
            var spend = nz((BigDecimal) r.getOrDefault("totalSpend", BigDecimal.ZERO));
            var pct = spend.multiply(BigDecimal.valueOf(100)).divide(grand, 2, RoundingMode.HALF_UP);
            if (best.get("maxPct") == null || pct.compareTo((BigDecimal) best.get("maxPct")) > 0) {
                best.put("maxPct", pct);
                best.put("name", r.getOrDefault("providerName", r.get("name")));
            }
        }
        return best;
    }

    private List<BigDecimal> firstN(List<MetricasFinanciera> metricas, int n, java.util.function.Function<MetricasFinanciera, BigDecimal> f) {
        return metricas.stream().limit(n).map(f).toList();
    }

    private boolean isPositiveForN(List<MetricasFinanciera> metricas, int n, java.util.function.Function<MetricasFinanciera, BigDecimal> f) {
        if (metricas.size() < n) return false;
        return metricas.stream().limit(n).map(f).allMatch(v -> nz(v).signum() > 0);
    }

    private BigDecimal growthPct(BigDecimal latest, BigDecimal earliest) {
        if (earliest.signum() <= 0) return BigDecimal.ZERO;
        return latest.subtract(earliest).multiply(BigDecimal.valueOf(100)).divide(earliest, 2, RoundingMode.HALF_UP);
    }

    private static int clampScore(BigDecimal raw) {
        return Math.max(0, Math.min(100, raw.setScale(0, RoundingMode.HALF_UP).intValue()));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static Map<String, Object> alert(String type, String title, String message, BigDecimal metric, BigDecimal threshold, String action) {
        var m = new LinkedHashMap<String, Object>();
        m.put("type", type);
        m.put("severity", "CRITICAL");
        m.put("title", title);
        m.put("message", message);
        m.put("metric", metric);
        m.put("threshold", threshold);
        m.put("action", action);
        return m;
    }

    private static Map<String, Object> signal(String type, String label, String metricLabel, BigDecimal current, String threshold) {
        var m = new LinkedHashMap<String, Object>();
        m.put("type", type);
        m.put("status", "met");
        m.put("label", label);
        m.put("metricLabel", metricLabel);
        m.put("current", current);
        m.put("threshold", threshold);
        return m;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize analytics result", e);
            return "[]";
        }
    }
}

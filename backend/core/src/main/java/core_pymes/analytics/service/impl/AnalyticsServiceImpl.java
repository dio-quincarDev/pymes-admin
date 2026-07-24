package core_pymes.analytics.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import core_pymes.analytics.domain.AnalisisGasto;
import core_pymes.analytics.repository.AnalisisGastoRepository;
import core_pymes.analytics.service.AnalyticsService;
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

        analisis.setAbc(toJson(analisisABC(tenantId, start, end)));
        analisis.setTrend(toJson(analisisTendencia(tenantId, start, end)));
        analisis.setMargin(toJson(analisisMargen(tenantId, start, end)));
        analisis.setOpexPct(toJson(analisisCostoOperativo(tenantId, start, end)));
        analisis.setProjection(toJson(analisisProyeccion(tenantId, start, end)));
        analisis.setAlerts(toJson(analisisAlertas(tenantId, start, end)));
        analisis.setSupplierComparison(toJson(analisisComparativaProveedores(tenantId, start, end)));
        analisis.setSupplierRecommendations(toJson(analisisRecomendacionProveedor(tenantId, start, end)));
        analisis.setPricePrediction(toJson(analisisProyeccionPrecios(tenantId, start, end)));

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
                           AVG(ii.unit_price / ii.conversion_factor) AS current_avg_price
                    FROM core.invoice_items ii
                    JOIN core.invoices i ON ii.invoice_id = i.id
                    JOIN core.products p ON ii.product_id = p.id
                    WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
                    GROUP BY ii.product_id, p.name
                ), moving_avg AS (
                    SELECT ii.product_id,
                           AVG(ii.unit_price / ii.conversion_factor) AS moving_avg_90d
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
                           AVG(ii.unit_price / ii.conversion_factor) AS avg_price
                    FROM core.invoice_items ii
                    JOIN core.invoices i ON ii.invoice_id = i.id
                    JOIN core.products p ON ii.product_id = p.id
                    WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
                    GROUP BY ii.product_id, p.name
                ), previous_prices AS (
                    SELECT ii.product_id,
                           AVG(ii.unit_price / ii.conversion_factor) AS avg_price
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

    List<Map<String, Object>> analisisCostoOperativo(UUID tenantId, LocalDate start, LocalDate end) {
        var sql = """
                WITH period_data AS (
                    SELECT i.id, i.issue_date, ii.product_id, i.provider_id, ii.subtotal
                    FROM core.invoices i
                    JOIN core.invoice_items ii ON ii.invoice_id = i.id
                    WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
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
        return jdbc.query(sql, (rs, row) -> {
            var total = rs.getBigDecimal("total_spend");
            var dailyAvg = rs.getBigDecimal("avg_daily_spend");
            var daysInMonth = start.lengthOfMonth();
            return Map.<String, Object>of(
                    "invoiceCount", rs.getInt("invoice_count"),
                    "productCount", rs.getInt("product_count"),
                    "providerCount", rs.getInt("provider_count"),
                    "totalSpend", total,
                    "avgDailySpend", dailyAvg,
                    "projectedMonthly", dailyAvg.multiply(BigDecimal.valueOf(daysInMonth))
            );
        }, tenantId, start, end);
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
                           AVG(ii.unit_price / ii.conversion_factor) AS avg_price,
                           STDDEV(ii.unit_price / ii.conversion_factor) AS stddev_price,
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
                SELECT ii.product_id, p.name AS product_name, i.issue_date,
                       AVG(ii.unit_price / NULLIF(ii.conversion_factor, 0)) AS unit_price
                FROM core.invoice_items ii
                JOIN core.invoices i ON ii.invoice_id = i.id
                JOIN core.products p ON ii.product_id = p.id
                WHERE i.tenant_id = ? AND i.issue_date >= ? AND i.issue_date < ?
                GROUP BY ii.product_id, p.name, i.issue_date
                ORDER BY ii.product_id, i.issue_date
                """;
        var rows = jdbc.query(sql, (rs, row) -> new Object[]{
                rs.getObject("product_id").toString(),
                rs.getString("product_name"),
                rs.getObject("issue_date", LocalDate.class),
                rs.getBigDecimal("unit_price")
        }, tenantId, lookbackStart, end);

        var byProduct = new LinkedHashMap<String, List<Object[]>>();
        for (var r : rows) {
            byProduct.computeIfAbsent((String) r[0], k -> new ArrayList<>()).add(r);
        }

        var resultados = new ArrayList<Map<String, Object>>();
        for (var entry : byProduct.entrySet()) {
            var points = entry.getValue();
            if (points.size() < 3) continue;

            var productName = (String) points.get(0)[1];
            int n = points.size();
            double sumX = 0, sumY = 0, sumXy = 0, sumXx = 0;
            for (int i = 0; i < n; i++) {
                double x = i;
                double y = ((BigDecimal) points.get(i)[3]).doubleValue();
                sumX += x;
                sumY += y;
                sumXy += x * y;
                sumXx += x * x;
            }
            double slope = (n * sumXy - sumX * sumY) / (n * sumXx - sumX * sumX);
            double intercept = (sumY - slope * sumX) / n;
            double predictedPrice = slope * n + intercept;

            // R² for confidence
            double meanY = sumY / n;
            double ssTot = 0, ssRes = 0;
            for (int i = 0; i < n; i++) {
                double y = ((BigDecimal) points.get(i)[3]).doubleValue();
                double yHat = slope * i + intercept;
                ssTot += (y - meanY) * (y - meanY);
                ssRes += (y - yHat) * (y - yHat);
            }
            double rSquared = ssTot > 0 ? 1 - ssRes / ssTot : 0;
            double confidence = Math.max(0, Math.min(100, rSquared * 100));

            var lastPrice = (BigDecimal) points.get(n - 1)[3];
            var predicted = BigDecimal.valueOf(predictedPrice).setScale(4, RoundingMode.HALF_UP);
            var change = lastPrice.compareTo(BigDecimal.ZERO) > 0
                    ? predicted.subtract(lastPrice).multiply(BigDecimal.valueOf(100)).divide(lastPrice, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            resultados.add(Map.<String, Object>of(
                    "productId", entry.getKey(),
                    "productName", productName,
                    "lastPrice", lastPrice,
                    "predictedPrice", predicted,
                    "pctChange", change,
                    "confidence", BigDecimal.valueOf(confidence).setScale(1, RoundingMode.HALF_UP),
                    "dataPoints", n
            ));
        }
        resultados.sort((a, b) -> ((BigDecimal) b.get("pctChange")).compareTo((BigDecimal) a.get("pctChange")));
        return resultados;
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

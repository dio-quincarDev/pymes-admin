package core_pymes.jpa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JPA: backfill de desglose de ventas (V9)")
class BackfillSalePaymentsTest extends AbstractJpaTest {

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    private UUID tenant;

    @BeforeEach
    void seed() {
        tenant = UUID.randomUUID();
        // Limpia por si acaso (AbstractJpaTest ya borra, pero payments primero)
        jdbc.execute("DELETE FROM core.daily_sale_payments");
        jdbc.execute("DELETE FROM core.daily_sales");
    }

    private void runBackfill() {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V9__sale_payments_breakdown.sql")) {
            if (in == null) throw new IllegalStateException("V9 not found on classpath");
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            jdbc.execute(sql);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void insertSale(LocalDate date, String desc, String amount) {
        jdbc.update(
                "INSERT INTO core.daily_sales (id, tenant_id, sale_date, gross_amount, description, is_active) " +
                        "VALUES (gen_random_uuid(), ?, ?, ?::numeric, ?, true)",
                tenant, date, amount, desc);
    }

    @Nested
    @DisplayName("casos validos")
    class ValidCases {

        @Test
        @DisplayName("parsea y agrega por enum; Ben se suma a YAPPY")
        void aggregatesByEnum() {
            insertSale(LocalDate.of(2026, 9, 4), "Efectivo:180.60, Punto de Venta: 15.45, Ben: 5.00, Yappy: 191.55", "392.60");
            runBackfill();

            var rows = jdbc.queryForList(
                    "SELECT metodo, monto FROM core.daily_sale_payments dsp " +
                            "JOIN core.daily_sales ds ON ds.id = dsp.sale_id WHERE ds.tenant_id = ?", tenant);
            assertThat(rows).hasSize(3);
            // YAPPY debe ser 191.55 + 5.00 = 196.55
            var yappy = rows.stream().filter(r -> "YAPPY".equals(r.get("metodo"))).findFirst().orElseThrow();
            assertThat(new BigDecimal(yappy.get("monto").toString())).isEqualByComparingTo("196.55");
        }

        @Test
        @DisplayName("PVenta con ; como separador se parsea")
        void semicolonSeparator() {
            insertSale(LocalDate.of(2026, 9, 10), "Efectivo: 129.00, Yappy: 105.45, PVenta;26.75, Ben: 4.85", "266.05");
            runBackfill();

            var metodos = jdbc.queryForList(
                    "SELECT metodo FROM core.daily_sale_payments dsp JOIN core.daily_sales ds ON ds.id=dsp.sale_id WHERE ds.tenant_id=?",
                    String.class, tenant);
            assertThat(metodos).containsExactlyInAnyOrder("EFECTIVO", "YAPPY", "TARJETA");
        }

        @Test
        @DisplayName("punto seguido de letra separa partes")
        void dotSeparator() {
            insertSale(LocalDate.of(2026, 9, 1), "Efectivo:100.00. Yappy: 50.00", "150.00");
            runBackfill();
            var count = jdbc.queryForObject(
                    "SELECT count(*) FROM core.daily_sale_payments dsp JOIN core.daily_sales ds ON ds.id=dsp.sale_id WHERE ds.tenant_id=?",
                    Integer.class, tenant);
            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("casos invalidos (no inserta nada)")
    class InvalidCases {

        @Test
        @DisplayName("fila sin descripcion -> sin desglose")
        void noDescription() {
            jdbc.update(
                    "INSERT INTO core.daily_sales (id, tenant_id, sale_date, gross_amount, description, is_active) " +
                            "VALUES (gen_random_uuid(), ?, ?, ?::numeric, NULL, true)",
                    tenant, LocalDate.of(2026, 9, 30), "451.05");
            runBackfill();
            var count = jdbc.queryForObject(
                    "SELECT count(*) FROM core.daily_sale_payments dsp JOIN core.daily_sales ds ON ds.id=dsp.sale_id WHERE ds.tenant_id=?",
                    Integer.class, tenant);
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("descuadre -> no inserta")
        void mismatch() {
            insertSale(LocalDate.of(2026, 9, 2), "Efectivo: 100.00, Yappy: 50.00", "200.00");
            runBackfill();
            var count = jdbc.queryForObject(
                    "SELECT count(*) FROM core.daily_sale_payments dsp JOIN core.daily_sales ds ON ds.id=dsp.sale_id WHERE ds.tenant_id=?",
                    Integer.class, tenant);
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("etiqueta desconocida -> no inserta")
        void unknownLabel() {
            insertSale(LocalDate.of(2026, 9, 3), "Efectivo: 100.00, Bitcoin: 50.00", "150.00");
            runBackfill();
            var count = jdbc.queryForObject(
                    "SELECT count(*) FROM core.daily_sale_payments dsp JOIN core.daily_sales ds ON ds.id=dsp.sale_id WHERE ds.tenant_id=?",
                    Integer.class, tenant);
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("idempotencia")
    class Idempotency {

        @Test
        @DisplayName("re-ejecutar el backfill no duplica")
        void rerun() {
            insertSale(LocalDate.of(2026, 9, 5), "Efectivo: 10.00, Yappy: 20.00", "30.00");
            runBackfill();
            runBackfill();
            var count = jdbc.queryForObject(
                    "SELECT count(*) FROM core.daily_sale_payments dsp JOIN core.daily_sales ds ON ds.id=dsp.sale_id WHERE ds.tenant_id=?",
                    Integer.class, tenant);
            assertThat(count).isEqualTo(2);
        }
    }
}

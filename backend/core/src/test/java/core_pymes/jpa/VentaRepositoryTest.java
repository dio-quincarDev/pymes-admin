package core_pymes.jpa;

import core_pymes.venta.domain.VentaDiaria;
import core_pymes.venta.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JPA: VentaDiaria repository")
class VentaRepositoryTest extends AbstractJpaTest {

    @Autowired
    private VentaRepository ventaRepository;

    private UUID tenantA;
    private UUID tenantB;
    private VentaDiaria venta1;
    private VentaDiaria venta2;

    @BeforeEach
    void setUp() {
        tenantA = UUID.randomUUID();
        tenantB = UUID.randomUUID();

        venta1 = em.persistFlushFind(VentaDiaria.builder()
                .tenantId(tenantA).saleDate(LocalDate.of(2026, 6, 1))
                .grossAmount(new BigDecimal("350.00")).description("Venta día 1").build());
        venta2 = em.persistFlushFind(VentaDiaria.builder()
                .tenantId(tenantA).saleDate(LocalDate.of(2026, 6, 15))
                .grossAmount(new BigDecimal("520.50")).description("Venta día 15").build());
        em.persistFlushFind(VentaDiaria.builder()
                .tenantId(tenantB).saleDate(LocalDate.of(2026, 6, 10))
                .grossAmount(new BigDecimal("100.00")).description("Otro tenant").build());
        em.clear();
    }

    @Nested
    @DisplayName("findByTenantIdOrderByCreatedAtDesc")
    class TenantQueryTests {

        @Test
        @DisplayName("returns only sales for that tenant")
        void returns_tenantSales() {
            var result = ventaRepository.findByTenantIdOrderByCreatedAtDesc(tenantA);
            assertThat(result).hasSize(2)
                    .extracting(VentaDiaria::getGrossAmount)
                    .containsExactlyInAnyOrder(new BigDecimal("350.00"), new BigDecimal("520.50"));
        }

        @Test
        @DisplayName("returns empty for unknown tenant")
        void returns_empty_forUnknownTenant() {
            var result = ventaRepository.findByTenantIdOrderByCreatedAtDesc(UUID.randomUUID());
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByTenantIdAndSaleDateBetweenOrderBySaleDateDesc")
    class DateRangeTests {

        @Test
        @DisplayName("filters sales by date range")
        void filters_byDateRange() {
            var from = LocalDate.of(2026, 6, 1);
            var to = LocalDate.of(2026, 6, 16);
            var result = ventaRepository.findByTenantIdAndSaleDateBetweenOrderBySaleDateDesc(
                    tenantA, from, to);
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("returns empty when no sales in range")
        void returns_empty_whenNoMatch() {
            var from = LocalDate.of(2026, 7, 1);
            var to = LocalDate.of(2026, 7, 31);
            var result = ventaRepository.findByTenantIdAndSaleDateBetweenOrderBySaleDateDesc(
                    tenantA, from, to);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("date range filters cross tenant boundary")
        void dateRange_scopedByTenant() {
            var from = LocalDate.of(2026, 6, 1);
            var to = LocalDate.of(2026, 6, 30);
            var resultA = ventaRepository.findByTenantIdAndSaleDateBetweenOrderBySaleDateDesc(
                    tenantA, from, to);
            var resultB = ventaRepository.findByTenantIdAndSaleDateBetweenOrderBySaleDateDesc(
                    tenantB, from, to);
            assertThat(resultA).hasSize(2);
            assertThat(resultB).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Soft delete")
    class SoftDeleteTests {

        @Test
        @DisplayName("deleted sales are excluded from queries")
        void softDelete_excludesDeleted() {
            ventaRepository.deleteById(venta1.getId());
            em.flush();
            em.clear();

            assertThat(ventaRepository.findByTenantIdOrderByCreatedAtDesc(tenantA)).hasSize(1);
            assertThat(ventaRepository.findById(venta1.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("multiple sales on same day are allowed")
        void multipleSalesSameDay() {
            em.persistFlushFind(VentaDiaria.builder()
                    .tenantId(tenantA).saleDate(LocalDate.of(2026, 6, 1))
                    .grossAmount(new BigDecimal("100.00")).description("Segunda venta del día").build());
            em.clear();

            var result = ventaRepository.findByTenantIdAndSaleDateBetweenOrderBySaleDateDesc(
                    tenantA, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2));
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("gross amount with 2 decimal precision")
        void amountPrecision() {
            var venta = em.persistFlushFind(VentaDiaria.builder()
                    .tenantId(tenantA).saleDate(LocalDate.of(2026, 6, 20))
                    .grossAmount(new BigDecimal("99.99")).build());
            em.clear();
            var loaded = em.find(VentaDiaria.class, venta.getId());
            assertThat(loaded.getGrossAmount()).isEqualByComparingTo("99.99");
        }

        @Test
        @DisplayName("description can be null")
        void nullableDescription() {
            var venta = em.persistFlushFind(VentaDiaria.builder()
                    .tenantId(tenantA).saleDate(LocalDate.of(2026, 6, 20))
                    .grossAmount(new BigDecimal("50.00")).description(null).build());
            em.clear();
            var loaded = em.find(VentaDiaria.class, venta.getId());
            assertThat(loaded.getDescription()).isNull();
        }

        @Test
        @DisplayName("default isActive is true")
        void defaultIsActive() {
            var venta = em.persistFlushFind(VentaDiaria.builder()
                    .tenantId(tenantA).saleDate(LocalDate.of(2026, 6, 20))
                    .grossAmount(new BigDecimal("75.00")).build());
            em.clear();
            var loaded = em.find(VentaDiaria.class, venta.getId());
            assertThat(loaded.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("Between is inclusive on both ends")
        void betweenIsInclusive() {
            var from = LocalDate.of(2026, 6, 15);
            var to = LocalDate.of(2026, 6, 15);
            var result = ventaRepository.findByTenantIdAndSaleDateBetweenOrderBySaleDateDesc(
                    tenantA, from, to);
            assertThat(result).hasSize(1); // venta2 has saleDate=2026-06-15
        }
    }
}

package core_pymes.jpa;

import core_pymes.gasto.domain.CategoriaGasto;
import core_pymes.gasto.domain.GastoOperativo;
import core_pymes.gasto.repository.GastoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JPA: GastoOperativo repository")
class GastoRepositoryTest extends AbstractJpaTest {

    @Autowired
    private GastoRepository gastoRepository;

    private UUID tenantA;
    private UUID tenantB;
    private GastoOperativo gasto1;
    private GastoOperativo gasto2;

    @BeforeEach
    void setUp() {
        tenantA = UUID.randomUUID();
        tenantB = UUID.randomUUID();

        gasto1 = em.persistFlushFind(GastoOperativo.builder()
                .tenantId(tenantA).category(CategoriaGasto.SALARIOS)
                .description("Salario mes junio").amount(new BigDecimal("1500.00"))
                .expenseDate(LocalDate.of(2026, 6, 1)).paymentMethod("TRANSFERENCIA").build());
        gasto2 = em.persistFlushFind(GastoOperativo.builder()
                .tenantId(tenantA).category(CategoriaGasto.AGUA)
                .description("Servicio de agua junio").amount(new BigDecimal("45.50"))
                .expenseDate(LocalDate.of(2026, 6, 15)).paymentMethod("EFECTIVO").build());
        em.persistFlushFind(GastoOperativo.builder()
                .tenantId(tenantB).category(CategoriaGasto.LUZ)
                .description("Electricidad").amount(new BigDecimal("120.00"))
                .expenseDate(LocalDate.of(2026, 6, 10)).paymentMethod("TARJETA").build());
        em.clear();
    }

    @Nested
    @DisplayName("findByTenantIdOrderByCreatedAtDesc")
    class TenantQueryTests {

        @Test
        @DisplayName("returns only expenses for that tenant")
        void returns_tenantExpenses() {
            var result = gastoRepository.findByTenantIdOrderByCreatedAtDesc(tenantA);
            assertThat(result).hasSize(2)
                    .extracting(GastoOperativo::getCategory)
                    .containsExactlyInAnyOrder(CategoriaGasto.SALARIOS, CategoriaGasto.AGUA);
        }

        @Test
        @DisplayName("returns empty for tenant with no expenses")
        void returns_empty_forUnknownTenant() {
            var result = gastoRepository.findByTenantIdOrderByCreatedAtDesc(UUID.randomUUID());
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByTenantIdAndExpenseDateBetweenOrderByExpenseDateDesc")
    class DateRangeTests {

        @Test
        @DisplayName("filters expenses by date range")
        void filters_byDateRange() {
            var from = LocalDate.of(2026, 6, 1);
            var to = LocalDate.of(2026, 6, 16);
            var result = gastoRepository.findByTenantIdAndExpenseDateBetweenOrderByExpenseDateDesc(
                    tenantA, from, to);
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("returns empty when date range has no expenses")
        void returns_empty_whenNoMatch() {
            var from = LocalDate.of(2026, 7, 1);
            var to = LocalDate.of(2026, 7, 31);
            var result = gastoRepository.findByTenantIdAndExpenseDateBetweenOrderByExpenseDateDesc(
                    tenantA, from, to);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Between is inclusive on both ends")
        void betweenIsInclusive() {
            var from = LocalDate.of(2026, 6, 15);
            var to = LocalDate.of(2026, 6, 15);
            var result = gastoRepository.findByTenantIdAndExpenseDateBetweenOrderByExpenseDateDesc(
                    tenantA, from, to);
            assertThat(result).hasSize(1); // gasto2 has expenseDate=2026-06-15
        }
    }

    @Nested
    @DisplayName("Soft delete")
    class SoftDeleteTests {

        @Test
        @DisplayName("deleted expenses are excluded from queries")
        void softDelete_excludesDeleted() {
            gastoRepository.deleteById(gasto1.getId());
            em.flush();
            em.clear();

            assertThat(gastoRepository.findByTenantIdOrderByCreatedAtDesc(tenantA)).hasSize(1);
            assertThat(gastoRepository.findById(gasto1.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("CategoriaGasto enum mapping")
    class EnumMappingTests {

        @Test
        @DisplayName("all enum values persist and load correctly")
        void enumMapping_roundTrip() {
            for (var cat : CategoriaGasto.values()) {
                var gasto = em.persistFlushFind(GastoOperativo.builder()
                        .tenantId(tenantA).category(cat)
                        .description("Test " + cat.name())
                        .amount(BigDecimal.ONE)
                        .expenseDate(LocalDate.of(2026, 1, 1)).build());
                em.clear();
                var loaded = em.find(GastoOperativo.class, gasto.getId());
                assertThat(loaded.getCategory()).isEqualTo(cat);
            }
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("amount with high precision persists correctly")
        void highPrecision_amount() {
            var gasto = em.persistFlushFind(GastoOperativo.builder()
                    .tenantId(tenantA).category(CategoriaGasto.INTERNET)
                    .description("Internet fibra óptica").amount(new BigDecimal("99.99"))
                    .expenseDate(LocalDate.of(2026, 6, 20)).build());
            em.clear();
            var loaded = em.find(GastoOperativo.class, gasto.getId());
            assertThat(loaded.getAmount()).isEqualByComparingTo("99.99");
        }

        @Test
        @DisplayName("nullable fields persist as null")
        void nullableFields() {
            var gasto = em.persistFlushFind(GastoOperativo.builder()
                    .tenantId(tenantA).category(CategoriaGasto.OTROS)
                    .description(null).amount(new BigDecimal("10.00"))
                    .expenseDate(LocalDate.of(2026, 6, 20)).paymentMethod(null).build());
            em.clear();
            var loaded = em.find(GastoOperativo.class, gasto.getId());
            assertThat(loaded.getDescription()).isNull();
            assertThat(loaded.getPaymentMethod()).isNull();
        }

        @Test
        @DisplayName("default isActive is true")
        void defaultIsActive() {
            var gasto = em.persistFlushFind(GastoOperativo.builder()
                    .tenantId(tenantA).category(CategoriaGasto.MANTENIMIENTO)
                    .amount(new BigDecimal("50.00"))
                    .expenseDate(LocalDate.of(2026, 6, 20)).build());
            em.clear();
            var loaded = em.find(GastoOperativo.class, gasto.getId());
            assertThat(loaded.getIsActive()).isTrue();
        }
    }
}

package core_pymes.jpa;

import core_pymes.accounting.domain.MetricasFinanciera;
import core_pymes.accounting.repository.MetricasRepository;
import core_pymes.inversion.domain.Patrimonio;
import core_pymes.inversion.repository.PatrimonioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JPA: Patrimonio + MetricasFinanciera repositories")
class PatrimonioMetricasRepositoryTest extends AbstractJpaTest {

    @Autowired
    private PatrimonioRepository patrimonioRepository;

    @Autowired
    private MetricasRepository metricasRepository;

    private UUID tenantA;
    private UUID tenantB;

    @BeforeEach
    void setUp() {
        tenantA = UUID.randomUUID();
        tenantB = UUID.randomUUID();
    }

    @Nested
    @DisplayName("PatrimonioRepository")
    class PatrimonioTests {

        @Test
        @DisplayName("findByTenantId returns patrimony for tenant")
        void findByTenantId_returnsPatrimony() {
            em.persistFlushFind(Patrimonio.builder()
                    .tenantId(tenantA).initialCapital(new BigDecimal("50000.00"))
                    .startDate(LocalDate.of(2026, 1, 1)).build());
            em.clear();

            var result = patrimonioRepository.findByTenantId(tenantA);
            assertThat(result).isPresent();
            assertThat(result.get().getInitialCapital()).isEqualByComparingTo("50000.00");
        }

        @Test
        @DisplayName("findByTenantId returns empty for unknown tenant")
        void findByTenantId_returnsEmpty_forUnknown() {
            var result = patrimonioRepository.findByTenantId(UUID.randomUUID());
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("one patrimony per tenant (PK = tenant_id)")
        void onePerTenant() {
            em.persistFlushFind(Patrimonio.builder()
                    .tenantId(tenantA).initialCapital(BigDecimal.TEN).startDate(LocalDate.now()).build());
            // findByTenantId returns the single patrimony
            var result = patrimonioRepository.findByTenantId(tenantA);
            assertThat(result).isPresent();
            assertThat(result.get().getInitialCapital()).isEqualByComparingTo(BigDecimal.TEN);
        }

        @Test
        @DisplayName("initial capital with high precision")
        void highPrecision() {
            em.persistFlushFind(Patrimonio.builder()
                    .tenantId(tenantA).initialCapital(new BigDecimal("1234567.89"))
                    .startDate(LocalDate.of(2026, 1, 1)).build());
            em.clear();

            var loaded = patrimonioRepository.findByTenantId(tenantA).orElseThrow();
            assertThat(loaded.getInitialCapital()).isEqualByComparingTo("1234567.89");
        }

        @Test
        @DisplayName("notes can be null")
        void nullableNotes() {
            em.persistFlushFind(Patrimonio.builder()
                    .tenantId(tenantA).initialCapital(BigDecimal.ZERO)
                    .startDate(LocalDate.now()).notes(null).build());
            em.clear();

            var loaded = patrimonioRepository.findByTenantId(tenantA).orElseThrow();
            assertThat(loaded.getNotes()).isNull();
        }
    }

    @Nested
    @DisplayName("MetricasRepository")
    class MetricasTests {

        @Test
        @DisplayName("findByTenantIdAndPeriod returns metrics")
        void findByTenantIdAndPeriod_returnsMetrics() {
            var metricas = em.persistFlushFind(MetricasFinanciera.builder()
                    .tenantId(tenantA).period("2026-06")
                    .totalIncome(new BigDecimal("5000.00"))
                    .costOfGoods(new BigDecimal("2000.00"))
                    .operatingExpenses(new BigDecimal("800.00"))
                    .loanPayments(new BigDecimal("200.00"))
                    .totalExpenses(new BigDecimal("3000.00"))
                    .grossMargin(new BigDecimal("3000.00"))
                    .grossMarginPct(new BigDecimal("60.0000"))
                    .operatingMargin(new BigDecimal("2200.00"))
                    .operatingMarginPct(new BigDecimal("44.0000"))
                    .netMargin(new BigDecimal("2000.00"))
                    .netMarginPct(new BigDecimal("40.0000")).build());
            em.clear();

            var result = metricasRepository.findByTenantIdAndPeriod(tenantA, "2026-06");
            assertThat(result).isPresent();
            assertThat(result.get().getTotalIncome()).isEqualByComparingTo("5000.00");
            assertThat(result.get().getNetMarginPct()).isEqualByComparingTo("40.0000");
        }

        @Test
        @DisplayName("findByTenantIdAndPeriod returns empty for unknown period")
        void findByTenantIdAndPeriod_returnsEmpty_forUnknownPeriod() {
            var result = metricasRepository.findByTenantIdAndPeriod(tenantA, "2099-01");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("metrics are scoped by tenant and period")
        void scopedByTenantAndPeriod() {
            em.persistFlushFind(MetricasFinanciera.builder()
                    .tenantId(tenantA).period("2026-06")
                    .totalIncome(new BigDecimal("5000.00")).costOfGoods(BigDecimal.ZERO)
                    .operatingExpenses(BigDecimal.ZERO).loanPayments(BigDecimal.ZERO)
                    .totalExpenses(BigDecimal.ZERO).grossMargin(new BigDecimal("5000.00"))
                    .grossMarginPct(new BigDecimal("100.0000")).operatingMargin(new BigDecimal("5000.00"))
                    .operatingMarginPct(new BigDecimal("100.0000")).netMargin(new BigDecimal("5000.00"))
                    .netMarginPct(new BigDecimal("100.0000")).build());
            em.persistFlushFind(MetricasFinanciera.builder()
                    .tenantId(tenantB).period("2026-06")
                    .totalIncome(new BigDecimal("1000.00")).costOfGoods(BigDecimal.ZERO)
                    .operatingExpenses(BigDecimal.ZERO).loanPayments(BigDecimal.ZERO)
                    .totalExpenses(BigDecimal.ZERO).grossMargin(new BigDecimal("1000.00"))
                    .grossMarginPct(new BigDecimal("100.0000")).operatingMargin(new BigDecimal("1000.00"))
                    .operatingMarginPct(new BigDecimal("100.0000")).netMargin(new BigDecimal("1000.00"))
                    .netMarginPct(new BigDecimal("100.0000")).build());
            em.clear();

            var resultA = metricasRepository.findByTenantIdAndPeriod(tenantA, "2026-06");
            var resultB = metricasRepository.findByTenantIdAndPeriod(tenantB, "2026-06");
            assertThat(resultA).isPresent();
            assertThat(resultB).isPresent();
            assertThat(resultA.get().getTotalIncome()).isEqualByComparingTo("5000.00");
            assertThat(resultB.get().getTotalIncome()).isEqualByComparingTo("1000.00");
        }

        @Test
        @DisplayName("upsert pattern: same tenant+period overwrites previous metrics")
        void upsert_overwritesPrevious() {
            em.persistFlushFind(MetricasFinanciera.builder()
                    .tenantId(tenantA).period("2026-06")
                    .totalIncome(new BigDecimal("1000.00")).costOfGoods(BigDecimal.ZERO)
                    .operatingExpenses(BigDecimal.ZERO).loanPayments(BigDecimal.ZERO)
                    .totalExpenses(BigDecimal.ZERO).grossMargin(new BigDecimal("1000.00"))
                    .grossMarginPct(new BigDecimal("100.0000")).operatingMargin(new BigDecimal("1000.00"))
                    .operatingMarginPct(new BigDecimal("100.0000")).netMargin(new BigDecimal("1000.00"))
                    .netMarginPct(new BigDecimal("100.0000")).build());
            em.clear();

            // Simulate upsert: find existing, update, save
            var existing = metricasRepository.findByTenantIdAndPeriod(tenantA, "2026-06").orElseThrow();
            existing.setTotalIncome(new BigDecimal("9999.00"));
            existing.setNetMargin(new BigDecimal("7000.00"));
            em.persistAndFlush(existing);
            em.clear();

            var updated = metricasRepository.findByTenantIdAndPeriod(tenantA, "2026-06").orElseThrow();
            assertThat(updated.getTotalIncome()).isEqualByComparingTo("9999.00");
            assertThat(updated.getNetMargin()).isEqualByComparingTo("7000.00");
        }

        @Test
        @DisplayName("zero margins persist correctly")
        void zeroMargins() {
            em.persistFlushFind(MetricasFinanciera.builder()
                    .tenantId(tenantA).period("2026-07")
                    .totalIncome(BigDecimal.ZERO).costOfGoods(BigDecimal.ZERO)
                    .operatingExpenses(BigDecimal.ZERO).loanPayments(BigDecimal.ZERO)
                    .totalExpenses(BigDecimal.ZERO).grossMargin(BigDecimal.ZERO)
                    .grossMarginPct(BigDecimal.ZERO).operatingMargin(BigDecimal.ZERO)
                    .operatingMarginPct(BigDecimal.ZERO).netMargin(BigDecimal.ZERO)
                    .netMarginPct(BigDecimal.ZERO).build());
            em.clear();

            var result = metricasRepository.findByTenantIdAndPeriod(tenantA, "2026-07");
            assertThat(result).isPresent();
            assertThat(result.get().getTotalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.get().getGrossMarginPct()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("negative margins (loss) persist correctly")
        void negativeMargins() {
            em.persistFlushFind(MetricasFinanciera.builder()
                    .tenantId(tenantA).period("2026-08")
                    .totalIncome(new BigDecimal("1000.00")).costOfGoods(new BigDecimal("1500.00"))
                    .operatingExpenses(BigDecimal.ZERO).loanPayments(BigDecimal.ZERO)
                    .totalExpenses(new BigDecimal("1500.00")).grossMargin(new BigDecimal("-500.00"))
                    .grossMarginPct(new BigDecimal("-50.0000")).operatingMargin(new BigDecimal("-500.00"))
                    .operatingMarginPct(new BigDecimal("-50.0000")).netMargin(new BigDecimal("-500.00"))
                    .netMarginPct(new BigDecimal("-50.0000")).build());
            em.clear();

            var result = metricasRepository.findByTenantIdAndPeriod(tenantA, "2026-08");
            assertThat(result).isPresent();
            assertThat(result.get().getNetMargin()).isEqualByComparingTo("-500.00");
            assertThat(result.get().getNetMarginPct()).isEqualByComparingTo("-50.0000");
        }
    }
}

package core_pymes.jpa;

import core_pymes.prestamo.domain.EstadoPrestamo;
import core_pymes.prestamo.domain.PagoPrestamo;
import core_pymes.prestamo.domain.Prestamo;
import core_pymes.prestamo.repository.PagoPrestamoRepository;
import core_pymes.prestamo.repository.PrestamoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JPA: Prestamo + PagoPrestamo repositories")
class PrestamoRepositoryTest extends AbstractJpaTest {

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private PagoPrestamoRepository pagoRepository;

    private UUID tenantA;
    private UUID tenantB;
    private Prestamo prestamoA1;
    private Prestamo prestamoA2;

    @BeforeEach
    void setUp() {
        tenantA = UUID.randomUUID();
        tenantB = UUID.randomUUID();

        prestamoA1 = em.persistFlushFind(Prestamo.builder()
                .tenantId(tenantA).name("Préstamo Bancario Q1").lender("Banco XYZ")
                .amount(new BigDecimal("10000.00")).interestRate(new BigDecimal("0.02"))
                .termMonths(12).startDate(LocalDate.of(2026, 1, 1))
                .remainingBalance(new BigDecimal("8500.00")).status(EstadoPrestamo.ACTIVO).build());
        prestamoA2 = em.persistFlushFind(Prestamo.builder()
                .tenantId(tenantA).name("Préstamo Familiar").lender("Familiar")
                .amount(new BigDecimal("5000.00")).interestRate(BigDecimal.ZERO)
                .termMonths(6).startDate(LocalDate.of(2026, 3, 1))
                .remainingBalance(new BigDecimal("2000.00")).status(EstadoPrestamo.ACTIVO).build());
        em.persistFlushFind(Prestamo.builder()
                .tenantId(tenantB).name("Préstamo Otro").lender("Banco ABC")
                .amount(new BigDecimal("20000.00")).interestRate(new BigDecimal("0.03"))
                .termMonths(24).startDate(LocalDate.of(2026, 1, 1))
                .remainingBalance(new BigDecimal("18000.00")).status(EstadoPrestamo.ACTIVO).build());
        em.clear();
    }

    @Nested
    @DisplayName("PrestamoRepository")
    class PrestamoTests {

        @Test
        @DisplayName("findByTenantIdOrderByCreatedAtDesc returns loans for tenant")
        void findByTenantId_returnsTenantLoans() {
            var result = prestamoRepository.findByTenantIdOrderByCreatedAtDesc(tenantA);
            assertThat(result).hasSize(2)
                    .extracting(Prestamo::getName)
                    .containsExactlyInAnyOrder("Préstamo Bancario Q1", "Préstamo Familiar");
        }

        @Test
        @DisplayName("findByTenantIdOrderByCreatedAtDesc scoped by tenant")
        void findByTenantId_tenantScoped() {
            var resultB = prestamoRepository.findByTenantIdOrderByCreatedAtDesc(tenantB);
            assertThat(resultB).hasSize(1);
            assertThat(resultB.get(0).getName()).isEqualTo("Préstamo Otro");
        }

        @Test
        @DisplayName("EstadoPrestamo enum persists correctly")
        void enumMapping_roundTrip() {
            for (var estado : EstadoPrestamo.values()) {
                var prestamo = em.persistFlushFind(Prestamo.builder()
                        .tenantId(tenantA).name("Test " + estado.name())
                        .amount(BigDecimal.TEN).startDate(LocalDate.now())
                        .remainingBalance(BigDecimal.TEN).status(estado).build());
                em.clear();
                var loaded = em.find(Prestamo.class, prestamo.getId());
                assertThat(loaded.getStatus()).isEqualTo(estado);
            }
        }

        @Test
        @DisplayName("remainingBalance can reach zero")
        void zeroBalance() {
            var prestamo = em.persistFlushFind(Prestamo.builder()
                    .tenantId(tenantA).name("Fully Paid")
                    .amount(new BigDecimal("1000.00")).startDate(LocalDate.now())
                    .remainingBalance(BigDecimal.ZERO).status(EstadoPrestamo.PAGADO).build());
            em.clear();
            var loaded = em.find(Prestamo.class, prestamo.getId());
            assertThat(loaded.getRemainingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(loaded.getStatus()).isEqualTo(EstadoPrestamo.PAGADO);
        }

        @Test
        @DisplayName("soft delete excludes deleted loans")
        void softDelete_excludesDeleted() {
            prestamoRepository.deleteById(prestamoA1.getId());
            em.flush();
            em.clear();

            assertThat(prestamoRepository.findByTenantIdOrderByCreatedAtDesc(tenantA)).hasSize(1);
            assertThat(prestamoRepository.findById(prestamoA1.getId())).isEmpty();
        }

        @Test
        @DisplayName("nullable lender and notes persist as null")
        void nullableFields() {
            var prestamo = em.persistFlushFind(Prestamo.builder()
                    .tenantId(tenantA).name("Sin detalles")
                    .amount(BigDecimal.TEN).startDate(LocalDate.now())
                    .remainingBalance(BigDecimal.TEN).status(EstadoPrestamo.ACTIVO).build());
            em.clear();
            var loaded = em.find(Prestamo.class, prestamo.getId());
            assertThat(loaded.getLender()).isNull();
            assertThat(loaded.getInterestRate()).isNull();
            assertThat(loaded.getTermMonths()).isNull();
            assertThat(loaded.getNotes()).isNull();
        }
    }

    @Nested
    @DisplayName("PagoPrestamoRepository")
    class PagoTests {

        private PagoPrestamo pago1;
        private PagoPrestamo pago2;

        @BeforeEach
        void setUpPagos() {
            pago1 = em.persistFlushFind(PagoPrestamo.builder()
                    .loanId(prestamoA1.getId()).amount(new BigDecimal("500.00"))
                    .interestPaid(new BigDecimal("10.00")).principalPaid(new BigDecimal("490.00"))
                    .paymentDate(LocalDate.of(2026, 2, 1)).paymentMethod("TRANSFERENCIA").build());
            pago2 = em.persistFlushFind(PagoPrestamo.builder()
                    .loanId(prestamoA1.getId()).amount(new BigDecimal("500.00"))
                    .interestPaid(new BigDecimal("8.00")).principalPaid(new BigDecimal("492.00"))
                    .paymentDate(LocalDate.of(2026, 3, 1)).paymentMethod("EFECTIVO").build());
            em.clear();
        }

        @Test
        @DisplayName("findByLoanIdOrderByPaymentDateAsc returns payments for loan")
        void findByLoanId_returnsPayments() {
            var result = pagoRepository.findByLoanIdOrderByPaymentDateAsc(prestamoA1.getId());
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getPaymentDate()).isBefore(result.get(1).getPaymentDate());
        }

        @Test
        @DisplayName("payments are scoped to their loan")
        void payments_scopedToLoan() {
            var resultOtherLoan = pagoRepository.findByLoanIdOrderByPaymentDateAsc(prestamoA2.getId());
            assertThat(resultOtherLoan).isEmpty();
        }

        @Test
        @DisplayName("soft delete on loan does not affect payments")
        void loanDelete_doesNotAffectPayments() {
            prestamoRepository.deleteById(prestamoA1.getId());
            em.flush();
            em.clear();

            // soft delete only sets is_active=false, payments are still linked
            var remainingPayments = pagoRepository.findByLoanIdOrderByPaymentDateAsc(prestamoA1.getId());
            assertThat(remainingPayments).hasSize(2);
            // loan is hidden by @Where(clause = "is_active = true")
            assertThat(prestamoRepository.findById(prestamoA1.getId())).isEmpty();
        }

        @Test
        @DisplayName("interest and principal payments can be zero")
        void zeroPayments() {
            var pago = em.persistFlushFind(PagoPrestamo.builder()
                    .loanId(prestamoA2.getId()).amount(new BigDecimal("100.00"))
                    .interestPaid(BigDecimal.ZERO).principalPaid(new BigDecimal("100.00"))
                    .paymentDate(LocalDate.of(2026, 4, 1)).paymentMethod("TARJETA").build());
            em.clear();
            var loaded = em.find(PagoPrestamo.class, pago.getId());
            assertThat(loaded.getInterestPaid()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(loaded.getPrincipalPaid()).isEqualByComparingTo("100.00");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("interest rate with 4 decimal precision")
        void interestRate_precision() {
            var prestamo = em.persistFlushFind(Prestamo.builder()
                    .tenantId(tenantA).name("Alta precisión")
                    .amount(new BigDecimal("50000.00")).interestRate(new BigDecimal("0.0175"))
                    .termMonths(36).startDate(LocalDate.now())
                    .remainingBalance(new BigDecimal("45000.00"))
                    .status(EstadoPrestamo.ACTIVO).build());
            em.clear();
            var loaded = em.find(Prestamo.class, prestamo.getId());
            assertThat(loaded.getInterestRate()).isEqualByComparingTo("0.0175");
        }

        @Test
        @DisplayName("large loan amount persists correctly")
        void largeAmount() {
            var prestamo = em.persistFlushFind(Prestamo.builder()
                    .tenantId(tenantA).name("Hipotecario")
                    .amount(new BigDecimal("500000.00")).interestRate(new BigDecimal("0.008"))
                    .termMonths(120).startDate(LocalDate.now())
                    .remainingBalance(new BigDecimal("490000.00"))
                    .status(EstadoPrestamo.ACTIVO).build());
            em.clear();
            var loaded = em.find(Prestamo.class, prestamo.getId());
            assertThat(loaded.getAmount()).isEqualByComparingTo("500000.00");
        }
    }
}

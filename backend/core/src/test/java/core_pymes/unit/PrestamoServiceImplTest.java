package core_pymes.unit;

import core_pymes.prestamo.domain.EstadoPrestamo;
import core_pymes.prestamo.domain.PagoPrestamo;
import core_pymes.prestamo.domain.Prestamo;
import core_pymes.prestamo.dto.PagoPrestamoRequest;
import core_pymes.prestamo.dto.PrestamoRequest;
import core_pymes.prestamo.event.PrestamoCreadoEvent;
import core_pymes.prestamo.repository.PagoPrestamoRepository;
import core_pymes.prestamo.repository.PrestamoRepository;
import core_pymes.prestamo.service.impl.PrestamoServiceImpl;
import core_pymes.common.exception.custom.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrestamoServiceImplTest {

    @Mock PrestamoRepository prestamoRepository;
    @Mock PagoPrestamoRepository pagoRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks PrestamoServiceImpl service;

    private Prestamo prestamo(UUID tenantId, BigDecimal amount, BigDecimal remaining) {
        return Prestamo.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .name("Prestamo Banco")
                .lender("Banco")
                .amount(amount)
                .interestRate(BigDecimal.ZERO)
                .termMonths(12)
                .startDate(LocalDate.of(2026, 1, 1))
                .remainingBalance(remaining)
                .status(EstadoPrestamo.ACTIVO)
                .createdAt(ZonedDateTime.now())
                .build();
    }

    @Test
    void findAll_returnsLoansForTenant() {
        var tenantId = UUID.randomUUID();
        var prestamo = prestamo(tenantId, new BigDecimal("1000"), new BigDecimal("1000"));
        when(prestamoRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(List.of(prestamo));

        var result = service.findAll(tenantId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().nombre()).isEqualTo("Prestamo Banco");
        assertThat(result.getFirst().estado()).isEqualTo("ACTIVO");
    }

    @Test
    void create_savesActiveLoanAndPublishesEvent() {
        var tenantId = UUID.randomUUID();
        var request = new PrestamoRequest(tenantId, "Prestamo Banco", "Banco",
                new BigDecimal("5000"), BigDecimal.ZERO, 12, LocalDate.of(2026, 1, 1));
        when(prestamoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.create(request);

        assertThat(result.saldoPendiente()).isEqualByComparingTo("5000");
        assertThat(result.estado()).isEqualTo("ACTIVO");
        var captor = ArgumentCaptor.forClass(PrestamoCreadoEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().monto()).isEqualByComparingTo("5000");
    }

    @Test
    void update_withWrongTenant_throws404() {
        var tenantId = UUID.randomUUID();
        var prestamo = prestamo(UUID.randomUUID(), new BigDecimal("1000"), new BigDecimal("1000"));
        when(prestamoRepository.findById(prestamo.getId())).thenReturn(Optional.of(prestamo));

        var request = new PrestamoRequest(tenantId, "Nuevo", null,
                new BigDecimal("1000"), null, null, LocalDate.of(2026, 1, 1));

        assertThatThrownBy(() -> service.update(prestamo.getId(), tenantId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Prestamo not found");
    }

    @Test
    void delete_withUnknownId_throws404() {
        when(prestamoRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Prestamo not found");
        verify(prestamoRepository, never()).delete(any());
    }

    @Test
    void registrarPago_reducesRemainingBalance() {
        var tenantId = UUID.randomUUID();
        var prestamo = prestamo(tenantId, new BigDecimal("5000"), new BigDecimal("3000"));
        when(prestamoRepository.findById(prestamo.getId())).thenReturn(Optional.of(prestamo));
        var pago = PagoPrestamo.builder()
                .id(UUID.randomUUID())
                .loanId(prestamo.getId())
                .amount(new BigDecimal("1000"))
                .interestPaid(BigDecimal.ZERO)
                .principalPaid(new BigDecimal("1000"))
                .paymentDate(LocalDate.of(2026, 2, 1))
                .paymentMethod("EFECTIVO")
                .createdAt(ZonedDateTime.now())
                .build();
        when(pagoRepository.save(any())).thenReturn(pago);

        var result = service.registrarPago(prestamo.getId(), tenantId,
                new PagoPrestamoRequest(new BigDecimal("1000"), BigDecimal.ZERO, new BigDecimal("1000"),
                        LocalDate.of(2026, 2, 1), "EFECTIVO"));

        assertThat(result.monto()).isEqualByComparingTo("1000");
        assertThat(prestamo.getRemainingBalance()).isEqualByComparingTo("2000");
        assertThat(prestamo.getStatus()).isEqualTo(EstadoPrestamo.ACTIVO);
    }

    @Test
    void registrarPago_marksLoanPaidWhenBalanceReachesZero() {
        var tenantId = UUID.randomUUID();
        var prestamo = prestamo(tenantId, new BigDecimal("5000"), new BigDecimal("500"));
        when(prestamoRepository.findById(prestamo.getId())).thenReturn(Optional.of(prestamo));
        var pago = PagoPrestamo.builder()
                .id(UUID.randomUUID())
                .loanId(prestamo.getId())
                .amount(new BigDecimal("500"))
                .interestPaid(BigDecimal.ZERO)
                .principalPaid(new BigDecimal("500"))
                .paymentDate(LocalDate.of(2026, 3, 1))
                .paymentMethod("TRANSFERENCIA")
                .createdAt(ZonedDateTime.now())
                .build();
        when(pagoRepository.save(any())).thenReturn(pago);

        service.registrarPago(prestamo.getId(), tenantId,
                new PagoPrestamoRequest(new BigDecimal("500"), BigDecimal.ZERO, new BigDecimal("500"),
                        LocalDate.of(2026, 3, 1), "TRANSFERENCIA"));

        assertThat(prestamo.getRemainingBalance()).isEqualByComparingTo("0");
        assertThat(prestamo.getStatus()).isEqualTo(EstadoPrestamo.PAGADO);
    }

    @Test
    void findPagos_returnsPaymentsForLoan() {
        var tenantId = UUID.randomUUID();
        var prestamo = prestamo(tenantId, new BigDecimal("1000"), new BigDecimal("1000"));
        when(prestamoRepository.findById(prestamo.getId())).thenReturn(Optional.of(prestamo));
        var pago = PagoPrestamo.builder()
                .id(UUID.randomUUID())
                .loanId(prestamo.getId())
                .amount(new BigDecimal("100"))
                .interestPaid(BigDecimal.ZERO)
                .principalPaid(new BigDecimal("100"))
                .paymentDate(LocalDate.of(2026, 2, 1))
                .paymentMethod("EFECTIVO")
                .createdAt(ZonedDateTime.now())
                .build();
        when(pagoRepository.findByLoanIdOrderByPaymentDateAsc(prestamo.getId())).thenReturn(List.of(pago));

        var result = service.findPagos(prestamo.getId(), tenantId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().monto()).isEqualByComparingTo("100");
    }
}
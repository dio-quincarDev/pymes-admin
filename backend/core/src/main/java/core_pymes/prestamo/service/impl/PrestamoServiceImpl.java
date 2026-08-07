package core_pymes.prestamo.service.impl;

import core_pymes.prestamo.domain.EstadoPrestamo;
import core_pymes.prestamo.domain.PagoPrestamo;
import core_pymes.prestamo.domain.Prestamo;
import core_pymes.prestamo.dto.PagoPrestamoRequest;
import core_pymes.prestamo.dto.PagoPrestamoResponse;
import core_pymes.prestamo.dto.PrestamoRequest;
import core_pymes.prestamo.dto.PrestamoResponse;
import core_pymes.prestamo.event.PrestamoCreadoEvent;
import core_pymes.prestamo.repository.PagoPrestamoRepository;
import core_pymes.prestamo.repository.PrestamoRepository;
import core_pymes.prestamo.service.PrestamoService;
import core_pymes.common.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrestamoServiceImpl implements PrestamoService {

    private final PrestamoRepository prestamoRepository;
    private final PagoPrestamoRepository pagoRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "prestamos", key = "#tenantId")
    public List<PrestamoResponse> findAll(UUID tenantId) {
        return prestamoRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "prestamos", key = "#id")
    public PrestamoResponse findById(UUID id, UUID tenantId) {
        return toResponse(getPrestamo(id, tenantId));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "prestamos", allEntries = true)
    public PrestamoResponse create(PrestamoRequest request) {
        var prestamo = Prestamo.builder()
                .tenantId(request.tenantId())
                .name(request.nombre())
                .lender(request.prestamista())
                .amount(request.monto())
                .interestRate(request.tasaInteres() != null ? request.tasaInteres() : BigDecimal.ZERO)
                .termMonths(request.plazoMeses())
                .startDate(request.fechaInicio())
                .remainingBalance(request.monto())
                .status(EstadoPrestamo.ACTIVO)
                .build();
        prestamo = prestamoRepository.save(prestamo);
        eventPublisher.publishEvent(new PrestamoCreadoEvent(
                prestamo.getTenantId(), prestamo.getStartDate(), prestamo.getAmount()));
        return toResponse(prestamo);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "prestamos", allEntries = true)
    public PrestamoResponse update(UUID id, UUID tenantId, PrestamoRequest request) {
        var prestamo = getPrestamo(id, tenantId);
        prestamo.setName(request.nombre());
        prestamo.setLender(request.prestamista());
        prestamo.setInterestRate(request.tasaInteres() != null ? request.tasaInteres() : BigDecimal.ZERO);
        prestamo.setTermMonths(request.plazoMeses());
        prestamo = prestamoRepository.save(prestamo);
        return toResponse(prestamo);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "prestamos", allEntries = true)
    public void delete(UUID id, UUID tenantId) {
        var prestamo = getPrestamo(id, tenantId);
        prestamoRepository.delete(prestamo);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "prestamos", allEntries = true)
    public PagoPrestamoResponse registrarPago(UUID id, UUID tenantId, PagoPrestamoRequest request) {
        var prestamo = getPrestamo(id, tenantId);

        var pago = PagoPrestamo.builder()
                .loanId(prestamo.getId())
                .amount(request.monto())
                .interestPaid(request.interesPagado() != null ? request.interesPagado() : BigDecimal.ZERO)
                .principalPaid(request.capitalPagado() != null ? request.capitalPagado() : request.monto())
                .paymentDate(request.fechaPago())
                .paymentMethod(request.metodoPago())
                .build();
        pago = pagoRepository.save(pago);

        prestamo.setRemainingBalance(prestamo.getRemainingBalance().subtract(pago.getPrincipalPaid()));
        if (prestamo.getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            prestamo.setRemainingBalance(BigDecimal.ZERO);
            prestamo.setStatus(EstadoPrestamo.PAGADO);
        }
        prestamoRepository.save(prestamo);

        return toPagoResponse(pago);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoPrestamoResponse> findPagos(UUID id, UUID tenantId) {
        getPrestamo(id, tenantId);
        return pagoRepository.findByLoanIdOrderByPaymentDateAsc(id).stream()
                .map(this::toPagoResponse)
                .toList();
    }

    private Prestamo getPrestamo(UUID id, UUID tenantId) {
        var prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prestamo not found: " + id));
        if (!prestamo.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Prestamo not found: " + id);
        }
        return prestamo;
    }

    private PrestamoResponse toResponse(Prestamo p) {
        return new PrestamoResponse(
                p.getId(), p.getTenantId(), p.getName(), p.getLender(),
                p.getAmount(), p.getInterestRate(), p.getTermMonths(),
                p.getStartDate(), p.getRemainingBalance(), p.getStatus().name(),
                p.getNotes(), p.getCreatedAt());
    }

    private PagoPrestamoResponse toPagoResponse(PagoPrestamo pg) {
        return new PagoPrestamoResponse(
                pg.getId(), pg.getLoanId(), pg.getAmount(),
                pg.getInterestPaid(), pg.getPrincipalPaid(),
                pg.getPaymentDate(), pg.getPaymentMethod(), pg.getCreatedAt());
    }
}

package core_pymes.venta.service.impl;

import core_pymes.venta.domain.DailySalePayment;
import core_pymes.venta.domain.MetodoPago;
import core_pymes.venta.domain.VentaDiaria;
import core_pymes.venta.dto.VentaPagoRequest;
import core_pymes.venta.dto.VentaPagoResponse;
import core_pymes.venta.dto.VentaRequest;
import core_pymes.venta.dto.VentaResponse;
import core_pymes.venta.event.VentaCreadaEvent;
import core_pymes.venta.repository.VentaRepository;
import core_pymes.venta.service.VentaService;
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
public class VentaServiceImpl implements VentaService {

    private final VentaRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "ventas", key = "#tenantId")
    public List<VentaResponse> findAll(UUID tenantId) {
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "ventas", key = "#id")
    public VentaResponse findById(UUID id, UUID tenantId) {
        return toResponse(getVenta(id, tenantId));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "ventas", allEntries = true)
    public VentaResponse create(VentaRequest request) {
        BigDecimal grossAmount = resolveGrossAmount(request);
        var venta = VentaDiaria.builder()
                .tenantId(request.tenantId())
                .saleDate(request.fecha())
                .grossAmount(grossAmount)
                .description(request.descripcion())
                .build();
        applyDesglose(venta, request.desglose());
        venta = repository.save(venta);
        eventPublisher.publishEvent(new VentaCreadaEvent(
                venta.getTenantId(), venta.getSaleDate(), venta.getGrossAmount()));
        return toResponse(venta);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "ventas", allEntries = true)
    public VentaResponse update(UUID id, UUID tenantId, VentaRequest request) {
        var venta = getVenta(id, tenantId);
        venta.setSaleDate(request.fecha());
        venta.setGrossAmount(resolveGrossAmount(request));
        venta.setDescription(request.descripcion());
        venta.getPayments().clear();
        applyDesglose(venta, request.desglose());
        venta = repository.save(venta);
        return toResponse(venta);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "ventas", allEntries = true)
    public void delete(UUID id, UUID tenantId) {
        var venta = getVenta(id, tenantId);
        repository.delete(venta);
    }

    private BigDecimal resolveGrossAmount(VentaRequest request) {
        List<VentaPagoRequest> desglose = request.desglose();
        if (desglose != null && !desglose.isEmpty()) {
            return desglose.stream()
                    .map(VentaPagoRequest::monto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        if (request.montoBruto() == null || request.montoBruto().signum() <= 0) {
            throw new IllegalArgumentException("montoBruto es obligatorio cuando no hay desglose");
        }
        return request.montoBruto();
    }

    private void applyDesglose(VentaDiaria venta, List<VentaPagoRequest> desglose) {
        if (desglose == null) return;
        for (VentaPagoRequest p : desglose) {
            var payment = DailySalePayment.builder()
                    .venta(venta)
                    .metodo(p.metodo())
                    .monto(p.monto())
                    .build();
            venta.getPayments().add(payment);
        }
    }

    private VentaDiaria getVenta(UUID id, UUID tenantId) {
        var venta = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VentaDiaria not found: " + id));
        if (!venta.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("VentaDiaria not found: " + id);
        }
        return venta;
    }

    private VentaResponse toResponse(VentaDiaria v) {
        List<VentaPagoResponse> desglose = v.getPayments().stream()
                .map(p -> new VentaPagoResponse(p.getId(), p.getMetodo(), p.getMonto()))
                .toList();
        return new VentaResponse(
                v.getId(), v.getTenantId(), v.getSaleDate(),
                v.getGrossAmount(), v.getDescription(), desglose, v.getCreatedAt());
    }
}

package core_pymes.venta.service.impl;

import core_pymes.venta.domain.VentaDiaria;
import core_pymes.venta.dto.VentaRequest;
import core_pymes.venta.dto.VentaResponse;
import core_pymes.venta.event.VentaCreadaEvent;
import core_pymes.venta.repository.VentaRepository;
import core_pymes.venta.service.VentaService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        var venta = VentaDiaria.builder()
                .tenantId(request.tenantId())
                .saleDate(request.fecha())
                .grossAmount(request.montoBruto())
                .description(request.descripcion())
                .build();
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
        venta.setGrossAmount(request.montoBruto());
        venta.setDescription(request.descripcion());
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

    private VentaDiaria getVenta(UUID id, UUID tenantId) {
        var venta = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("VentaDiaria not found: " + id));
        if (!venta.getTenantId().equals(tenantId)) {
            throw new EntityNotFoundException("VentaDiaria not found: " + id);
        }
        return venta;
    }

    private VentaResponse toResponse(VentaDiaria v) {
        return new VentaResponse(
                v.getId(), v.getTenantId(), v.getSaleDate(),
                v.getGrossAmount(), v.getDescription(), v.getCreatedAt());
    }
}

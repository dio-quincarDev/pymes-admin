package core_pymes.gasto.service.impl;

import core_pymes.gasto.domain.CategoriaGasto;
import core_pymes.gasto.domain.GastoOperativo;
import core_pymes.gasto.dto.GastoRequest;
import core_pymes.gasto.dto.GastoResponse;
import core_pymes.gasto.event.GastoCreadoEvent;
import core_pymes.gasto.repository.GastoRepository;
import core_pymes.gasto.service.GastoService;
import core_pymes.common.exception.custom.ResourceNotFoundException;
import core_pymes.common.service.RecomputeDebounceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GastoServiceImpl implements GastoService {

    private final GastoRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final RecomputeDebounceService recomputeService;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "gastos", key = "#tenantId")
    public List<GastoResponse> findAll(UUID tenantId) {
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "gastos", key = "#id")
    public GastoResponse findById(UUID id, UUID tenantId) {
        return toResponse(getGasto(id, tenantId));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "gastos", allEntries = true)
    public GastoResponse create(GastoRequest request) {
        var gasto = GastoOperativo.builder()
                .tenantId(request.tenantId())
                .category(CategoriaGasto.valueOf(request.categoria()))
                .description(request.descripcion())
                .amount(request.monto())
                .expenseDate(request.fecha())
                .paymentMethod(request.metodoPago())
                .build();
        gasto = repository.save(gasto);
        eventPublisher.publishEvent(new GastoCreadoEvent(
                gasto.getTenantId(), gasto.getExpenseDate(), gasto.getAmount()));
        return toResponse(gasto);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "gastos", allEntries = true)
    public GastoResponse update(UUID id, UUID tenantId, GastoRequest request) {
        var gasto = getGasto(id, tenantId);
        var periodoAnterior = YearMonth.from(gasto.getExpenseDate()).toString();
        gasto.setCategory(CategoriaGasto.valueOf(request.categoria()));
        gasto.setDescription(request.descripcion());
        gasto.setAmount(request.monto());
        gasto.setExpenseDate(request.fecha());
        gasto.setPaymentMethod(request.metodoPago());
        gasto = repository.save(gasto);
        var periodoNuevo = YearMonth.from(gasto.getExpenseDate()).toString();
        recomputeService.markMetricsDirty(tenantId, periodoAnterior);
        recomputeService.markMetricsDirty(tenantId, periodoNuevo);
        recomputeService.markAnalyticsDirty(tenantId, periodoAnterior);
        recomputeService.markAnalyticsDirty(tenantId, periodoNuevo);
        return toResponse(gasto);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "gastos", allEntries = true)
    public void delete(UUID id, UUID tenantId) {
        var gasto = getGasto(id, tenantId);
        var periodo = YearMonth.from(gasto.getExpenseDate()).toString();
        repository.delete(gasto);
        recomputeService.markMetricsDirty(tenantId, periodo);
        recomputeService.markAnalyticsDirty(tenantId, periodo);
    }

    private GastoOperativo getGasto(UUID id, UUID tenantId) {
        var gasto = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GastoOperativo not found: " + id));
        if (!gasto.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("GastoOperativo not found: " + id);
        }
        return gasto;
    }

    private GastoResponse toResponse(GastoOperativo g) {
        return new GastoResponse(
                g.getId(), g.getTenantId(), g.getCategory().name(),
                g.getDescription(), g.getAmount(), g.getExpenseDate(),
                g.getPaymentMethod(), g.getCreatedAt());
    }
}

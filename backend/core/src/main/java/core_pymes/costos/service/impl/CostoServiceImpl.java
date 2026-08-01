package core_pymes.costos.service.impl;

import core_pymes.accounting.repository.MetricasRepository;
import core_pymes.common.exception.custom.ResourceNotFoundException;
import core_pymes.costos.domain.Collaborador;
import core_pymes.costos.domain.ConfigLaboral;
import core_pymes.costos.domain.GastoFijoRecurrente;
import core_pymes.costos.domain.TipoPago;
import core_pymes.costos.dto.*;
import core_pymes.costos.event.CostoStructureChangedEvent;
import core_pymes.costos.repository.CollaboradorRepository;
import core_pymes.costos.repository.ConfigLaboralRepository;
import core_pymes.costos.repository.GastoFijoRepository;
import core_pymes.costos.service.CostoService;
import core_pymes.gasto.domain.CategoriaGasto;
import core_pymes.venta.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CostoServiceImpl implements CostoService {

    private static final int DIAS_LABORALES_DEFAULT = 26;
    private static final BigDecimal SEMANAL_FACTOR = new BigDecimal("4.33");

    private final CollaboradorRepository collaboradorRepository;
    private final GastoFijoRepository gastoFijoRepository;
    private final ConfigLaboralRepository configLaboralRepository;
    private final VentaRepository ventaRepository;
    private final MetricasRepository metricasRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "costos", key = "'collaboradores:' + #tenantId")
    public List<CollaboradorResponse> findAllCollaboradores(UUID tenantId) {
        return collaboradorRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "costos", key = "'collaborador:' + #id")
    public CollaboradorResponse findCollaborador(UUID id, UUID tenantId) {
        return toResponse(getCollaborador(id, tenantId));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "costos", allEntries = true)
    public CollaboradorResponse crearCollaborador(CollaboradorRequest request) {
        var colaborador = collaboradorRepository.save(Collaborador.builder()
                .tenantId(request.tenantId())
                .nombre(request.nombre())
                .tipoPago(TipoPago.valueOf(request.tipoPago()))
                .monto(request.monto())
                .build());
        publishChanged(colaborador.getTenantId());
        return toResponse(colaborador);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "costos", allEntries = true)
    public CollaboradorResponse actualizarCollaborador(UUID id, UUID tenantId, CollaboradorRequest request) {
        var colaborador = getCollaborador(id, tenantId);
        colaborador.setNombre(request.nombre());
        colaborador.setTipoPago(TipoPago.valueOf(request.tipoPago()));
        colaborador.setMonto(request.monto());
        colaborador = collaboradorRepository.save(colaborador);
        publishChanged(colaborador.getTenantId());
        return toResponse(colaborador);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "costos", allEntries = true)
    public void eliminarCollaborador(UUID id, UUID tenantId) {
        var colaborador = getCollaborador(id, tenantId);
        collaboradorRepository.delete(colaborador);
        publishChanged(colaborador.getTenantId());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "costos", key = "'gastos-fijos:' + #tenantId")
    public List<GastoFijoResponse> findAllGastosFijos(UUID tenantId) {
        return gastoFijoRepository.findByTenantIdOrderByCategoriaAsc(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "costos", key = "'gasto-fijo:' + #id")
    public GastoFijoResponse findGastoFijo(UUID id, UUID tenantId) {
        return toResponse(getGastoFijo(id, tenantId));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "costos", allEntries = true)
    public GastoFijoResponse crearGastoFijo(GastoFijoRequest request) {
        var gastoFijo = gastoFijoRepository.save(GastoFijoRecurrente.builder()
                .tenantId(request.tenantId())
                .categoria(CategoriaGasto.valueOf(request.categoria()))
                .monto(request.monto())
                .descripcion(request.descripcion())
                .diaEjecucion(request.diaEjecucion())
                .metodoPago(request.metodoPago())
                .build());
        publishChanged(gastoFijo.getTenantId());
        return toResponse(gastoFijo);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "costos", allEntries = true)
    public GastoFijoResponse actualizarGastoFijo(UUID id, UUID tenantId, GastoFijoRequest request) {
        var gastoFijo = getGastoFijo(id, tenantId);
        gastoFijo.setCategoria(CategoriaGasto.valueOf(request.categoria()));
        gastoFijo.setMonto(request.monto());
        gastoFijo.setDescripcion(request.descripcion());
        gastoFijo.setDiaEjecucion(request.diaEjecucion());
        gastoFijo.setMetodoPago(request.metodoPago());
        gastoFijo = gastoFijoRepository.save(gastoFijo);
        publishChanged(gastoFijo.getTenantId());
        return toResponse(gastoFijo);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "costos", allEntries = true)
    public void eliminarGastoFijo(UUID id, UUID tenantId) {
        var gastoFijo = getGastoFijo(id, tenantId);
        gastoFijoRepository.delete(gastoFijo);
        publishChanged(gastoFijo.getTenantId());
    }

    @Override
    @Transactional(readOnly = true)
    public ConfigLaboralResponse obtenerConfiguracion(UUID tenantId) {
        return new ConfigLaboralResponse(tenantId, getDiasLaborales(tenantId));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "costos", allEntries = true)
    public ConfigLaboralResponse actualizarConfiguracion(UUID tenantId, ConfigLaboralRequest request) {
        var config = configLaboralRepository.findById(tenantId)
                .orElse(ConfigLaboral.builder().tenantId(tenantId).build());
        config.setDiasLaborales(request.diasLaborales());
        configLaboralRepository.save(config);
        publishChanged(tenantId);
        return new ConfigLaboralResponse(tenantId, config.getDiasLaborales());
    }

    @Override
    @Transactional(readOnly = true)
    public CostoDiarioResponse calcularDiario(UUID tenantId) {
        var diasLaborales = getDiasLaborales(tenantId);

        var costoFijoMensual = gastoFijoRepository.findByTenantIdOrderByCategoriaAsc(tenantId).stream()
                .map(GastoFijoRecurrente::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var costoSalariosMensual = collaboradorRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(c -> c.getMonto().multiply(mensualizarFactor(c.getTipoPago(), diasLaborales)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var costoOperativoMensual = costoFijoMensual.add(costoSalariosMensual);
        var costoOperativoDiario = costoOperativoMensual.divide(
                BigDecimal.valueOf(diasLaborales), 2, RoundingMode.HALF_UP);

        var hoy = LocalDate.now(ZoneOffset.UTC);
        var ventasHoy = ventaRepository.findByTenantIdAndSaleDateBetweenOrderBySaleDateDesc(tenantId, hoy, hoy).stream()
                .map(v -> v.getGrossAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var cogsPct = cogsPorcentajePromedio(tenantId);
        var gananciaRealEstimada = ventasHoy
                .subtract(ventasHoy.multiply(cogsPct))
                .subtract(costoOperativoDiario);

        return new CostoDiarioResponse(
                costoFijoMensual, BigDecimal.ZERO, costoSalariosMensual,
                costoOperativoMensual, diasLaborales, costoOperativoDiario,
                ventasHoy, gananciaRealEstimada);
    }

    private int getDiasLaborales(UUID tenantId) {
        return configLaboralRepository.findById(tenantId)
                .map(ConfigLaboral::getDiasLaborales)
                .orElse(DIAS_LABORALES_DEFAULT);
    }

    private BigDecimal mensualizarFactor(TipoPago tipoPago, int diasLaborales) {
        return switch (tipoPago) {
            case DIARIO -> BigDecimal.valueOf(diasLaborales);
            case SEMANAL -> SEMANAL_FACTOR;
            case QUINCENAL -> BigDecimal.valueOf(2);
            case MENSUAL -> BigDecimal.ONE;
        };
    }

    // ponytail: cogs% del mes actual, 0 si no hay métricas. Promedio histórico si se necesita precisión.
    private BigDecimal cogsPorcentajePromedio(UUID tenantId) {
        var periodo = YearMonth.now(ZoneOffset.UTC).toString();
        return metricasRepository.findByTenantIdAndPeriod(tenantId, periodo)
                .filter(m -> m.getTotalIncome() != null && m.getTotalIncome().signum() > 0)
                .map(m -> m.getCostOfGoods().divide(m.getTotalIncome(), 4, RoundingMode.HALF_UP))
                .orElse(BigDecimal.ZERO);
    }

    private void publishChanged(UUID tenantId) {
        eventPublisher.publishEvent(new CostoStructureChangedEvent(
                tenantId, YearMonth.now(ZoneOffset.UTC).toString()));
    }

    private Collaborador getCollaborador(UUID id, UUID tenantId) {
        var colaborador = collaboradorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collaborador not found: " + id));
        if (!colaborador.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Collaborador not found: " + id);
        }
        return colaborador;
    }

    private GastoFijoRecurrente getGastoFijo(UUID id, UUID tenantId) {
        var gastoFijo = gastoFijoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GastoFijoRecurrente not found: " + id));
        if (!gastoFijo.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("GastoFijoRecurrente not found: " + id);
        }
        return gastoFijo;
    }

    private CollaboradorResponse toResponse(Collaborador c) {
        return new CollaboradorResponse(
                c.getId(), c.getTenantId(), c.getNombre(),
                c.getTipoPago().name(), c.getMonto(), c.getActivo(), c.getCreatedAt());
    }

    private GastoFijoResponse toResponse(GastoFijoRecurrente g) {
        return new GastoFijoResponse(
                g.getId(), g.getTenantId(), g.getCategoria().name(),
                g.getMonto(), g.getDescripcion(), g.getDiaEjecucion(), g.getMetodoPago(), g.getActivo());
    }
}

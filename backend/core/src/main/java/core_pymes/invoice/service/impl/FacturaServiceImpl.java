package core_pymes.invoice.service.impl;

import core_pymes.invoice.domain.Factura;
import core_pymes.invoice.domain.ItemFactura;
import core_pymes.invoice.domain.Proveedor;
import core_pymes.invoice.dto.*;
import core_pymes.invoice.event.FacturaCreadaEvent;
import core_pymes.invoice.event.FacturaPagadaEvent;
import core_pymes.invoice.mapper.FacturaMapper;
import core_pymes.invoice.repository.FacturaRepository;
import core_pymes.invoice.repository.ProveedorRepository;
import core_pymes.invoice.service.FacturaService;
import core_pymes.invoice.service.InvoiceCalculator;
import core_pymes.common.exception.custom.InvalidInputException;
import core_pymes.common.exception.custom.ResourceNotFoundException;
import core_pymes.product.domain.Presentacion;
import core_pymes.product.repository.PresentacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacturaServiceImpl implements FacturaService {

    private final ProveedorRepository proveedorRepository;
    private final FacturaRepository facturaRepository;
    private final FacturaMapper mapper;
    private final ApplicationEventPublisher eventPublisher;
    private final JdbcTemplate jdbc;
    private final PresentacionRepository presentacionRepository;

    // -- Proveedores --

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "proveedores", key = "#tenantId")
    public List<ProveedorResponse> findAllProveedores(UUID tenantId) {
        return mapper.toProveedorResponseList(proveedorRepository.findByTenantId(tenantId));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "proveedores", key = "#id")
    public ProveedorResponse findProveedor(UUID id, UUID tenantId) {
        return mapper.toProveedorResponse(getProveedor(id, tenantId));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "proveedores", allEntries = true)
    public ProveedorResponse createProveedor(ProveedorRequest request) {
        var proveedor = Proveedor.builder()
                .tenantId(request.tenantId())
                .name(request.name())
                .contactName(request.contactName())
                .contactPhone(request.contactPhone())
                .contactEmail(request.contactEmail())
                .build();
        proveedor = proveedorRepository.save(proveedor);
        return mapper.toProveedorResponse(proveedor);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "proveedores", allEntries = true)
    public ProveedorResponse updateProveedor(UUID id, UUID tenantId, ProveedorRequest request) {
        var proveedor = getProveedor(id, tenantId);
        proveedor.setName(request.name());
        proveedor.setContactName(request.contactName());
        proveedor.setContactPhone(request.contactPhone());
        proveedor.setContactEmail(request.contactEmail());
        proveedor = proveedorRepository.save(proveedor);
        return mapper.toProveedorResponse(proveedor);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "proveedores", allEntries = true)
    public void deleteProveedor(UUID id, UUID tenantId) {
        getProveedor(id, tenantId);
        proveedorRepository.deleteById(id);
    }

    // -- Facturas --

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "facturas", key = "#tenantId")
    public List<FacturaResponse> findAllFacturas(UUID tenantId) {
        return facturaRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(f -> mapper.toResponse(f, mapper.toItemResponseList(f.getItems())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "facturas", key = "#id")
    public FacturaResponse findFactura(UUID id, UUID tenantId) {
        var factura = getFactura(id, tenantId);
        return mapper.toResponse(factura, mapper.toItemResponseList(factura.getItems()));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "facturas", allEntries = true)
    public FacturaResponse createFactura(FacturaRequest request) {
        Proveedor proveedor = null;
        if (request.proveedorId() != null) {
            proveedor = proveedorRepository.findById(request.proveedorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Proveedor not found: " + request.proveedorId()));
        }

        var invoiceNumber = generateInvoiceNumber(request.tenantId(), request.fecha().getYear());

        var factura = Factura.builder()
                .tenantId(request.tenantId())
                .providerId(request.proveedorId())
                .proveedor(proveedor)
                .invoiceNumber(invoiceNumber)
                .issueDate(request.fecha())
                .type(request.tipo())
                .globalDiscount(request.descuentoGlobal() != null ? request.descuentoGlobal() : BigDecimal.ZERO)
                .paymentMethod(request.metodoPago())
                .status("REGISTRADA")
                .total(BigDecimal.ZERO)
                .build();

        if (isGastoSinItems(request)) {
            // ponytail: gasto operativo con monto directo, sin items de producto (ej: servicios, salarios)
            factura.setTotal(nz(request.total()).subtract(factura.getGlobalDiscount()));
            factura = facturaRepository.save(factura);
            eventPublisher.publishEvent(new FacturaCreadaEvent(factura));
            return mapper.toResponse(factura, mapper.toItemResponseList(factura.getItems()));
        }

        if (request.items() == null || request.items().isEmpty()) {
            throw new InvalidInputException("Factura debe tener al menos un item");
        }

        var productIds = request.items().stream().map(ItemFacturaRequest::productoId).distinct().toList();
        var inClause = productIds.stream().map(id -> "?").collect(Collectors.joining(","));
        var productNameMap = new HashMap<UUID, String>();
        // ponytail: string concat for IN clause is safe — `inClause` is only `?,?` from validated UUIDs
        jdbc.query(
            "SELECT id, name FROM core.products WHERE id IN (" + inClause + ")",
            (rs, row) -> productNameMap.put(UUID.fromString(rs.getString("id")), rs.getString("name")),
            productIds.toArray());

        // Batch load presentaciones
        var presentacionIds = request.items().stream()
                .map(ItemFacturaRequest::presentacionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, Presentacion> presentacionMap = Collections.emptyMap();
        if (!presentacionIds.isEmpty()) {
            presentacionMap = presentacionRepository.findAllById(presentacionIds).stream()
                    .collect(Collectors.toMap(Presentacion::getId, p -> p));
        }

        BigDecimal total = BigDecimal.ZERO;
        for (var itemReq : request.items()) {
            var calc = buildItem(itemReq, productNameMap, presentacionMap, factura, request.tenantId(), request.fecha());
            total = total.add(calc.subtotal());
        }

        factura.setTotal(total.subtract(factura.getGlobalDiscount()));
        factura = facturaRepository.save(factura);

        eventPublisher.publishEvent(new FacturaCreadaEvent(factura));
        log.debug("Factura created: {} for tenant {}", factura.getId(), factura.getTenantId());

        return mapper.toResponse(factura, mapper.toItemResponseList(factura.getItems()));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "facturas", allEntries = true)
    public FacturaResponse updateFactura(UUID id, UUID tenantId, FacturaRequest request) {
        var factura = getFactura(id, tenantId);
        if (!"REGISTRADA".equals(factura.getStatus())) {
            throw new InvalidInputException("Solo facturas en estado REGISTRADA pueden editarse");
        }
        if (isGastoSinItems(request)) {
            // ponytail: gasto operativo con monto directo, sin items de producto
            reverseProductStats(factura.getItems(), tenantId, factura.getId());
            factura.getItems().clear();
            factura.setProviderId(request.proveedorId());
            factura.setIssueDate(request.fecha());
            factura.setType(request.tipo());
            factura.setGlobalDiscount(request.descuentoGlobal() != null ? request.descuentoGlobal() : BigDecimal.ZERO);
            factura.setPaymentMethod(request.metodoPago());
            factura.setTotal(nz(request.total()).subtract(factura.getGlobalDiscount()));
            factura = facturaRepository.save(factura);
            return mapper.toResponse(factura, mapper.toItemResponseList(factura.getItems()));
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new InvalidInputException("Factura debe tener al menos un item");
        }
        reverseProductStats(factura.getItems(), tenantId, factura.getId());

        // 2. Clear old items (orphanRemoval=true deletes from DB)
        factura.getItems().clear();

        // 3. Build product name map AND presentacion map for new items
        var productIds = request.items().stream().map(ItemFacturaRequest::productoId).distinct().toList();
        var inClause = productIds.stream().map(p -> "?").collect(Collectors.joining(","));
        var productNameMap = new HashMap<UUID, String>();
        jdbc.query(
            "SELECT id, name FROM core.products WHERE id IN (" + inClause + ")",
            (rs, row) -> productNameMap.put(UUID.fromString(rs.getString("id")), rs.getString("name")),
            productIds.toArray());

        // Batch load presentaciones
        var presentacionIds = request.items().stream()
                .map(ItemFacturaRequest::presentacionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, Presentacion> presentacionMap = Collections.emptyMap();
        if (!presentacionIds.isEmpty()) {
            presentacionMap = presentacionRepository.findAllById(presentacionIds).stream()
                    .collect(Collectors.toMap(Presentacion::getId, p -> p));
        }

        // 4. Create new items using InvoiceCalculator
        BigDecimal total = BigDecimal.ZERO;
        for (var itemReq : request.items()) {
            var calc = buildItem(itemReq, productNameMap, presentacionMap, factura, tenantId, request.fecha());
            total = total.add(calc.subtotal());
        }

        // 5. Update header
        factura.setProviderId(request.proveedorId());
        factura.setIssueDate(request.fecha());
        factura.setType(request.tipo());
        factura.setGlobalDiscount(request.descuentoGlobal() != null ? request.descuentoGlobal() : BigDecimal.ZERO);
        factura.setPaymentMethod(request.metodoPago());
        factura.setTotal(total.subtract(factura.getGlobalDiscount()));

        factura = facturaRepository.save(factura);
        log.debug("Factura updated: {} for tenant {}", factura.getId(), factura.getTenantId());

        return mapper.toResponse(factura, mapper.toItemResponseList(factura.getItems()));
    }

    private InvoiceCalculator.CalculatedItem buildItem(ItemFacturaRequest itemReq,
                                                         Map<UUID, String> productNameMap,
                                                         Map<UUID, Presentacion> presentacionMap,
                                                         Factura factura,
                                                         UUID tenantId,
                                                         java.time.LocalDate fecha) {
        String productoName = productNameMap.get(itemReq.productoId());

        int conversionFactor = 1;
        UUID presentacionId = null;
        if (itemReq.presentacionId() != null) {
            var presentacion = presentacionMap.get(itemReq.presentacionId());
            if (presentacion == null) {
                throw new ResourceNotFoundException("Presentacion not found: " + itemReq.presentacionId());
            }
            if (!presentacion.getProducto().getId().equals(itemReq.productoId())) {
                throw new InvalidInputException("Presentacion does not belong to product " + itemReq.productoId());
            }
            conversionFactor = presentacion.getConversion();
            presentacionId = presentacion.getId();
        }

        var resolveReq = new InvoiceCalculator.ResolveRequest(
                itemReq.cantidad(),
                itemReq.precioUnitario(),
                itemReq.descuento(),
                itemReq.cantidadPresentacion(),
                itemReq.valorPresentacion(),
                itemReq.precioUnitarioInput(),
                itemReq.descuentoInput(),
                itemReq.descuentoEsPorcentaje(),
                conversionFactor
        );

        var calc = InvoiceCalculator.resolve(resolveReq);

        var item = ItemFactura.builder()
                .factura(factura)
                .productId(itemReq.productoId())
                .productName(productoName)
                .presentacionId(presentacionId)
                .conversionFactor(conversionFactor)
                .quantity(calc.quantity())
                .unitPrice(calc.unitPrice())
                .discount(calc.discount())
                .subtotal(calc.subtotal())
                .cantidadPresentacion(calc.cantidadPresentacionOriginal())
                .valorPresentacion(calc.valorPresentacionOriginal())
                .precioUnitarioInput(calc.precioUnitarioInputOriginal())
                .descuentoInput(calc.descuentoInputOriginal())
                .descuentoEsPorcentaje(calc.descuentoEsPorcentajeOriginal())
                .build();
        factura.getItems().add(item);

        // Update product expense stats
        jdbc.update("""
            UPDATE core.products SET
                last_unit_price = ?,
                total_investment = total_investment + ?,
                last_purchase_date = ?
            WHERE id = ? AND tenant_id = ?
            """, calc.unitPrice(), calc.subtotal(), fecha,
                itemReq.productoId(), tenantId);

        return calc;
    }

    private void reverseProductStats(List<ItemFactura> oldItems, UUID tenantId, UUID facturaId) {
        if (oldItems.isEmpty()) return;

        var sql = """
            UPDATE core.products SET
                total_investment = total_investment - ?,
                last_unit_price = (
                    SELECT ii.unit_price FROM core.invoice_items ii
                    JOIN core.invoices i ON i.id = ii.invoice_id
                    WHERE ii.product_id = ? AND i.tenant_id = ? AND i.status != 'ELIMINADA' AND i.id != ?
                    ORDER BY i.issue_date DESC, i.created_at DESC
                    LIMIT 1
                ),
                last_purchase_date = (
                    SELECT i.issue_date FROM core.invoices i
                    JOIN core.invoice_items ii ON ii.invoice_id = i.id
                    WHERE ii.product_id = ? AND i.tenant_id = ? AND i.status != 'ELIMINADA' AND i.id != ?
                    ORDER BY i.issue_date DESC, i.created_at DESC
                    LIMIT 1
                )
            WHERE id = ? AND tenant_id = ?
            """;
        var batchArgs = new ArrayList<Object[]>();
        for (var item : oldItems) {
            batchArgs.add(new Object[]{
                item.getSubtotal(), item.getProductId(), tenantId, facturaId,
                item.getProductId(), tenantId, facturaId,
                item.getProductId(), tenantId
            });
        }
        jdbc.batchUpdate(sql, batchArgs);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "facturas", allEntries = true)
    public FacturaResponse pagarFactura(UUID id, UUID tenantId) {
        var factura = getFactura(id, tenantId);
        if (!"REGISTRADA".equals(factura.getStatus())) {
            throw new InvalidInputException("Factura already " + factura.getStatus());
        }
        factura.setStatus("PAGADA");
        factura = facturaRepository.save(factura);
        eventPublisher.publishEvent(new FacturaPagadaEvent(factura));
        return mapper.toResponse(factura, mapper.toItemResponseList(factura.getItems()));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "facturas", allEntries = true)
    public void deleteFactura(UUID id, UUID tenantId) {
        var factura = getFactura(id, tenantId);
        if (!"REGISTRADA".equals(factura.getStatus())) {
            throw new InvalidInputException("Cannot delete factura in status " + factura.getStatus());
        }
        reverseProductStats(factura.getItems(), tenantId, factura.getId());
        facturaRepository.delete(factura);
    }

    // -- helpers --

    private boolean isGastoSinItems(FacturaRequest request) {
        return "GASTO_OPERATIVO".equals(request.tipo())
                && (request.items() == null || request.items().isEmpty());
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Proveedor getProveedor(UUID id, UUID tenantId) {
        var proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor not found: " + id));
        if (!proveedor.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Proveedor not found: " + id);
        }
        return proveedor;
    }

    private Factura getFactura(UUID id, UUID tenantId) {
        var factura = facturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factura not found: " + id));
        if (!factura.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Factura not found: " + id);
        }
        return factura;
    }

    private String generateInvoiceNumber(UUID tenantId, int year) {
        var prefix = "F-PROV-" + year + "-";
        var max = facturaRepository.findMaxInvoiceNumber(tenantId, prefix + "%");
        var next = max.map(s -> Integer.parseInt(s.substring(prefix.length())) + 1).orElse(1);
        return prefix + String.format("%04d", next);
    }
}

package core_pymes.invoice.service.impl;

import core_pymes.invoice.domain.Factura;
import core_pymes.invoice.domain.ItemFactura;
import core_pymes.invoice.domain.Proveedor;
import core_pymes.invoice.dto.*;
import core_pymes.invoice.event.FacturaCreadaEvent;
import core_pymes.invoice.mapper.FacturaMapper;
import core_pymes.invoice.repository.FacturaRepository;
import core_pymes.invoice.repository.ProveedorRepository;
import core_pymes.invoice.service.FacturaService;
import core_pymes.product.repository.PresentacionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
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
                .ruc(request.ruc())
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
        proveedor.setRuc(request.ruc());
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
        var proveedor = proveedorRepository.findById(request.proveedorId())
                .orElseThrow(() -> new EntityNotFoundException("Proveedor not found: " + request.proveedorId()));

        var invoiceNumber = generateInvoiceNumber(request.tenantId(), request.fecha().getYear());

        var factura = Factura.builder()
                .tenantId(request.tenantId())
                .providerId(request.proveedorId())
                .invoiceNumber(invoiceNumber)
                .issueDate(request.fecha())
                .type(request.tipo())
                .globalDiscount(request.descuentoGlobal() != null ? request.descuentoGlobal() : BigDecimal.ZERO)
                .paymentMethod(request.metodoPago())
                .status("REGISTRADA")
                .total(BigDecimal.ZERO)
                .build();

        var productIds = request.items().stream().map(ItemFacturaRequest::productoId).distinct().toList();
        var inClause = productIds.stream().map(id -> "?").collect(Collectors.joining(","));
        var productNameMap = new HashMap<UUID, String>();
        jdbc.query(
            "SELECT id, name FROM core.products WHERE id IN (" + inClause + ")",
            (rs, row) -> productNameMap.put(UUID.fromString(rs.getString("id")), rs.getString("name")),
            productIds.toArray());

        BigDecimal total = BigDecimal.ZERO;
        for (var itemReq : request.items()) {
            var discount = itemReq.descuento() != null ? itemReq.descuento() : BigDecimal.ZERO;
            var subtotal = itemReq.cantidad().multiply(itemReq.precioUnitario()).subtract(discount);
            total = total.add(subtotal);

            var productoName = productNameMap.get(itemReq.productoId());

            int conversionFactor = 1;
            UUID presentacionId = null;
            if (itemReq.presentacionId() != null) {
                var presentacion = presentacionRepository.findById(itemReq.presentacionId())
                        .orElseThrow(() -> new EntityNotFoundException("Presentacion not found: " + itemReq.presentacionId()));
                if (!presentacion.getProducto().getId().equals(itemReq.productoId())) {
                    throw new IllegalArgumentException("Presentacion does not belong to product " + itemReq.productoId());
                }
                conversionFactor = presentacion.getConversion();
                presentacionId = presentacion.getId();
            }

            var item = ItemFactura.builder()
                    .factura(factura)
                    .productId(itemReq.productoId())
                    .productName(productoName)
                    .presentacionId(presentacionId)
                    .conversionFactor(conversionFactor)
                    .quantity(itemReq.cantidad())
                    .unitPrice(itemReq.precioUnitario())
                    .discount(discount)
                    .subtotal(subtotal)
                    .build();
            factura.getItems().add(item);

            // ponytail: update product expense stats inline
            jdbc.update("""
                UPDATE core.products SET
                    last_unit_price = ?,
                    total_investment = total_investment + ?,
                    last_purchase_date = ?
                WHERE id = ? AND tenant_id = ?
                """, itemReq.precioUnitario(), subtotal, request.fecha(),
                    itemReq.productoId(), request.tenantId());
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
    public FacturaResponse pagarFactura(UUID id, UUID tenantId) {
        var factura = getFactura(id, tenantId);
        if (!"REGISTRADA".equals(factura.getStatus())) {
            throw new IllegalStateException("Factura already " + factura.getStatus());
        }
        factura.setStatus("PAGADA");
        factura = facturaRepository.save(factura);
        return mapper.toResponse(factura, mapper.toItemResponseList(factura.getItems()));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "facturas", allEntries = true)
    public void deleteFactura(UUID id, UUID tenantId) {
        var factura = getFactura(id, tenantId);
        if (!"REGISTRADA".equals(factura.getStatus())) {
            throw new IllegalStateException("Cannot delete factura in status " + factura.getStatus());
        }
        facturaRepository.delete(factura);
    }

    // -- helpers --

    private Proveedor getProveedor(UUID id, UUID tenantId) {
        var proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor not found: " + id));
        if (!proveedor.getTenantId().equals(tenantId)) {
            throw new EntityNotFoundException("Proveedor not found: " + id);
        }
        return proveedor;
    }

    private Factura getFactura(UUID id, UUID tenantId) {
        var factura = facturaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Factura not found: " + id));
        if (!factura.getTenantId().equals(tenantId)) {
            throw new EntityNotFoundException("Factura not found: " + id);
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

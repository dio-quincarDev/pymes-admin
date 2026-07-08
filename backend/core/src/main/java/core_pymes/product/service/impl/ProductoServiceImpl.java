package core_pymes.product.service.impl;

import core_pymes.invoice.domain.Proveedor;
import core_pymes.invoice.repository.ProveedorRepository;
import core_pymes.product.domain.Presentacion;
import core_pymes.product.domain.Producto;
import core_pymes.product.dto.PresentacionRequest;
import core_pymes.product.dto.PresentacionResponse;
import core_pymes.product.dto.ProductoRequest;
import core_pymes.product.dto.ProductoResponse;
import core_pymes.product.event.PresentacionCreadaEvent;
import core_pymes.product.event.ProductoCreadoEvent;
import core_pymes.product.mapper.ProductoMapper;
import core_pymes.product.repository.PresentacionRepository;
import core_pymes.product.repository.ProductoRepository;
import core_pymes.product.service.ProductoService;
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
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final PresentacionRepository presentacionRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productos", key = "#tenantId")
    public List<ProductoResponse> findAll(UUID tenantId) {
        return productoRepository.findByTenantId(tenantId).stream()
                .map(p -> mapper.toResponse(p, mapPresentaciones(p.getId()), findProveedor(p.getProviderId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productos", key = "#id")
    public ProductoResponse findById(UUID id, UUID tenantId) {
        var producto = getProducto(id, tenantId);
        return mapper.toResponse(producto, mapPresentaciones(id), findProveedor(producto.getProviderId()));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "productos", allEntries = true)
    public ProductoResponse create(ProductoRequest request) {
        var sku = request.sku();
        if (sku == null || sku.isBlank()) {
            long count = productoRepository.countByTenantId(request.tenantId());
            sku = String.format("P-%04d", count + 1);
        }
        var producto = Producto.builder()
                .tenantId(request.tenantId())
                .name(request.name())
                .sku(sku)
                .category(request.category())
                .baseUnit(request.baseUnit())
                .imageUrl(request.imageUrl())
                .minQuantity(request.minQuantity())
                .maxQuantity(request.maxQuantity())
                .providerId(request.proveedorId())
                .build();
        producto = productoRepository.save(producto);
        eventPublisher.publishEvent(new ProductoCreadoEvent(producto));
        log.debug("Producto created: {} for tenant {}", producto.getId(), producto.getTenantId());
        return mapper.toResponse(producto, List.of(), findProveedor(producto.getProviderId()));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "productos", allEntries = true)
    public ProductoResponse update(UUID id, UUID tenantId, ProductoRequest request) {
        var producto = getProducto(id, tenantId);
        producto.setName(request.name());
        producto.setSku(request.sku());
        producto.setCategory(request.category());
        producto.setBaseUnit(request.baseUnit());
        producto.setImageUrl(request.imageUrl());
        producto.setMinQuantity(request.minQuantity());
        producto.setMaxQuantity(request.maxQuantity());
        producto.setProviderId(request.proveedorId());
        producto = productoRepository.save(producto);
        return mapper.toResponse(producto, mapPresentaciones(id), findProveedor(producto.getProviderId()));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "productos", allEntries = true)
    public void delete(UUID id, UUID tenantId) {
        var producto = getProducto(id, tenantId);
        productoRepository.delete(producto);
        log.debug("Producto deleted: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PresentacionResponse> findPresentaciones(UUID productId, UUID tenantId) {
        getProducto(productId, tenantId);
        return mapPresentaciones(productId);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "productos", allEntries = true)
    public PresentacionResponse addPresentacion(UUID productId, UUID tenantId, PresentacionRequest request) {
        var producto = getProducto(productId, tenantId);
        var presentacion = Presentacion.builder()
                .producto(producto)
                .name(request.name())
                .conversion(request.conversion())
                .build();
        presentacion = presentacionRepository.save(presentacion);
        eventPublisher.publishEvent(new PresentacionCreadaEvent(presentacion));
        log.debug("Presentacion created: {} for product {}", presentacion.getId(), productId);
        return mapper.toResponse(presentacion);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "productos", allEntries = true)
    public void deletePresentacion(UUID presentacionId, UUID tenantId) {
        var presentacion = presentacionRepository.findById(presentacionId)
                .orElseThrow(() -> new EntityNotFoundException("Presentacion not found: " + presentacionId));
        var producto = presentacion.getProducto();
        if (!producto.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Presentacion does not belong to tenant");
        }
        presentacionRepository.delete(presentacion);
        log.debug("Presentacion deleted: {}", presentacionId);
    }

    private Producto getProducto(UUID id, UUID tenantId) {
        var producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto not found: " + id));
        if (!producto.getTenantId().equals(tenantId)) {
            throw new EntityNotFoundException("Producto not found: " + id);
        }
        return producto;
    }

    private List<PresentacionResponse> mapPresentaciones(UUID productId) {
        return mapper.toResponseList(presentacionRepository.findByProductoIdAndIsActiveTrue(productId));
    }

    private Proveedor findProveedor(UUID providerId) {
        if (providerId == null) return null;
        return proveedorRepository.findById(providerId).orElse(null);
    }
}

package core_pymes.product.controller.impl;

import core_pymes.product.controller.ProductoApi;
import core_pymes.product.dto.PresentacionRequest;
import core_pymes.product.dto.PresentacionResponse;
import core_pymes.product.dto.ProductoRequest;
import core_pymes.product.dto.ProductoResponse;
import core_pymes.product.service.ProductoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ProductoController implements ProductoApi {

    private final ProductoService productoService;

    @Override
    public ResponseEntity<List<ProductoResponse>> findAll(UUID tenantId) {
        return ResponseEntity.ok(productoService.findAll(tenantId));
    }

    @Override
    public ResponseEntity<Page<ProductoResponse>> search(UUID tenantId, String category, String search, Pageable pageable) {
        return ResponseEntity.ok(productoService.search(tenantId, category, search, pageable));
    }

    @Override
    public ResponseEntity<ProductoResponse> findById(UUID id, UUID tenantId) {
        return ResponseEntity.ok(productoService.findById(id, tenantId));
    }

    @Override
    public ResponseEntity<ProductoResponse> create(ProductoRequest request) {
        return ResponseEntity.ok(productoService.create(request));
    }

    @Override
    public ResponseEntity<ProductoResponse> update(UUID id, UUID tenantId, ProductoRequest request) {
        return ResponseEntity.ok(productoService.update(id, tenantId, request));
    }

    @Override
    public ResponseEntity<Void> delete(UUID id, UUID tenantId) {
        productoService.delete(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<PresentacionResponse>> findPresentaciones(UUID id, UUID tenantId) {
        return ResponseEntity.ok(productoService.findPresentaciones(id, tenantId));
    }

    @Override
    public ResponseEntity<PresentacionResponse> addPresentacion(UUID id, UUID tenantId, PresentacionRequest request) {
        return ResponseEntity.ok(productoService.addPresentacion(id, tenantId, request));
    }

    @Override
    public ResponseEntity<Void> deletePresentacion(UUID presentacionId, UUID tenantId) {
        productoService.deletePresentacion(presentacionId, tenantId);
        return ResponseEntity.noContent().build();
    }
}

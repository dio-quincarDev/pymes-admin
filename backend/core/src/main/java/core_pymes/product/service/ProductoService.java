package core_pymes.product.service;

import core_pymes.product.dto.PresentacionRequest;
import core_pymes.product.dto.PresentacionResponse;
import core_pymes.product.dto.ProductoRequest;
import core_pymes.product.dto.ProductoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProductoService {

    List<ProductoResponse> findAll(UUID tenantId);

    Page<ProductoResponse> search(UUID tenantId, String category, String search, Pageable pageable);

    ProductoResponse findById(UUID id, UUID tenantId);

    ProductoResponse create(ProductoRequest request);

    ProductoResponse update(UUID id, UUID tenantId, ProductoRequest request);

    void delete(UUID id, UUID tenantId);

    List<PresentacionResponse> findPresentaciones(UUID productId, UUID tenantId);

    PresentacionResponse addPresentacion(UUID productId, UUID tenantId, PresentacionRequest request);

    void deletePresentacion(UUID presentacionId, UUID tenantId);
}

package core_pymes.product.controller;

import core_pymes.common.constant.CorePath;
import core_pymes.product.dto.PresentacionRequest;
import core_pymes.product.dto.PresentacionResponse;
import core_pymes.product.dto.ProductoRequest;
import core_pymes.product.dto.ProductoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Productos", description = "Product and presentation management")
@RequestMapping(CorePath.V1_ROUTE + CorePath.CORE_ROUTE + CorePath.PRODUCTOS_ROUTE)
public interface ProductoApi {

    @Operation(summary = "List all products for a tenant")
    @GetMapping
    ResponseEntity<List<ProductoResponse>> findAll(@RequestParam UUID tenantId);

    @Operation(summary = "Search products with optional category filter, name search, and pagination")
    @GetMapping("/search")
    ResponseEntity<Page<ProductoResponse>> search(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            Pageable pageable);

    @Operation(summary = "Get product by ID")
    @GetMapping("/{id}")
    ResponseEntity<ProductoResponse> findById(@PathVariable UUID id, @RequestParam UUID tenantId);

    @Operation(summary = "Create a product")
    @PostMapping
    ResponseEntity<ProductoResponse> create(@Valid @RequestBody ProductoRequest request);

    @Operation(summary = "Update a product")
    @PutMapping("/{id}")
    ResponseEntity<ProductoResponse> update(@PathVariable UUID id, @RequestParam UUID tenantId,
                                            @Valid @RequestBody ProductoRequest request);

    @Operation(summary = "Delete a product")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam UUID tenantId);

    @Operation(summary = "List presentations for a product")
    @GetMapping("/{id}/presentaciones")
    ResponseEntity<List<PresentacionResponse>> findPresentaciones(@PathVariable UUID id,
                                                                  @RequestParam UUID tenantId);

    @Operation(summary = "Add a presentation to a product")
    @PostMapping("/{id}/presentaciones")
    ResponseEntity<PresentacionResponse> addPresentacion(@PathVariable UUID id, @RequestParam UUID tenantId,
                                                         @Valid @RequestBody PresentacionRequest request);

    @Operation(summary = "Delete a presentation")
    @DeleteMapping("/presentaciones/{presentacionId}")
    ResponseEntity<Void> deletePresentacion(@PathVariable UUID presentacionId, @RequestParam UUID tenantId);
}

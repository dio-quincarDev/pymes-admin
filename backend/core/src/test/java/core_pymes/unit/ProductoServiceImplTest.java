package core_pymes.unit;

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
import core_pymes.common.exception.custom.InvalidInputException;
import core_pymes.common.exception.custom.ResourceNotFoundException;
import core_pymes.product.service.impl.ProductoServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    @Mock ProductoRepository productoRepository;
    @Mock PresentacionRepository presentacionRepository;
    @Mock ProveedorRepository proveedorRepository;
    @Mock ProductoMapper mapper;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks ProductoServiceImpl service;

    @Test
    void findAll_withTenantId_returnsProducts() {
        var tenantId = UUID.randomUUID();
        var producto = Producto.builder().id(UUID.randomUUID()).tenantId(tenantId).name("Arroz").build();
        when(productoRepository.findByTenantId(tenantId)).thenReturn(List.of(producto));
        when(presentacionRepository.findByProductoIdAndIsActiveTrue(producto.getId())).thenReturn(List.of());
        when(mapper.toResponseList(any())).thenReturn(List.of());
        var expected = new ProductoResponse(producto.getId(), tenantId, "Arroz", null, null, null, null, true, null, null, List.of(), null, null, null, null, null, null, null);
        when(mapper.toResponse(producto, List.of(), null)).thenReturn(expected);

        var result = service.findAll(tenantId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Arroz");
    }

    @Test
    void findById_withUnknownId_throws404() {
        var tenantId = UUID.randomUUID();
        when(productoRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(UUID.randomUUID(), tenantId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Producto not found");
    }

    @Test
    void findById_withWrongTenant_throws404() {
        var tenantId = UUID.randomUUID();
        var otherTenant = UUID.randomUUID();
        var producto = Producto.builder().id(UUID.randomUUID()).tenantId(otherTenant).build();
        when(productoRepository.findById(producto.getId())).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> service.findById(producto.getId(), tenantId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Producto not found");
    }

    @Test
    void search_withNoFilters_delegatesToFindByTenantId() {
        var tenantId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 20);
        var producto = Producto.builder().id(UUID.randomUUID()).tenantId(tenantId).name("Arroz").build();
        var page = new PageImpl<>(List.of(producto), pageable, 1);
        when(productoRepository.findByTenantId(tenantId, pageable)).thenReturn(page);
        when(presentacionRepository.findByProductoIdInAndIsActiveTrue(List.of(producto.getId()))).thenReturn(List.of());
        when(mapper.toResponse(eq(producto), any(), any())).thenReturn(
                new ProductoResponse(producto.getId(), tenantId, "Arroz", null, null, null, null, true, null, null, List.of(), null, null, null, null, null, null, null));

        var result = service.search(tenantId, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().name()).isEqualTo("Arroz");
        verify(productoRepository).findByTenantId(tenantId, pageable);
    }

    @Test
    void search_withCategoryFilter_delegatesToFindByTenantIdAndCategory() {
        var tenantId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 20);
        var producto = Producto.builder().id(UUID.randomUUID()).tenantId(tenantId).name("Leche").category("LACTEOS").build();
        var page = new PageImpl<>(List.of(producto), pageable, 1);
        when(productoRepository.findByTenantIdAndCategory(tenantId, "LACTEOS", pageable)).thenReturn(page);
        when(presentacionRepository.findByProductoIdInAndIsActiveTrue(List.of(producto.getId()))).thenReturn(List.of());
        when(mapper.toResponse(eq(producto), any(), any())).thenReturn(
                new ProductoResponse(producto.getId(), tenantId, "Leche", null, "LACTEOS", null, null, true, null, null, List.of(), null, null, null, null, null, null, null));

        var result = service.search(tenantId, "LACTEOS", null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().category()).isEqualTo("LACTEOS");
        verify(productoRepository).findByTenantIdAndCategory(tenantId, "LACTEOS", pageable);
    }

    @Test
    void search_withNameSearch_delegatesToFindByTenantIdAndNameContainingIgnoreCase() {
        var tenantId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 20);
        var producto = Producto.builder().id(UUID.randomUUID()).tenantId(tenantId).name("Arroz").build();
        var page = new PageImpl<>(List.of(producto), pageable, 1);
        when(productoRepository.findByTenantIdAndNameContainingIgnoreCase(tenantId, "arroz", pageable)).thenReturn(page);
        when(presentacionRepository.findByProductoIdInAndIsActiveTrue(List.of(producto.getId()))).thenReturn(List.of());
        when(mapper.toResponse(eq(producto), any(), any())).thenReturn(
                new ProductoResponse(producto.getId(), tenantId, "Arroz", null, null, null, null, true, null, null, List.of(), null, null, null, null, null, null, null));

        var result = service.search(tenantId, null, "arroz", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().name()).isEqualTo("Arroz");
        verify(productoRepository).findByTenantIdAndNameContainingIgnoreCase(tenantId, "arroz", pageable);
    }

    @Test
    void search_withCategoryAndSearch_delegatesToCombinedQuery() {
        var tenantId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 20);
        var producto = Producto.builder().id(UUID.randomUUID()).tenantId(tenantId).name("Arroz").category("ABARROTES").build();
        var page = new PageImpl<>(List.of(producto), pageable, 1);
        when(productoRepository.findByTenantIdAndCategoryAndNameContainingIgnoreCase(tenantId, "ABARROTES", "arroz", pageable)).thenReturn(page);
        when(presentacionRepository.findByProductoIdInAndIsActiveTrue(List.of(producto.getId()))).thenReturn(List.of());
        when(mapper.toResponse(eq(producto), any(), any())).thenReturn(
                new ProductoResponse(producto.getId(), tenantId, "Arroz", null, "ABARROTES", null, null, true, null, null, List.of(), null, null, null, null, null, null, null));

        var result = service.search(tenantId, "ABARROTES", "arroz", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().name()).isEqualTo("Arroz");
        verify(productoRepository).findByTenantIdAndCategoryAndNameContainingIgnoreCase(tenantId, "ABARROTES", "arroz", pageable);
    }

    @Test
    void search_usesBatchFetchForPresentacionesAndProveedores() {
        var tenantId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 20);
        var producto = Producto.builder().id(UUID.randomUUID()).tenantId(tenantId).name("Arroz").providerId(UUID.randomUUID()).build();
        var page = new PageImpl<>(List.of(producto), pageable, 1);
        when(productoRepository.findByTenantId(tenantId, pageable)).thenReturn(page);
        when(presentacionRepository.findByProductoIdInAndIsActiveTrue(List.of(producto.getId()))).thenReturn(List.of());
        when(proveedorRepository.findByIdIn(List.of(producto.getProviderId()))).thenReturn(List.of(new Proveedor()));
        when(mapper.toResponse(eq(producto), any(), any())).thenReturn(
                new ProductoResponse(producto.getId(), tenantId, "Arroz", null, null, null, null, true, null, null, List.of(), null, null, null, null, null, producto.getProviderId(), "Proveedor X"));

        var result = service.search(tenantId, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        // Batch fetch called with product ids, not individual lookups
        verify(presentacionRepository).findByProductoIdInAndIsActiveTrue(List.of(producto.getId()));
        verify(proveedorRepository).findByIdIn(List.of(producto.getProviderId()));
        verify(presentacionRepository, never()).findByProductoIdAndIsActiveTrue(any());
        verify(proveedorRepository, never()).findById(any());
    }

    @Test
    void create_withValidRequest_savesAndPublishesEvent() {
        var tenantId = UUID.randomUUID();
        var request = new ProductoRequest(tenantId, "Arroz", "ARR-001", "ABARROTES", "Kg", null, null, null, null);
        var saved = Producto.builder().id(UUID.randomUUID()).tenantId(tenantId).name("Arroz").sku("ARR-001").build();
        when(productoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var response = new ProductoResponse(saved.getId(), tenantId, "Arroz", "ARR-001", null, null, null, true, null, null, List.of(), null, null, null, null, null, null, null);
        when(mapper.toResponse(any(), eq(List.of()), isNull())).thenReturn(response);

        var result = service.create(request);

        assertThat(result.name()).isEqualTo("Arroz");
        var captor = ArgumentCaptor.forClass(ProductoCreadoEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().producto().getName()).isEqualTo("Arroz");
    }

    @Test
    void addPresentacion_withValidData_savesAndPublishesEvent() {
        var tenantId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        var producto = Producto.builder().id(productId).tenantId(tenantId).build();
        when(productoRepository.findById(productId)).thenReturn(Optional.of(producto));
        var request = new PresentacionRequest("Caja", 24);
        var saved = Presentacion.builder().id(UUID.randomUUID()).name("Caja").conversion(24).build();
        when(presentacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var response = new PresentacionResponse(saved.getId(), productId, "Caja", 24, true, null);
        when(mapper.toResponse(any())).thenReturn(response);

        var result = service.addPresentacion(productId, tenantId, request);

        assertThat(result.conversion()).isEqualTo(24);
        var captor = ArgumentCaptor.forClass(PresentacionCreadaEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().presentacion().getConversion()).isEqualTo(24);
    }

    @Test
    void deletePresentacion_withWrongTenant_throws() {
        var tenantId = UUID.randomUUID();
        var otherTenant = UUID.randomUUID();
        var producto = Producto.builder().id(UUID.randomUUID()).tenantId(otherTenant).build();
        var presentacion = Presentacion.builder().id(UUID.randomUUID()).producto(producto).build();
        when(presentacionRepository.findById(presentacion.getId())).thenReturn(Optional.of(presentacion));

        assertThatThrownBy(() -> service.deletePresentacion(presentacion.getId(), tenantId))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("does not belong to tenant");
    }
}

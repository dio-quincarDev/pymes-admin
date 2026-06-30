package core_pymes.unit;

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
import core_pymes.product.service.impl.ProductoServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    @Mock ProductoRepository productoRepository;
    @Mock PresentacionRepository presentacionRepository;
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
        var expected = new ProductoResponse(producto.getId(), tenantId, "Arroz", null, null, null, null, true, null, null, List.of(), null, null, null, null, null);
        when(mapper.toResponse(producto, List.of())).thenReturn(expected);

        var result = service.findAll(tenantId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Arroz");
    }

    @Test
    void findById_withUnknownId_throws404() {
        var tenantId = UUID.randomUUID();
        when(productoRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(UUID.randomUUID(), tenantId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Producto not found");
    }

    @Test
    void findById_withWrongTenant_throws404() {
        var tenantId = UUID.randomUUID();
        var otherTenant = UUID.randomUUID();
        var producto = Producto.builder().id(UUID.randomUUID()).tenantId(otherTenant).build();
        when(productoRepository.findById(producto.getId())).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> service.findById(producto.getId(), tenantId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Producto not found");
    }

    @Test
    void create_withValidRequest_savesAndPublishesEvent() {
        var tenantId = UUID.randomUUID();
        var request = new ProductoRequest(tenantId, "Arroz", "ARR-001", "ABARROTES", "Kg", null, null, null);
        var saved = Producto.builder().id(UUID.randomUUID()).tenantId(tenantId).name("Arroz").sku("ARR-001").build();
        when(productoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var response = new ProductoResponse(saved.getId(), tenantId, "Arroz", "ARR-001", null, null, null, true, null, null, List.of(), null, null, null, null, null);
        when(mapper.toResponse(any(), eq(List.of()))).thenReturn(response);

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
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to tenant");
    }
}

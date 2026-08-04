package core_pymes.unit;

import core_pymes.invoice.domain.Factura;
import core_pymes.invoice.domain.ItemFactura;
import core_pymes.invoice.domain.Proveedor;
import core_pymes.costos.domain.Collaborador;
import core_pymes.costos.repository.CollaboradorRepository;
import core_pymes.invoice.dto.*;
import core_pymes.invoice.event.FacturaCreadaEvent;
import core_pymes.invoice.mapper.FacturaMapper;
import core_pymes.invoice.repository.FacturaRepository;
import core_pymes.invoice.repository.ProveedorRepository;
import core_pymes.invoice.service.impl.FacturaServiceImpl;
import core_pymes.product.domain.Presentacion;
import core_pymes.product.domain.Producto;
import core_pymes.product.repository.PresentacionRepository;
import core_pymes.common.exception.custom.InvalidInputException;
import core_pymes.common.exception.custom.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.lenient;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacturaServiceImplTest {

    @Mock ProveedorRepository proveedorRepository;
    @Mock CollaboradorRepository collaboradorRepository;
    @Mock FacturaRepository facturaRepository;
    @Mock FacturaMapper mapper;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock JdbcTemplate jdbc;
    @Mock PresentacionRepository presentacionRepository;
    @InjectMocks FacturaServiceImpl service;

    private Presentacion mockPresentacion(UUID productId, int conversion) {
        var producto = Producto.builder().id(productId).build();
        return Presentacion.builder().id(UUID.randomUUID()).producto(producto).conversion(conversion).build();
    }

    private void mockPresentaciones(List<Presentacion> presentaciones) {
        lenient().when(presentacionRepository.findAllById(any())).thenReturn(presentaciones);
        presentaciones.forEach(p -> lenient().when(presentacionRepository.findById(p.getId())).thenReturn(Optional.of(p)));
    }

    @Test
    void createFactura_withValidRequest_calculatesTotalAndGeneratesNumber() {
        var tenantId = UUID.randomUUID();
        var proveedorId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        var presentacion = mockPresentacion(productId, 1);
        var proveedor = Proveedor.builder().id(proveedorId).tenantId(tenantId).name("Distribuidora ABC").contactName("Carlos").contactPhone("555-0100").contactEmail("carlos@abc.com").build();
        when(proveedorRepository.findById(proveedorId)).thenReturn(Optional.of(proveedor));
        when(facturaRepository.findMaxInvoiceNumber(eq(tenantId), anyString())).thenReturn(Optional.empty());
        mockPresentaciones(List.of(presentacion));

        var item = new ItemFacturaRequest(productId, presentacion.getId(), new BigDecimal("10"), new BigDecimal("5.50"), BigDecimal.ZERO, null, null, null, null, null);
        var request = new FacturaRequest(tenantId, proveedorId, null, LocalDate.of(2026, 6, 1),
                "FACTURA", "EFECTIVO", null, BigDecimal.ZERO, null, List.of(item));

        var savedFactura = Factura.builder().id(UUID.randomUUID()).tenantId(tenantId).providerId(proveedorId)
                .invoiceNumber("F-PROV-2026-0001").total(new BigDecimal("55.00")).status("REGISTRADA")
                .issueDate(request.fecha()).type(request.tipo()).build();
        when(facturaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var itemResponse = new ItemFacturaResponse(UUID.randomUUID(), productId, "Arroz",
                presentacion.getId(), 1, new BigDecimal("10"), new BigDecimal("5.50"), BigDecimal.ZERO, new BigDecimal("55.00"),
                null, null, null, null, null);
        when(mapper.toItemResponseList(anyList())).thenReturn(List.of(itemResponse));
        var facturaResponse = new FacturaResponse(savedFactura.getId(), tenantId, proveedorId, "Distribuidora ABC",
                null, null,
                "F-PROV-2026-0001", LocalDate.of(2026, 6, 1), "FACTURA",
                BigDecimal.ZERO, "EFECTIVO", null, "REGISTRADA", new BigDecimal("55.00"), List.of(itemResponse), null);
        when(mapper.toResponse(any(), anyList())).thenReturn(facturaResponse);

        var result = service.createFactura(request);

        assertThat(result.invoiceNumber()).isEqualTo("F-PROV-2026-0001");
        assertThat(result.total()).isEqualByComparingTo(new BigDecimal("55.00"));
        assertThat(result.status()).isEqualTo("REGISTRADA");
    }

    @Test
    void createFactura_withGlobalDiscount_subtractsFromTotal() {
        var tenantId = UUID.randomUUID();
        var proveedorId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        var presentacion = mockPresentacion(productId, 1);
        var proveedor = Proveedor.builder().id(proveedorId).tenantId(tenantId).build();
        when(proveedorRepository.findById(proveedorId)).thenReturn(Optional.of(proveedor));
        when(facturaRepository.findMaxInvoiceNumber(eq(tenantId), anyString())).thenReturn(Optional.empty());
        mockPresentaciones(List.of(presentacion));

        var item = new ItemFacturaRequest(productId, presentacion.getId(), new BigDecimal("5"), new BigDecimal("20.00"), BigDecimal.ZERO, null, null, null, null, null);
        var request = new FacturaRequest(tenantId, proveedorId, null, LocalDate.of(2026, 6, 1),
                "FACTURA", null, null, new BigDecimal("10.00"), null, List.of(item));

        var savedFactura = Factura.builder().id(UUID.randomUUID()).tenantId(tenantId)
                .invoiceNumber("F-PROV-2026-0001").total(new BigDecimal("90.00")).status("REGISTRADA")
                .issueDate(request.fecha()).type(request.tipo()).build();
        when(facturaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toItemResponseList(anyList())).thenReturn(List.of());
        when(mapper.toResponse(any(), anyList())).thenAnswer(i -> {
            Factura f = i.getArgument(0);
            return new FacturaResponse(f.getId(), f.getTenantId(), f.getProviderId(), null, null, null,
                    f.getInvoiceNumber(), f.getIssueDate(), f.getType(), f.getGlobalDiscount(),
                    f.getPaymentMethod(), null, f.getStatus(), f.getTotal(), List.of(), null);
        });

        var result = service.createFactura(request);

        assertThat(result.total()).isEqualByComparingTo(new BigDecimal("90.00"));
        assertThat(result.globalDiscount()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void createFactura_withValidRequest_publishesEvent() {
        var tenantId = UUID.randomUUID();
        var proveedorId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        var presentacion = mockPresentacion(productId, 1);
        var proveedor = Proveedor.builder().id(proveedorId).tenantId(tenantId).build();
        when(proveedorRepository.findById(proveedorId)).thenReturn(Optional.of(proveedor));
        when(facturaRepository.findMaxInvoiceNumber(eq(tenantId), anyString())).thenReturn(Optional.empty());
        mockPresentaciones(List.of(presentacion));
        when(facturaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toItemResponseList(anyList())).thenReturn(List.of());
        when(mapper.toResponse(any(), anyList())).thenReturn(
                new FacturaResponse(null, tenantId, proveedorId, null, null, null, null, null, null,
                        null, null, null, null, null, List.of(), null));

        service.createFactura(new FacturaRequest(tenantId, proveedorId, null, LocalDate.of(2026, 6, 1),
                "FACTURA", null, null, BigDecimal.ZERO, null,
                List.of(new ItemFacturaRequest(productId, presentacion.getId(), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, null, null, null, null, null))));

        var captor = ArgumentCaptor.forClass(FacturaCreadaEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().factura()).isNotNull();
    }

    @Test
    void createFactura_withUnmatchedPresentacion_throws() {
        var tenantId = UUID.randomUUID();
        var proveedorId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        var otherProductId = UUID.randomUUID();
        var presentacion = mockPresentacion(otherProductId, 1);
        var proveedor = Proveedor.builder().id(proveedorId).tenantId(tenantId).build();
        when(proveedorRepository.findById(proveedorId)).thenReturn(Optional.of(proveedor));
        when(facturaRepository.findMaxInvoiceNumber(eq(tenantId), anyString())).thenReturn(Optional.empty());
        mockPresentaciones(List.of(presentacion));

        var item = new ItemFacturaRequest(productId, presentacion.getId(), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, null, null, null, null, null);
        var request = new FacturaRequest(tenantId, proveedorId, null, LocalDate.of(2026, 6, 1),
                "FACTURA", null, null, BigDecimal.ZERO, null, List.of(item));

        assertThatThrownBy(() -> service.createFactura(request))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("does not belong to product");
    }

    @Test
    void pagarFactura_whenRegistered_marksAsPaid() {
        var tenantId = UUID.randomUUID();
        var facturaId = UUID.randomUUID();
        var factura = Factura.builder().id(facturaId).tenantId(tenantId).status("REGISTRADA").items(List.of()).build();
        when(facturaRepository.findById(facturaId)).thenReturn(Optional.of(factura));
        when(facturaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toItemResponseList(anyList())).thenReturn(List.of());
        when(mapper.toResponse(any(), anyList())).thenAnswer(i -> {
            Factura f = i.getArgument(0);
            return new FacturaResponse(f.getId(), f.getTenantId(), null, null, null, null,
                    null, null, null, null, null, null, f.getStatus(), null, List.of(), null);
        });

        var result = service.pagarFactura(facturaId, tenantId);

        assertThat(result.status()).isEqualTo("PAGADA");
    }

    @Test
    void pagarFactura_whenAlreadyPaid_throws() {
        var tenantId = UUID.randomUUID();
        var factura = Factura.builder().id(UUID.randomUUID()).tenantId(tenantId).status("PAGADA").build();
        when(facturaRepository.findById(factura.getId())).thenReturn(Optional.of(factura));

        assertThatThrownBy(() -> service.pagarFactura(factura.getId(), tenantId))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("already");
    }

    @Test
    void deleteFactura_whenPaid_throws() {
        var tenantId = UUID.randomUUID();
        var factura = Factura.builder().id(UUID.randomUUID()).tenantId(tenantId).status("PAGADA").build();
        when(facturaRepository.findById(factura.getId())).thenReturn(Optional.of(factura));

        assertThatThrownBy(() -> service.deleteFactura(factura.getId(), tenantId))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Cannot delete");
    }

    @Test
    void updateFactura_withEmptyItems_throws() {
        var tenantId = UUID.randomUUID();
        var facturaId = UUID.randomUUID();
        var factura = Factura.builder().id(facturaId).tenantId(tenantId).status("REGISTRADA").items(List.of()).build();
        when(facturaRepository.findById(facturaId)).thenReturn(Optional.of(factura));

        var request = new FacturaRequest(tenantId, UUID.randomUUID(), null, LocalDate.of(2026, 7, 1),
                "FACTURA", null, null, BigDecimal.ZERO, null, List.of());

        assertThatThrownBy(() -> service.updateFactura(facturaId, tenantId, request))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("al menos un item");
    }

    @Test
    void updateFactura_whenAlreadyPaid_throws() {
        var tenantId = UUID.randomUUID();
        var facturaId = UUID.randomUUID();
        var factura = Factura.builder().id(facturaId).tenantId(tenantId).status("PAGADA").build();
        when(facturaRepository.findById(facturaId)).thenReturn(Optional.of(factura));

        var request = new FacturaRequest(tenantId, UUID.randomUUID(), null, LocalDate.of(2026, 7, 1),
                "FACTURA", null, null, BigDecimal.ZERO, null, List.of());

        assertThatThrownBy(() -> service.updateFactura(facturaId, tenantId, request))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("REGISTRADA");
    }

    @Test
    void updateFactura_withValidRequest_updatesFactura() {
        var tenantId = UUID.randomUUID();
        var facturaId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        var proveedorId = UUID.randomUUID();
        var presentacion = mockPresentacion(productId, 1);
        var factura = Factura.builder().id(facturaId).tenantId(tenantId).providerId(proveedorId).status("REGISTRADA")
                .items(new ArrayList<>()).issueDate(LocalDate.of(2026, 6, 1)).build();
        when(facturaRepository.findById(facturaId)).thenReturn(Optional.of(factura));
        when(facturaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(proveedorRepository.findById(proveedorId)).thenReturn(Optional.of(
                Proveedor.builder().id(proveedorId).tenantId(tenantId).name("Prov").build()));
        mockPresentaciones(List.of(presentacion));

        var item = new ItemFacturaRequest(productId, presentacion.getId(), new BigDecimal("10"), new BigDecimal("5.50"), BigDecimal.ZERO, null, null, null, null, null);
        var request = new FacturaRequest(tenantId, proveedorId, null, LocalDate.of(2026, 7, 1),
                "FACTURA", "EFECTIVO", null, BigDecimal.ZERO, null, List.of(item));

        when(mapper.toItemResponseList(anyList())).thenReturn(List.of());
        when(mapper.toResponse(any(), anyList())).thenAnswer(i -> {
            Factura f = i.getArgument(0);
            return new FacturaResponse(f.getId(), f.getTenantId(), f.getProviderId(), null, null, null,
                    f.getInvoiceNumber(), f.getIssueDate(), f.getType(), f.getGlobalDiscount(),
                    f.getPaymentMethod(), null, f.getStatus(), f.getTotal(), List.of(), null);
        });

        var result = service.updateFactura(facturaId, tenantId, request);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("REGISTRADA");
        verify(facturaRepository, times(1)).save(any());
        verify(jdbc, atLeastOnce()).update(anyString(), any(), any(), any(), any(), any());
    }

    @Test
    void findProveedor_withWrongTenant_throws404() {
        var tenantId = UUID.randomUUID();
        var otherTenant = UUID.randomUUID();
        var proveedor = Proveedor.builder().id(UUID.randomUUID()).tenantId(otherTenant).build();
        when(proveedorRepository.findById(proveedor.getId())).thenReturn(Optional.of(proveedor));

        assertThatThrownBy(() -> service.findProveedor(proveedor.getId(), tenantId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Proveedor not found");
    }

    // -- Colaborador edge cases --

    @Test
    void createFactura_gastoOperativoSinItems_withColaboradorId_setsColaboradorId() {
        var tenantId = UUID.randomUUID();
        var colaboradorId = UUID.randomUUID();
        var colaborador = Collaborador.builder().id(colaboradorId).tenantId(tenantId).nombre("Juan Pérez").build();
        when(collaboradorRepository.findById(colaboradorId)).thenReturn(Optional.of(colaborador));
        when(facturaRepository.findMaxInvoiceNumber(eq(tenantId), anyString())).thenReturn(Optional.empty());
        when(facturaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toItemResponseList(anyList())).thenReturn(List.of());
        when(mapper.toResponse(any(), anyList())).thenAnswer(i -> {
            Factura f = i.getArgument(0);
            return new FacturaResponse(f.getId(), f.getTenantId(), null, null,
                    f.getColaboradorId(), null,
                    f.getInvoiceNumber(), f.getIssueDate(), f.getType(), null,
                    null, "SALARIOS", f.getStatus(), f.getTotal(), List.of(), null);
        });

        var request = new FacturaRequest(tenantId, null, colaboradorId, LocalDate.of(2026, 8, 1),
                "GASTO_OPERATIVO", null, "SALARIOS", null, new BigDecimal("150.00"), List.of());

        var result = service.createFactura(request);

        var captor = ArgumentCaptor.forClass(Factura.class);
        verify(facturaRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getColaboradorId()).isEqualTo(colaboradorId);
        assertThat(saved.getColaborador()).isEqualTo(colaborador);
        assertThat(result.total()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void createFactura_gastoOperativoSinItems_withoutColaboradorId_leavesNull() {
        var tenantId = UUID.randomUUID();
        when(facturaRepository.findMaxInvoiceNumber(eq(tenantId), anyString())).thenReturn(Optional.empty());
        when(facturaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toItemResponseList(anyList())).thenReturn(List.of());
        when(mapper.toResponse(any(), anyList())).thenAnswer(i -> {
            Factura f = i.getArgument(0);
            return new FacturaResponse(f.getId(), f.getTenantId(), null, null,
                    null, null,
                    f.getInvoiceNumber(), f.getIssueDate(), f.getType(), null,
                    null, "SALARIOS", f.getStatus(), f.getTotal(), List.of(), null);
        });

        var request = new FacturaRequest(tenantId, null, null, LocalDate.of(2026, 8, 1),
                "GASTO_OPERATIVO", null, "SALARIOS", null, new BigDecimal("100.00"), List.of());

        service.createFactura(request);

        var captor = ArgumentCaptor.forClass(Factura.class);
        verify(facturaRepository).save(captor.capture());
        assertThat(captor.getValue().getColaboradorId()).isNull();
        assertThat(captor.getValue().getColaborador()).isNull();
    }

    @Test
    void createFactura_gastoOperativoSinItems_withColaboradorId_publishesEvent() {
        var tenantId = UUID.randomUUID();
        var colaboradorId = UUID.randomUUID();
        var colaborador = Collaborador.builder().id(colaboradorId).tenantId(tenantId).nombre("Ana").build();
        when(collaboradorRepository.findById(colaboradorId)).thenReturn(Optional.of(colaborador));
        when(facturaRepository.findMaxInvoiceNumber(eq(tenantId), anyString())).thenReturn(Optional.empty());
        when(facturaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toItemResponseList(anyList())).thenReturn(List.of());
        when(mapper.toResponse(any(), anyList())).thenAnswer(i -> {
            Factura f = i.getArgument(0);
            return new FacturaResponse(f.getId(), f.getTenantId(), null, null,
                    f.getColaboradorId(), null,
                    null, null, null, null, null, null, f.getStatus(), null, List.of(), null);
        });

        service.createFactura(new FacturaRequest(tenantId, null, colaboradorId, LocalDate.of(2026, 8, 1),
                "GASTO_OPERATIVO", null, "SALARIOS", null, BigDecimal.TEN, List.of()));

        var captor = ArgumentCaptor.forClass(FacturaCreadaEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().factura().getColaboradorId()).isEqualTo(colaboradorId);
    }

    @Test
    void createFactura_gastoOperativoSinItems_withNonExistentColaborador_throws() {
        var tenantId = UUID.randomUUID();
        var fakeId = UUID.randomUUID();
        when(collaboradorRepository.findById(fakeId)).thenReturn(Optional.empty());

        var request = new FacturaRequest(tenantId, null, fakeId, LocalDate.of(2026, 8, 1),
                "GASTO_OPERATIVO", null, "SALARIOS", null, BigDecimal.TEN, List.of());

        assertThatThrownBy(() -> service.createFactura(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Colaborador not found");
    }

    @Test
    void createFactura_gastoOperativoSinItems_withColaboradorFromOtherTenant_throws() {
        var tenantId = UUID.randomUUID();
        var otherTenant = UUID.randomUUID();
        var colaboradorId = UUID.randomUUID();
        var colaborador = Collaborador.builder().id(colaboradorId).tenantId(otherTenant).nombre("Otro").build();
        when(collaboradorRepository.findById(colaboradorId)).thenReturn(Optional.of(colaborador));

        var request = new FacturaRequest(tenantId, null, colaboradorId, LocalDate.of(2026, 8, 1),
                "GASTO_OPERATIVO", null, "SALARIOS", null, BigDecimal.TEN, List.of());

        assertThatThrownBy(() -> service.createFactura(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Colaborador not found");
    }

    @Test
    void updateFactura_gastoOperativoSinItems_setsColaboradorId() {
        var tenantId = UUID.randomUUID();
        var facturaId = UUID.randomUUID();
        var colaboradorId = UUID.randomUUID();
        var colaborador = Collaborador.builder().id(colaboradorId).tenantId(tenantId).nombre("Carlos").build();
        var factura = Factura.builder().id(facturaId).tenantId(tenantId).status("REGISTRADA")
                .items(new ArrayList<>()).issueDate(LocalDate.of(2026, 6, 1)).build();
        when(facturaRepository.findById(facturaId)).thenReturn(Optional.of(factura));
        when(facturaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(collaboradorRepository.findById(colaboradorId)).thenReturn(Optional.of(colaborador));
        when(mapper.toItemResponseList(anyList())).thenReturn(List.of());
        when(mapper.toResponse(any(), anyList())).thenAnswer(i -> {
            Factura f = i.getArgument(0);
            return new FacturaResponse(f.getId(), f.getTenantId(), null, null,
                    f.getColaboradorId(), null,
                    f.getInvoiceNumber(), f.getIssueDate(), f.getType(), null,
                    null, "SALARIOS", f.getStatus(), f.getTotal(), List.of(), null);
        });

        var request = new FacturaRequest(tenantId, null, colaboradorId, LocalDate.of(2026, 8, 1),
                "GASTO_OPERATIVO", null, "SALARIOS", null, new BigDecimal("200.00"), List.of());

        service.updateFactura(facturaId, tenantId, request);

        var captor = ArgumentCaptor.forClass(Factura.class);
        verify(facturaRepository).save(captor.capture());
        assertThat(captor.getValue().getColaboradorId()).isEqualTo(colaboradorId);
        assertThat(captor.getValue().getColaborador()).isEqualTo(colaborador);
    }

    @Test
    void updateFactura_gastoOperativoSinItems_removesColaboradorId() {
        var tenantId = UUID.randomUUID();
        var facturaId = UUID.randomUUID();
        var oldColaboradorId = UUID.randomUUID();
        var factura = Factura.builder().id(facturaId).tenantId(tenantId).status("REGISTRADA")
                .colaboradorId(oldColaboradorId)
                .items(new ArrayList<>()).issueDate(LocalDate.of(2026, 6, 1)).build();
        when(facturaRepository.findById(facturaId)).thenReturn(Optional.of(factura));
        when(facturaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toItemResponseList(anyList())).thenReturn(List.of());
        when(mapper.toResponse(any(), anyList())).thenAnswer(i -> {
            Factura f = i.getArgument(0);
            return new FacturaResponse(f.getId(), f.getTenantId(), null, null,
                    f.getColaboradorId(), null,
                    f.getInvoiceNumber(), f.getIssueDate(), f.getType(), null,
                    null, "SALARIOS", f.getStatus(), f.getTotal(), List.of(), null);
        });

        var request = new FacturaRequest(tenantId, null, null, LocalDate.of(2026, 8, 1),
                "GASTO_OPERATIVO", null, "SALARIOS", null, new BigDecimal("100.00"), List.of());

        service.updateFactura(facturaId, tenantId, request);

        var captor = ArgumentCaptor.forClass(Factura.class);
        verify(facturaRepository).save(captor.capture());
        assertThat(captor.getValue().getColaboradorId()).isNull();
        assertThat(captor.getValue().getColaborador()).isNull();
    }
}

package core_pymes.unit;

import core_pymes.invoice.domain.Factura;
import core_pymes.invoice.domain.ItemFactura;
import core_pymes.invoice.domain.Proveedor;
import core_pymes.invoice.dto.*;
import core_pymes.invoice.event.FacturaCreadaEvent;
import core_pymes.invoice.event.FacturaPagadaEvent;
import core_pymes.invoice.mapper.FacturaMapper;
import core_pymes.invoice.repository.FacturaRepository;
import core_pymes.invoice.repository.ProveedorRepository;
import core_pymes.invoice.service.impl.FacturaServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    @Mock FacturaRepository facturaRepository;
    @Mock FacturaMapper mapper;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock JdbcTemplate jdbc;
    @InjectMocks FacturaServiceImpl service;

    @Test
    void createFactura_withValidRequest_calculatesTotalAndGeneratesNumber() {
        var tenantId = UUID.randomUUID();
        var proveedorId = UUID.randomUUID();
        var proveedor = Proveedor.builder().id(proveedorId).tenantId(tenantId).name("Distribuidora ABC").build();
        when(proveedorRepository.findById(proveedorId)).thenReturn(Optional.of(proveedor));
        when(facturaRepository.findMaxInvoiceNumber(eq(tenantId), anyString())).thenReturn(Optional.empty());
        when(jdbc.queryForObject(anyString(), eq(String.class), any())).thenReturn("Arroz");

        var item = new ItemFacturaRequest(UUID.randomUUID(), new BigDecimal("10"), new BigDecimal("5.50"), BigDecimal.ZERO);
        var request = new FacturaRequest(tenantId, proveedorId, LocalDate.of(2026, 6, 1),
                "FACTURA", "EFECTIVO", BigDecimal.ZERO, List.of(item));

        var savedFactura = Factura.builder().id(UUID.randomUUID()).tenantId(tenantId).providerId(proveedorId)
                .invoiceNumber("F-PROV-2026-0001").total(new BigDecimal("55.00")).status("REGISTRADA")
                .issueDate(request.fecha()).type(request.tipo()).build();
        when(facturaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var itemResponse = new ItemFacturaResponse(UUID.randomUUID(), UUID.randomUUID(), "Arroz",
                new BigDecimal("10"), new BigDecimal("5.50"), BigDecimal.ZERO, new BigDecimal("55.00"));
        when(mapper.toItemResponseList(anyList())).thenReturn(List.of(itemResponse));
        var facturaResponse = new FacturaResponse(savedFactura.getId(), tenantId, proveedorId, "Distribuidora ABC",
                "F-PROV-2026-0001", LocalDate.of(2026, 6, 1), "FACTURA",
                BigDecimal.ZERO, "EFECTIVO", "REGISTRADA", new BigDecimal("55.00"), List.of(itemResponse), null);
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
        var proveedor = Proveedor.builder().id(proveedorId).tenantId(tenantId).build();
        when(proveedorRepository.findById(proveedorId)).thenReturn(Optional.of(proveedor));
        when(facturaRepository.findMaxInvoiceNumber(eq(tenantId), anyString())).thenReturn(Optional.empty());
        when(jdbc.queryForObject(anyString(), eq(String.class), any())).thenReturn("Producto");

        var item = new ItemFacturaRequest(UUID.randomUUID(), new BigDecimal("5"), new BigDecimal("20.00"), BigDecimal.ZERO);
        var request = new FacturaRequest(tenantId, proveedorId, LocalDate.of(2026, 6, 1),
                "FACTURA", null, new BigDecimal("10.00"), List.of(item));

        var savedFactura = Factura.builder().id(UUID.randomUUID()).tenantId(tenantId)
                .invoiceNumber("F-PROV-2026-0001").total(new BigDecimal("90.00")).status("REGISTRADA")
                .issueDate(request.fecha()).type(request.tipo()).build();
        when(facturaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toItemResponseList(anyList())).thenReturn(List.of());
        when(mapper.toResponse(any(), anyList())).thenAnswer(i -> {
            Factura f = i.getArgument(0);
            return new FacturaResponse(f.getId(), f.getTenantId(), f.getProviderId(), null,
                    f.getInvoiceNumber(), f.getIssueDate(), f.getType(), f.getGlobalDiscount(),
                    f.getPaymentMethod(), f.getStatus(), f.getTotal(), List.of(), null);
        });

        var result = service.createFactura(request);

        // 5 × 20.00 = 100.00 - 10.00 descuento = 90.00
        assertThat(result.total()).isEqualByComparingTo(new BigDecimal("90.00"));
        assertThat(result.globalDiscount()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void createFactura_withValidRequest_publishesEvent() {
        var tenantId = UUID.randomUUID();
        var proveedorId = UUID.randomUUID();
        var proveedor = Proveedor.builder().id(proveedorId).tenantId(tenantId).build();
        when(proveedorRepository.findById(proveedorId)).thenReturn(Optional.of(proveedor));
        when(facturaRepository.findMaxInvoiceNumber(eq(tenantId), anyString())).thenReturn(Optional.empty());
        when(jdbc.queryForObject(anyString(), eq(String.class), any())).thenReturn("Prod");
        when(facturaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toItemResponseList(anyList())).thenReturn(List.of());
        when(mapper.toResponse(any(), anyList())).thenReturn(
                new FacturaResponse(null, tenantId, proveedorId, null, null, null, null,
                        null, null, null, null, List.of(), null));

        service.createFactura(new FacturaRequest(tenantId, proveedorId, LocalDate.of(2026, 6, 1),
                "FACTURA", null, BigDecimal.ZERO,
                List.of(new ItemFacturaRequest(UUID.randomUUID(), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO))));

        var captor = ArgumentCaptor.forClass(FacturaCreadaEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().factura()).isNotNull();
    }

    @Test
    void pagarFactura_whenRegistered_marksAsPaidAndPublishesEvent() {
        var tenantId = UUID.randomUUID();
        var facturaId = UUID.randomUUID();
        var factura = Factura.builder().id(facturaId).tenantId(tenantId).status("REGISTRADA").items(List.of()).build();
        when(facturaRepository.findById(facturaId)).thenReturn(Optional.of(factura));
        when(facturaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toItemResponseList(anyList())).thenReturn(List.of());
        when(mapper.toResponse(any(), anyList())).thenAnswer(i -> {
            Factura f = i.getArgument(0);
            return new FacturaResponse(f.getId(), f.getTenantId(), null, null,
                    null, null, null, null, null, f.getStatus(), null, List.of(), null);
        });

        var result = service.pagarFactura(facturaId, tenantId);

        assertThat(result.status()).isEqualTo("PAGADA");
        var captor = ArgumentCaptor.forClass(FacturaPagadaEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().factura().getStatus()).isEqualTo("PAGADA");
    }

    @Test
    void pagarFactura_whenAlreadyPaid_throws() {
        var tenantId = UUID.randomUUID();
        var factura = Factura.builder().id(UUID.randomUUID()).tenantId(tenantId).status("PAGADA").build();
        when(facturaRepository.findById(factura.getId())).thenReturn(Optional.of(factura));

        assertThatThrownBy(() -> service.pagarFactura(factura.getId(), tenantId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already");
    }

    @Test
    void deleteFactura_whenPaid_throws() {
        var tenantId = UUID.randomUUID();
        var factura = Factura.builder().id(UUID.randomUUID()).tenantId(tenantId).status("PAGADA").build();
        when(facturaRepository.findById(factura.getId())).thenReturn(Optional.of(factura));

        assertThatThrownBy(() -> service.deleteFactura(factura.getId(), tenantId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete");
    }

    @Test
    void findProveedor_withWrongTenant_throws404() {
        var tenantId = UUID.randomUUID();
        var otherTenant = UUID.randomUUID();
        var proveedor = Proveedor.builder().id(UUID.randomUUID()).tenantId(otherTenant).build();
        when(proveedorRepository.findById(proveedor.getId())).thenReturn(Optional.of(proveedor));

        assertThatThrownBy(() -> service.findProveedor(proveedor.getId(), tenantId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Proveedor not found");
    }
}

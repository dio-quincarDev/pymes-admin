package core_pymes.jpa;

import core_pymes.invoice.domain.Factura;
import core_pymes.invoice.domain.ItemFactura;
import core_pymes.invoice.domain.Proveedor;
import core_pymes.product.domain.Presentacion;
import core_pymes.product.domain.Producto;
import core_pymes.invoice.repository.FacturaRepository;
import core_pymes.invoice.repository.ProveedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JPA: Factura + Proveedor repositories")
class FacturaRepositoryTest extends AbstractJpaTest {

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private ProveedorRepository proveedorRepository;

    private UUID tenantA;
    private UUID tenantB;
    private Proveedor providerA;
    private Proveedor providerB;
    private Factura invoice1;
    private Factura invoice2;

    @BeforeEach
    void setUp() {
        tenantA = UUID.randomUUID();
        tenantB = UUID.randomUUID();

        providerA = em.persistFlushFind(Proveedor.builder().tenantId(tenantA).name("Distribuidora A").ruc("123").build());
        providerB = em.persistFlushFind(Proveedor.builder().tenantId(tenantA).name("Distribuidora B").ruc("456").build());
        var providerOther = em.persistFlushFind(Proveedor.builder().tenantId(tenantB).name("Other Corp").ruc("999").build());

        invoice1 = em.persistFlushFind(Factura.builder()
                .tenantId(tenantA).providerId(providerA.getId()).invoiceNumber("F-PROV-2026-0001")
                .issueDate(LocalDate.of(2026, 6, 1)).type("FACTURA").status("REGISTRADA")
                .globalDiscount(BigDecimal.ZERO).total(new BigDecimal("100.00")).build());
        invoice2 = em.persistFlushFind(Factura.builder()
                .tenantId(tenantA).providerId(providerB.getId()).invoiceNumber("F-PROV-2026-0002")
                .issueDate(LocalDate.of(2026, 6, 15)).type("FACTURA").status("PAGADA")
                .globalDiscount(BigDecimal.ZERO).total(new BigDecimal("200.00")).build());
        em.persistFlushFind(Factura.builder()
                .tenantId(tenantB).providerId(providerOther.getId()).invoiceNumber("F-PROV-2026-0001")
                .issueDate(LocalDate.of(2026, 6, 1)).type("FACTURA").status("REGISTRADA")
                .globalDiscount(BigDecimal.ZERO).total(new BigDecimal("50.00")).build());
        em.clear();
    }

    @Nested
    @DisplayName("FacturaRepository")
    class FacturaTests {

        @Test
        @DisplayName("findByTenantIdOrderByCreatedAtDesc returns invoices for tenant")
        void findByTenantIdOrderByCreatedAtDesc_returnsTenantInvoices() {
            var result = facturaRepository.findByTenantIdOrderByCreatedAtDesc(tenantA);
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("findByIdAndTenantId matches both id and tenant")
        void findByIdAndTenantId_matchesIdAndTenant() {
            var found = facturaRepository.findByIdAndTenantId(invoice1.getId(), tenantA);
            assertThat(found).isPresent();
            assertThat(found.get().getInvoiceNumber()).isEqualTo("F-PROV-2026-0001");
        }

        @Test
        @DisplayName("findByIdAndTenantId returns empty for wrong tenant")
        void findByIdAndTenantId_wrongTenant_returnsEmpty() {
            var found = facturaRepository.findByIdAndTenantId(invoice1.getId(), tenantB);
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("findMaxInvoiceNumber returns highest number for year prefix")
        void findMaxInvoiceNumber_returnsMax() {
            var max = facturaRepository.findMaxInvoiceNumber(tenantA, "F-PROV-2026-%");
            assertThat(max).isPresent().hasValue("F-PROV-2026-0002");
        }

        @Test
        @DisplayName("findMaxInvoiceNumber returns empty when no invoices match")
        void findMaxInvoiceNumber_noMatch_returnsEmpty() {
            var max = facturaRepository.findMaxInvoiceNumber(tenantA, "F-PROV-2025-%");
            assertThat(max).isEmpty();
        }

        @Test
        @DisplayName("findMaxInvoiceNumber scoped by tenant")
        void findMaxInvoiceNumber_tenantScoped() {
            var max = facturaRepository.findMaxInvoiceNumber(tenantA, "F-PROV-2026-%");
            assertThat(max).isPresent().hasValue("F-PROV-2026-0002");

            var maxB = facturaRepository.findMaxInvoiceNumber(tenantB, "F-PROV-2026-%");
            assertThat(maxB).isPresent().hasValue("F-PROV-2026-0001");
        }

        @Test
        @DisplayName("countByTenantId returns count for tenant")
        void countByTenantId_returnsCount() {
            assertThat(facturaRepository.countByTenantId(tenantA)).isEqualTo(2);
            assertThat(facturaRepository.countByTenantId(tenantB)).isEqualTo(1);
        }

        @Test
        @DisplayName("soft delete excludes deleted invoices")
        void softDelete_excludesDeleted() {
            facturaRepository.deleteById(invoice1.getId());
            em.flush();
            em.clear();

            assertThat(facturaRepository.findByIdAndTenantId(invoice1.getId(), tenantA)).isEmpty();
            assertThat(facturaRepository.findByTenantIdOrderByCreatedAtDesc(tenantA)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("ProveedorRepository")
    class ProveedorTests {

        @Test
        @DisplayName("findByTenantId returns providers for tenant")
        void findByTenantId_returnsTenantProviders() {
            var result = proveedorRepository.findByTenantId(tenantA);
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("findByIdAndTenantId matches both id and tenant")
        void findByIdAndTenantId_matchesIdAndTenant() {
            var found = proveedorRepository.findByIdAndTenantId(providerA.getId(), tenantA);
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Distribuidora A");
        }

        @Test
        @DisplayName("findByIdAndTenantId returns empty for wrong tenant")
        void findByIdAndTenantId_wrongTenant_returnsEmpty() {
            var found = proveedorRepository.findByIdAndTenantId(providerA.getId(), tenantB);
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("existsByTenantIdAndNameIgnoreCase is case-insensitive and tenant-scoped")
        void existsByTenantIdAndNameIgnoreCase_caseInsensitiveAndScoped() {
            assertThat(proveedorRepository.existsByTenantIdAndNameIgnoreCase(tenantA, "DISTRIBUIDORA A")).isTrue();
            assertThat(proveedorRepository.existsByTenantIdAndNameIgnoreCase(tenantA, "distribuidora a")).isTrue();
            assertThat(proveedorRepository.existsByTenantIdAndNameIgnoreCase(tenantB, "Distribuidora A")).isFalse();
        }

        @Test
        @DisplayName("soft delete excludes deleted providers")
        void softDelete_excludesDeleted() {
            proveedorRepository.deleteById(providerA.getId());
            em.flush();
            em.clear();

            assertThat(proveedorRepository.findByTenantId(tenantA)).hasSize(1);
            assertThat(proveedorRepository.findById(providerA.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Factura — ItemFactura cascade")
    class CascadeTests {

        @Test
        @DisplayName("persisting Factura cascades to items")
        void persistFactura_cascadesToItems() {
            var product = em.persistFlushFind(Producto.builder().tenantId(tenantA).name("Arroz").build());
            var presentacion = em.persistFlushFind(Presentacion.builder().producto(product).name("Bolsa 1kg").conversion(1).build());
            var invoice = Factura.builder()
                    .tenantId(tenantA).providerId(providerA.getId()).invoiceNumber("F-PROV-2026-0003")
                    .issueDate(LocalDate.of(2026, 7, 1)).type("FACTURA").status("REGISTRADA")
                    .globalDiscount(BigDecimal.ZERO).total(new BigDecimal("55.00")).build();
            invoice.getItems().add(ItemFactura.builder()
                    .factura(invoice).productId(product.getId()).productName("Arroz")
                    .presentacionId(presentacion.getId()).conversionFactor(1)
                    .quantity(new BigDecimal("10")).unitPrice(new BigDecimal("5.50"))
                    .discount(BigDecimal.ZERO).subtotal(new BigDecimal("55.00")).build());

            em.persistAndFlush(invoice);
            em.clear();

            var saved = facturaRepository.findByIdAndTenantId(invoice.getId(), tenantA);
            assertThat(saved).isPresent();
            assertThat(saved.get().getItems()).hasSize(1);
            assertThat(saved.get().getItems().get(0).getProductName()).isEqualTo("Arroz");
        }

        @Test
        @DisplayName("removing Factura cascades to items (orphanRemoval)")
        void removeFactura_cascadesToItems() {
            facturaRepository.deleteById(invoice1.getId());
            em.flush();
            em.clear();

            var items = jdbc.queryForList("SELECT * FROM core.invoice_items WHERE invoice_id = ?", invoice1.getId());
            assertThat(items).isEmpty();
        }
    }
}

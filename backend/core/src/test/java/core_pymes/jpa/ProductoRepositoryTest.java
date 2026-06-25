package core_pymes.jpa;

import core_pymes.product.domain.Presentacion;
import core_pymes.product.domain.Producto;
import core_pymes.product.repository.PresentacionRepository;
import core_pymes.product.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JPA: Producto + Presentacion repositories")
class ProductoRepositoryTest extends AbstractJpaTest {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private PresentacionRepository presentacionRepository;

    private UUID tenantA;
    private UUID tenantB;
    private Producto productA;

    @BeforeEach
    void setUp() {
        tenantA = UUID.randomUUID();
        tenantB = UUID.randomUUID();
        productA = em.persistFlushFind(Producto.builder().tenantId(tenantA).name("Arroz").sku("ARZ-001").build());
        em.persistFlushFind(Producto.builder().tenantId(tenantA).name("Frijol").sku("FRJ-001").build());
        em.persistFlushFind(Producto.builder().tenantId(tenantB).name("Arroz B").sku("ARZ-B-001").build());
        em.clear();
    }

    @Nested
    @DisplayName("ProductoRepository")
    class ProductoTests {

        @Test
        @DisplayName("findByTenantId returns only products for that tenant")
        void findByTenantId_returnsTenantProducts() {
            var result = productoRepository.findByTenantId(tenantA);
            assertThat(result).hasSize(2).extracting(Producto::getSku).containsExactlyInAnyOrder("ARZ-001", "FRJ-001");
        }

        @Test
        @DisplayName("findByIdAndTenantId matches both id and tenant")
        void findByIdAndTenantId_matchesIdAndTenant() {
            var found = productoRepository.findByIdAndTenantId(productA.getId(), tenantA);
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Arroz");
        }

        @Test
        @DisplayName("findByIdAndTenantId returns empty for wrong tenant")
        void findByIdAndTenantId_wrongTenant_returnsEmpty() {
            var found = productoRepository.findByIdAndTenantId(productA.getId(), tenantB);
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("existsByTenantIdAndSku returns true for existing SKU")
        void existsByTenantIdAndSku_found() {
            assertThat(productoRepository.existsByTenantIdAndSku(tenantA, "ARZ-001")).isTrue();
        }

        @Test
        @DisplayName("existsByTenantIdAndSku returns false for missing SKU")
        void existsByTenantIdAndSku_notFound() {
            assertThat(productoRepository.existsByTenantIdAndSku(tenantA, "NONEXISTENT")).isFalse();
        }

        @Test
        @DisplayName("existsByTenantIdAndSku scoped by tenant")
        void existsByTenantIdAndSku_tenantScoped() {
            assertThat(productoRepository.existsByTenantIdAndSku(tenantB, "ARZ-001")).isFalse();
        }

        @Test
        @DisplayName("soft delete excludes deleted products from queries")
        void softDelete_excludesDeleted() {
            productoRepository.deleteById(productA.getId());
            em.flush();
            em.clear();

            assertThat(productoRepository.findByIdAndTenantId(productA.getId(), tenantA)).isEmpty();
            assertThat(productoRepository.findByTenantId(tenantA)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("PresentacionRepository")
    class PresentacionTests {

        private Presentacion presA;

        @BeforeEach
        void setUpPresentaciones() {
            presA = em.persistFlushFind(Presentacion.builder().producto(productA).name("Bolsa 1kg").conversion(1).build());
            em.persistFlushFind(Presentacion.builder().producto(productA).name("Saco 50kg").conversion(50).build());
            em.clear();
        }

        @Test
        @DisplayName("findByProductoId returns all presentations for a product")
        void findByProductoId_returnsAll() {
            var result = presentacionRepository.findByProductoId(productA.getId());
            assertThat(result).hasSize(2).extracting(Presentacion::getName)
                    .containsExactlyInAnyOrder("Bolsa 1kg", "Saco 50kg");
        }

        @Test
        @DisplayName("findByProductoIdAndIsActiveTrue filters inactive")
        void findByProductoIdAndIsActiveTrue_filtersInactive() {
            presentacionRepository.deleteById(presA.getId());
            em.flush();
            em.clear();

            var result = presentacionRepository.findByProductoIdAndIsActiveTrue(productA.getId());
            assertThat(result).hasSize(1).extracting(Presentacion::getName).containsExactly("Saco 50kg");
        }

        @Test
        @DisplayName("existsByProductoIdAndNameIgnoreCase is case-insensitive")
        void existsByProductoIdAndNameIgnoreCase_caseInsensitive() {
            assertThat(presentacionRepository.existsByProductoIdAndNameIgnoreCase(productA.getId(), "BOLSA 1KG")).isTrue();
            assertThat(presentacionRepository.existsByProductoIdAndNameIgnoreCase(productA.getId(), "bolsa 1kg")).isTrue();
            assertThat(presentacionRepository.existsByProductoIdAndNameIgnoreCase(productA.getId(), "No existe")).isFalse();
        }

        @Test
        @DisplayName("soft delete excludes deleted presentations")
        void softDelete_excludesDeleted() {
            presentacionRepository.deleteById(presA.getId());
            em.flush();
            em.clear();

            assertThat(presentacionRepository.findByProductoId(productA.getId())).hasSize(1);
            assertThat(presentacionRepository.findById(presA.getId())).isEmpty();
        }
    }
}

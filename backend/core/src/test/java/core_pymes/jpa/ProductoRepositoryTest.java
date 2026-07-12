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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
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
    private Producto productLeche;
    private Producto productQueso;

    @BeforeEach
    void setUp() {
        tenantA = UUID.randomUUID();
        tenantB = UUID.randomUUID();
        productA = em.persistFlushFind(Producto.builder().tenantId(tenantA).name("Arroz").sku("ARZ-001").category("ABARROTES").build());
        em.persistFlushFind(Producto.builder().tenantId(tenantA).name("Frijol").sku("FRJ-001").category("ABARROTES").build());
        productLeche = em.persistFlushFind(Producto.builder().tenantId(tenantA).name("Leche").sku("LEC-001").category("LACTEOS").build());
        productQueso = em.persistFlushFind(Producto.builder().tenantId(tenantA).name("Queso").sku("QUE-001").category("LACTEOS").build());
        em.persistFlushFind(Producto.builder().tenantId(tenantA).name("Jabon").sku("JAB-001").category("LIMPIEZA").build());
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
            assertThat(result).hasSize(5).extracting(Producto::getSku)
                    .containsExactlyInAnyOrder("ARZ-001", "FRJ-001", "LEC-001", "QUE-001", "JAB-001");
        }

        @Test
        @DisplayName("findByTenantId paginated returns correct page")
        void findByTenantId_paginated() {
            var pageable = PageRequest.of(0, 3, Sort.by("name").ascending());
            var page = productoRepository.findByTenantId(tenantA, pageable);

            assertThat(page.getContent()).hasSize(3);
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(2);
            assertThat(page.getContent().get(0).getName()).isEqualTo("Arroz");
        }

        @Test
        @DisplayName("findByTenantId page beyond total returns empty content")
        void findByTenantId_pageBeyondTotal() {
            var pageable = PageRequest.of(99, 3, Sort.by("name").ascending());
            var page = productoRepository.findByTenantId(tenantA, pageable);
            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("findByTenantId page size 1 paginates correctly")
        void findByTenantId_pageSizeOne() {
            var pageable = PageRequest.of(0, 1, Sort.by("name").ascending());
            var page = productoRepository.findByTenantId(tenantA, pageable);
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(5);
            assertThat(page.getContent().get(0).getName()).isEqualTo("Arroz");
        }

        @Test
        @DisplayName("findByTenantIdAndCategory returns only matching category")
        void findByTenantIdAndCategory_filters() {
            var pageable = PageRequest.of(0, 10);
            var page = productoRepository.findByTenantIdAndCategory(tenantA, "LACTEOS", pageable);

            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getContent()).extracting(Producto::getName)
                    .containsExactlyInAnyOrder("Leche", "Queso");
            assertThat(page.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("findByTenantIdAndCategory returns empty for non-existent category")
        void findByTenantIdAndCategory_noMatch() {
            var pageable = PageRequest.of(0, 10);
            var page = productoRepository.findByTenantIdAndCategory(tenantA, "INEXISTENTE", pageable);

            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("findByTenantIdAndNameContainingIgnoreCase searches by name case-insensitive")
        void findByNameContainingIgnoreCase_filters() {
            var pageable = PageRequest.of(0, 10);
            var page = productoRepository.findByTenantIdAndNameContainingIgnoreCase(tenantA, "AR", pageable);

            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getName()).isEqualTo("Arroz");
        }

        @Test
        @DisplayName("findByTenantIdAndNameContainingIgnoreCase partial name match")
        void findByNameContainingIgnoreCase_partialMatch() {
            var pageable = PageRequest.of(0, 10);
            var page = productoRepository.findByTenantIdAndNameContainingIgnoreCase(tenantA, "rij", pageable);

            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getName()).isEqualTo("Frijol");
        }

        @Test
        @DisplayName("findByTenantIdAndNameContainingIgnoreCase with special SQL chars does not error")
        void findByNameContainingIgnoreCase_specialChars() {
            var pageable = PageRequest.of(0, 10);
            var byPercent = productoRepository.findByTenantIdAndNameContainingIgnoreCase(tenantA, "%", pageable);
            assertThat(byPercent).isNotNull();
            var byUnderscore = productoRepository.findByTenantIdAndNameContainingIgnoreCase(tenantA, "_", pageable);
            assertThat(byUnderscore).isNotNull();
            var byQuote = productoRepository.findByTenantIdAndNameContainingIgnoreCase(tenantA, "'", pageable);
            assertThat(byQuote).isNotNull();
            var byApostrophe = productoRepository.findByTenantIdAndNameContainingIgnoreCase(tenantA, "O'Brien", pageable);
            assertThat(byApostrophe).isNotNull();
        }

        @Test
        @DisplayName("findByTenantIdAndNameContainingIgnoreCase no match returns empty")
        void findByNameContainingIgnoreCase_noMatch() {
            var pageable = PageRequest.of(0, 10);
            var page = productoRepository.findByTenantIdAndNameContainingIgnoreCase(tenantA, "xyzzy", pageable);
            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("findByTenantIdAndNameContainingIgnoreCase empty string matches all")
        void findByNameContainingIgnoreCase_emptyString() {
            var pageable = PageRequest.of(0, 10);
            var page = productoRepository.findByTenantIdAndNameContainingIgnoreCase(tenantA, "", pageable);
            assertThat(page.getContent()).hasSize(5);
        }

        @Test
        @DisplayName("findByTenantIdAndCategoryAndNameContainingIgnoreCase combines both filters")
        void findCombined_filters() {
            var pageable = PageRequest.of(0, 10);
            var page = productoRepository.findByTenantIdAndCategoryAndNameContainingIgnoreCase(
                    tenantA, "ABARROTES", "ar", pageable);

            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getName()).isEqualTo("Arroz");
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
        @DisplayName("findByTenantId for tenant with no products returns empty")
        void findByTenantId_emptyTenant() {
            assertThat(productoRepository.findByTenantId(UUID.randomUUID())).isEmpty();
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
            assertThat(productoRepository.findByTenantId(tenantA)).hasSize(4);
        }

        @Test
        @DisplayName("product with null providerId is returned correctly")
        void findByTenantId_nullProviderId() {
            var prod = em.persistFlushFind(Producto.builder().tenantId(tenantA).name("Sin Proveedor").build());
            em.clear();
            var found = productoRepository.findByIdAndTenantId(prod.getId(), tenantA);
            assertThat(found).isPresent();
            assertThat(found.get().getProviderId()).isNull();
        }
    }

    @Nested
    @DisplayName("PresentacionRepository — batch queries")
    class PresentacionBatchTests {

        private Presentacion presArroz;
        private Presentacion presLeche;

        @BeforeEach
        void setUpPresentaciones() {
            presArroz = em.persistFlushFind(Presentacion.builder().producto(productA).name("Bolsa 1kg").conversion(1).build());
            em.persistFlushFind(Presentacion.builder().producto(productA).name("Saco 50kg").conversion(50).build());
            presLeche = em.persistFlushFind(Presentacion.builder().producto(productLeche).name("Litro").conversion(1).build());
            em.persistFlushFind(Presentacion.builder().producto(productLeche).name("Galon").conversion(4).build());
            em.persistFlushFind(Presentacion.builder().producto(productQueso).name("Kg").conversion(1).build());
            em.clear();
        }

        @Test
        @DisplayName("findByProductoIdInAndIsActiveTrue returns presentations for all given product ids")
        void findByProductoIdIn_returnsAll() {
            var result = presentacionRepository.findByProductoIdInAndIsActiveTrue(
                    List.of(productA.getId(), productLeche.getId(), productQueso.getId()));

            assertThat(result).hasSize(5);
            assertThat(result).extracting(Presentacion::getName)
                    .containsExactlyInAnyOrder("Bolsa 1kg", "Saco 50kg", "Litro", "Galon", "Kg");
        }

        @Test
        @DisplayName("findByProductoIdInAndIsActiveTrue only for specified product ids")
        void findByProductoIdIn_limitedToIds() {
            var result = presentacionRepository.findByProductoIdInAndIsActiveTrue(List.of(productA.getId()));

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Presentacion::getName)
                    .containsExactlyInAnyOrder("Bolsa 1kg", "Saco 50kg");
        }

        @Test
        @DisplayName("findByProductoIdInAndIsActiveTrue returns empty for empty id list")
        void findByProductoIdIn_emptyList() {
            var result = presentacionRepository.findByProductoIdInAndIsActiveTrue(List.of());
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("findByProductoIdInAndIsActiveTrue filters inactive presentations")
        void findByProductoIdIn_filtersInactive() {
            presentacionRepository.deleteById(presArroz.getId());
            em.flush();
            em.clear();

            var result = presentacionRepository.findByProductoIdInAndIsActiveTrue(
                    List.of(productA.getId(), productLeche.getId()));

            assertThat(result).hasSize(3);
            assertThat(result).extracting(Presentacion::getName)
                    .containsExactlyInAnyOrder("Saco 50kg", "Litro", "Galon");
        }

        @Test
        @DisplayName("findByProductoIdInAndIsActiveTrue returns empty for non-existent ids")
        void findByProductoIdIn_nonExistentIds() {
            var result = presentacionRepository.findByProductoIdInAndIsActiveTrue(
                    List.of(UUID.randomUUID(), UUID.randomUUID()));
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("findByProductoIdInAndIsActiveTrue does not return presentations from other tenant products")
        void findByProductoIdIn_crossTenantIsolation() {
            var productB = em.persistFlushFind(Producto.builder().tenantId(tenantB).name("Producto B").build());
            em.persistFlushFind(Presentacion.builder().producto(productB).name("Presentacion B").conversion(1).build());
            em.clear();

            var result = presentacionRepository.findByProductoIdInAndIsActiveTrue(
                    List.of(productA.getId(), productLeche.getId(), productQueso.getId()));
            assertThat(result).extracting(Presentacion::getName)
                    .doesNotContain("Presentacion B");
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

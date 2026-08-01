package core_pymes.jpa;

import core_pymes.costos.domain.Collaborador;
import core_pymes.costos.domain.ConfigLaboral;
import core_pymes.costos.domain.GastoFijoRecurrente;
import core_pymes.costos.domain.TipoPago;
import core_pymes.costos.repository.CollaboradorRepository;
import core_pymes.costos.repository.ConfigLaboralRepository;
import core_pymes.costos.repository.GastoFijoRepository;
import core_pymes.gasto.domain.CategoriaGasto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JPA: costos repositories")
class CostosRepositoryTest extends AbstractJpaTest {

    @Autowired
    private CollaboradorRepository collaboradorRepository;
    @Autowired
    private GastoFijoRepository gastoFijoRepository;
    @Autowired
    private ConfigLaboralRepository configLaboralRepository;

    private UUID tenantA;
    private UUID tenantB;

    @BeforeEach
    void setUp() {
        tenantA = UUID.randomUUID();
        tenantB = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Collaborador")
    class CollaboradorTests {

        @Test
        @DisplayName("returns only collaborators for that tenant")
        void returns_tenantCollaborators() {
            em.persistFlushFind(Collaborador.builder()
                    .tenantId(tenantA).nombre("Ana").tipoPago(TipoPago.MENSUAL)
                    .monto(new BigDecimal("1500.00")).build());
            em.persistFlushFind(Collaborador.builder()
                    .tenantId(tenantB).nombre("Luis").tipoPago(TipoPago.DIARIO)
                    .monto(new BigDecimal("50.00")).build());
            em.clear();

            var result = collaboradorRepository.findByTenantIdOrderByCreatedAtDesc(tenantA);
            assertThat(result).hasSize(1)
                    .extracting(Collaborador::getNombre)
                    .containsExactly("Ana");
        }

        @Test
        @DisplayName("all tipo_pago values round-trip")
        void enumMapping_roundTrip() {
            for (var tipo : TipoPago.values()) {
                var c = em.persistFlushFind(Collaborador.builder()
                        .tenantId(tenantA).nombre("T").tipoPago(tipo).monto(BigDecimal.TEN).build());
                em.clear();
                assertThat(em.find(Collaborador.class, c.getId()).getTipoPago()).isEqualTo(tipo);
            }
        }

        @Test
        @DisplayName("soft delete excludes deleted from queries")
        void softDelete_excludesDeleted() {
            var c = em.persistFlushFind(Collaborador.builder()
                    .tenantId(tenantA).nombre("Ana").tipoPago(TipoPago.MENSUAL)
                    .monto(new BigDecimal("1500.00")).build());
            em.flush();
            em.clear();

            collaboradorRepository.deleteById(c.getId());
            em.flush();
            em.clear();

            assertThat(collaboradorRepository.findById(c.getId())).isEmpty();
            assertThat(collaboradorRepository.findByTenantIdOrderByCreatedAtDesc(tenantA)).isEmpty();
        }

        @Test
        @DisplayName("default activo is true")
        void defaultActivo() {
            var c = em.persistFlushFind(Collaborador.builder()
                    .tenantId(tenantA).nombre("Ana").tipoPago(TipoPago.MENSUAL)
                    .monto(new BigDecimal("1500.00")).build());
            em.clear();
            assertThat(em.find(Collaborador.class, c.getId()).getActivo()).isTrue();
        }
    }

    @Nested
    @DisplayName("GastoFijoRecurrente")
    class GastoFijoTests {

        @Test
        @DisplayName("returns only recurring expenses for that tenant")
        void returns_tenantExpenses() {
            em.persistFlushFind(GastoFijoRecurrente.builder()
                    .tenantId(tenantA).categoria(CategoriaGasto.ALQUILER)
                    .monto(new BigDecimal("500.00")).diaEjecucion(1).build());
            em.persistFlushFind(GastoFijoRecurrente.builder()
                    .tenantId(tenantB).categoria(CategoriaGasto.INTERNET)
                    .monto(new BigDecimal("50.00")).diaEjecucion(15).build());
            em.clear();

            var result = gastoFijoRepository.findByTenantIdOrderByCategoriaAsc(tenantA);
            assertThat(result).hasSize(1)
                    .extracting(GastoFijoRecurrente::getCategoria)
                    .containsExactly(CategoriaGasto.ALQUILER);
        }

        @Test
        @DisplayName("soft delete excludes deleted from queries")
        void softDelete_excludesDeleted() {
            var g = em.persistFlushFind(GastoFijoRecurrente.builder()
                    .tenantId(tenantA).categoria(CategoriaGasto.AGUA)
                    .monto(new BigDecimal("45.00")).diaEjecucion(10).build());
            em.clear();

            gastoFijoRepository.deleteById(g.getId());
            em.flush();
            em.clear();

            assertThat(gastoFijoRepository.findById(g.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("ConfigLaboral")
    class ConfigLaboralTests {

        @Test
        @DisplayName("one row per tenant, upsertable by natural key")
        void naturalKey_roundTrip() {
            var config = ConfigLaboral.builder().tenantId(tenantA).diasLaborales(22).build();
            configLaboralRepository.save(config);
            em.flush();
            em.clear();

            var loaded = configLaboralRepository.findById(tenantA);
            assertThat(loaded).isPresent();
            assertThat(loaded.get().getDiasLaborales()).isEqualTo(22);
        }

        @Test
        @DisplayName("unknown tenant returns empty")
        void unknownTenant() {
            assertThat(configLaboralRepository.findById(UUID.randomUUID())).isEmpty();
        }
    }
}

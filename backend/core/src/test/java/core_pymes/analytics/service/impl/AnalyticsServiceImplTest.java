package core_pymes.analytics.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import core_pymes.analytics.domain.AnalisisGasto;
import core_pymes.analytics.repository.AnalisisGastoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock JdbcTemplate jdbc;
    @Mock AnalisisGastoRepository repository;
    @Mock ObjectMapper objectMapper;
    AnalyticsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new AnalyticsServiceImpl(jdbc, repository, objectMapper));
    }

    @Test
    void ejecutarCompleto_whenNoExistingAnalysis_createsAndSaves() throws Exception {
        var tenantId = UUID.randomUUID();
        when(repository.findByTenantIdAndPeriod(tenantId, "2026-06")).thenReturn(Optional.empty());
        doReturn(List.of()).when(service).analisisABC(any(), any(), any());
        doReturn(List.of()).when(service).analisisTendencia(any(), any(), any());
        doReturn(List.of()).when(service).analisisMargen(any(), any(), any());
        doReturn(List.of()).when(service).analisisCostoOperativo(any(), any(), any());
        doReturn(List.of()).when(service).analisisProyeccion(any(), any(), any());
        doReturn(List.of()).when(service).analisisAlertas(any(), any(), any());
        doReturn(List.of()).when(service).analisisComparativaProveedores(any(), any(), any());
        doReturn(List.of()).when(service).analisisRecomendacionProveedor(any(), any(), any());
        doReturn(List.of()).when(service).analisisProyeccionPrecios(any(), any(), any());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        var result = service.ejecutarCompleto(tenantId, "2026-06");

        assertThat(result.getTenantId()).isEqualTo(tenantId);
        assertThat(result.getPeriod()).isEqualTo("2026-06");
        assertThat(result.getAbc()).isEqualTo("{}");
        verify(repository).save(result);
    }

    @Test
    void ejecutarCompleto_whenExistingAnalysis_updatesFields() throws Exception {
        var tenantId = UUID.randomUUID();
        var existing = AnalisisGasto.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .period("2026-06")
                .abc("old")
                .build();
        when(repository.findByTenantIdAndPeriod(tenantId, "2026-06")).thenReturn(Optional.of(existing));
        doReturn(List.of()).when(service).analisisABC(any(), any(), any());
        doReturn(List.of()).when(service).analisisTendencia(any(), any(), any());
        doReturn(List.of()).when(service).analisisMargen(any(), any(), any());
        doReturn(List.of()).when(service).analisisCostoOperativo(any(), any(), any());
        doReturn(List.of()).when(service).analisisProyeccion(any(), any(), any());
        doReturn(List.of()).when(service).analisisAlertas(any(), any(), any());
        doReturn(List.of()).when(service).analisisComparativaProveedores(any(), any(), any());
        doReturn(List.of()).when(service).analisisRecomendacionProveedor(any(), any(), any());
        doReturn(List.of()).when(service).analisisProyeccionPrecios(any(), any(), any());
        when(objectMapper.writeValueAsString(any())).thenReturn("updated");

        service.ejecutarCompleto(tenantId, "2026-06");

        assertThat(existing.getAbc()).isEqualTo("updated");
        verify(repository).save(existing);
    }

    @Test
    void ejecutarCompleto_invokesAllEngines() {
        var tenantId = UUID.randomUUID();
        when(repository.findByTenantIdAndPeriod(tenantId, "2026-06")).thenReturn(Optional.empty());
        doReturn(List.of(Map.of("from", "abc"))).when(service).analisisABC(any(), any(), any());
        doReturn(List.of(Map.of("from", "trend"))).when(service).analisisTendencia(any(), any(), any());
        doReturn(List.of(Map.of("from", "margin"))).when(service).analisisMargen(any(), any(), any());
        doReturn(List.of(Map.of("from", "opex"))).when(service).analisisCostoOperativo(any(), any(), any());
        doReturn(List.of(Map.of("from", "proj"))).when(service).analisisProyeccion(any(), any(), any());
        doReturn(List.of(Map.of("from", "alert"))).when(service).analisisAlertas(any(), any(), any());
        doReturn(List.of(Map.of("from", "cmp"))).when(service).analisisComparativaProveedores(any(), any(), any());
        doReturn(List.of(Map.of("from", "rec"))).when(service).analisisRecomendacionProveedor(any(), any(), any());
        doReturn(List.of(Map.of("from", "pred"))).when(service).analisisProyeccionPrecios(any(), any(), any());

        service.ejecutarCompleto(tenantId, "2026-06");

        verify(service).analisisABC(any(), any(), any());
        verify(service).analisisTendencia(any(), any(), any());
        verify(service).analisisMargen(any(), any(), any());
        verify(service).analisisCostoOperativo(any(), any(), any());
        verify(service).analisisProyeccion(any(), any(), any());
        verify(service).analisisAlertas(any(), any(), any());
        verify(service).analisisComparativaProveedores(any(), any(), any());
        verify(service).analisisRecomendacionProveedor(any(), any(), any());
        verify(service).analisisProyeccionPrecios(any(), any(), any());
    }

    @Test
    void consultar_withExisting_returnsAnalysis() {
        var tenantId = UUID.randomUUID();
        var analysis = AnalisisGasto.builder().build();
        when(repository.findByTenantIdAndPeriod(tenantId, "2026-06")).thenReturn(Optional.of(analysis));

        var result = service.consultar(tenantId, "2026-06");

        assertThat(result).isPresent();
    }

    @Test
    void consultar_withoutExisting_returnsEmpty() {
        when(repository.findByTenantIdAndPeriod(any(), any())).thenReturn(Optional.empty());

        var result = service.consultar(UUID.randomUUID(), "2026-06");

        assertThat(result).isEmpty();
    }
}

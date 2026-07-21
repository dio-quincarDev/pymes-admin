package core_pymes.unit;

import core_pymes.setup.domain.TenantSetup;
import core_pymes.setup.dto.SetupResponse;
import core_pymes.setup.mapper.SetupMapper;
import core_pymes.setup.repository.TenantSetupRepository;
import core_pymes.common.exception.custom.InvalidInputException;
import core_pymes.setup.service.impl.SetupServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SetupServiceImplTest {

    @Mock
    private TenantSetupRepository repository;

    @Mock
    private JdbcTemplate jdbc;

    @Mock
    private SetupMapper mapper;

    @InjectMocks
    private SetupServiceImpl service;

    @Test
    void getOrInitialize_WhenNotExists_CreatesNew() {
        var tenantId = UUID.randomUUID();
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any(), any(), any(), any(), any())).thenReturn(
                new SetupResponse(null, tenantId, null, false, List.of(), List.of(), List.of(), List.of()));

        var result = service.getOrInitialize(tenantId);

        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.onboardingCompleted()).isFalse();
        assertThat(result.categories()).isEmpty();
        assertThat(result.units()).isEmpty();
        assertThat(result.locations()).isEmpty();
        verify(repository).save(any());
    }

    @Test
    void getOrInitialize_WhenExists_ReturnsExisting() {
        var tenantId = UUID.randomUUID();
        var existing = new TenantSetup(tenantId);
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.of(existing));
        when(mapper.toResponse(any(), any(), any(), any(), any())).thenReturn(
                new SetupResponse(null, tenantId, null, false, List.of(), List.of(), List.of(), List.of()));

        var result = service.getOrInitialize(tenantId);

        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.onboardingCompleted()).isFalse();
        assertThat(result.categories()).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void completeOnboarding_WhenNotExists_CreatesAndCompletes() {
        var tenantId = UUID.randomUUID();
        var industry = "restaurante";
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(industry))).thenReturn(1);
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(jdbc.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), eq(industry)))
                .thenReturn(List.of());
        when(mapper.toResponse(any(), any(), any(), any(), any())).thenReturn(
                new SetupResponse(null, tenantId, industry, true, List.of(), List.of(), List.of(), List.of()));

        var result = service.completeOnboarding(tenantId, industry);

        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.industry()).isEqualTo(industry);
        assertThat(result.onboardingCompleted()).isTrue();
        verify(repository, times(2)).save(any());
    }

    @Test
    void completeOnboarding_WhenExists_CompletesExisting() {
        var tenantId = UUID.randomUUID();
        var industry = "bares";
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(industry))).thenReturn(1);
        var existing = new TenantSetup(tenantId);
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(jdbc.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), eq(industry)))
                .thenReturn(List.of());
        when(mapper.toResponse(any(), any(), any(), any(), any())).thenReturn(
                new SetupResponse(null, tenantId, industry, true, List.of(), List.of(), List.of(), List.of()));

        var result = service.completeOnboarding(tenantId, industry);

        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.industry()).isEqualTo(industry);
        assertThat(result.onboardingCompleted()).isTrue();
        verify(repository, times(1)).save(any());
    }

    @Test
    void completeOnboarding_InvalidIndustry_Throws() {
        var tenantId = UUID.randomUUID();
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("inventada"))).thenReturn(0);

        assertThatThrownBy(() -> service.completeOnboarding(tenantId, "inventada"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Industry not found");

        verify(repository, never()).findByTenantId(any());
        verify(repository, never()).save(any());
    }

    @Test
    void previewIndustry_ValidIndustry_ReturnsSetupResponse() {
        var industry = "restaurante";
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(industry))).thenReturn(1);
        when(jdbc.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), eq(industry)))
                .thenReturn(List.of());

        var result = service.previewIndustry(industry);

        assertThat(result.industry()).isEqualTo(industry);
        assertThat(result.tenantId()).isNull();
        assertThat(result.onboardingCompleted()).isFalse();
        verify(repository, never()).findByTenantId(any());
        verify(repository, never()).save(any());
    }

    @Test
    void previewIndustry_InvalidIndustry_Throws() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("inventada"))).thenReturn(0);

        assertThatThrownBy(() -> service.previewIndustry("inventada"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Industry not found");

        verify(repository, never()).findByTenantId(any());
    }

    @Test
    void buildCategoryTree_FlatList_ReturnsRootsOnly() {
        var industry = "restaurante";
        var flat = List.of(
            new SetupResponse.ItemDTO("cat1", "Alimentos", null, null),
            new SetupResponse.ItemDTO("cat2", "Bebidas", null, null)
        );
        when(jdbc.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), eq(industry)))
                .thenReturn(flat);

        var result = service.buildCategoryTree(industry);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).code()).isEqualTo("cat1");
        assertThat(result.get(0).children()).isEmpty();
        assertThat(result.get(1).code()).isEqualTo("cat2");
        assertThat(result.get(1).children()).isEmpty();
    }

    @Test
    void buildCategoryTree_HierarchicalList_ReturnsTree() {
        var industry = "restaurante";
        var flat = List.of(
            new SetupResponse.ItemDTO("cat1", "Alimentos", null, null),
            new SetupResponse.ItemDTO("sub1", "Carnes", "cat1", null),
            new SetupResponse.ItemDTO("sub2", "Verduras", "cat1", null),
            new SetupResponse.ItemDTO("cat2", "Bebidas", null, null),
            new SetupResponse.ItemDTO("sub3", "Gaseosas", "cat2", null)
        );
        when(jdbc.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), eq(industry)))
                .thenReturn(flat);

        var result = service.buildCategoryTree(industry);

        assertThat(result).hasSize(2);
        // cat1 with 2 children
        assertThat(result.get(0).code()).isEqualTo("cat1");
        assertThat(result.get(0).children()).hasSize(2);
        assertThat(result.get(0).children().get(0).code()).isEqualTo("sub1");
        assertThat(result.get(0).children().get(1).code()).isEqualTo("sub2");
        // cat2 with 1 child
        assertThat(result.get(1).code()).isEqualTo("cat2");
        assertThat(result.get(1).children()).hasSize(1);
        assertThat(result.get(1).children().get(0).code()).isEqualTo("sub3");
    }

    @Test
    void buildCategoryTree_ThreeLevelDepth_ReturnsNestedTree() {
        var industry = "restaurante";
        var flat = List.of(
            new SetupResponse.ItemDTO("cat1", "Alimentos", null, null),
            new SetupResponse.ItemDTO("sub1", "Carnes", "cat1", null),
            new SetupResponse.ItemDTO("item1", "Res", "sub1", null)
        );
        when(jdbc.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), eq(industry)))
                .thenReturn(flat);

        var result = service.buildCategoryTree(industry);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).children()).hasSize(1);
        assertThat(result.get(0).children().get(0).children()).hasSize(1);
        assertThat(result.get(0).children().get(0).children().get(0).code()).isEqualTo("item1");
    }

    @Test
    void getCategories_withIndustry_returnsCategoryTree() {
        var tenantId = UUID.randomUUID();
        var config = new TenantSetup(tenantId);
        config.completeOnboarding("restaurante");
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.of(config));
        var flat = List.of(
                new SetupResponse.ItemDTO("cat1", "Bebidas", null, null),
                new SetupResponse.ItemDTO("sub1", "Gaseosas", "cat1", null)
        );
        when(jdbc.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), eq("restaurante")))
                .thenReturn(flat);

        var result = service.getCategories(tenantId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("cat1");
        assertThat(result.get(0).children()).hasSize(1);
        assertThat(result.get(0).children().get(0).code()).isEqualTo("sub1");
    }

    @Test
    void getCategories_withoutIndustry_returnsEmptyList() {
        var tenantId = UUID.randomUUID();
        var config = new TenantSetup(tenantId); // no industry set
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.of(config));

        var result = service.getCategories(tenantId);

        assertThat(result).isEmpty();
        verify(jdbc, never()).query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), anyString());
    }

    @Test
    void getCategories_unknownTenant_throws() {
        var tenantId = UUID.randomUUID();
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCategories(tenantId))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Tenant not found");
    }
}

package core_pymes.unit;

import core_pymes.inversion.domain.Patrimonio;
import core_pymes.inversion.dto.PatrimonioRequest;
import core_pymes.inversion.repository.PatrimonioRepository;
import core_pymes.inversion.service.impl.PatrimonioServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatrimonioServiceImplTest {

    @Mock PatrimonioRepository repository;
    @InjectMocks PatrimonioServiceImpl service;

    private Patrimonio patrimonio(UUID tenantId, BigDecimal capital) {
        return Patrimonio.builder()
                .tenantId(tenantId)
                .initialCapital(capital)
                .startDate(LocalDate.of(2026, 1, 1))
                .build();
    }

    @Test
    void getOrCreate_returnsExistingPatrimony() {
        var tenantId = UUID.randomUUID();
        var patrimonio = patrimonio(tenantId, new BigDecimal("1000"));
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.of(patrimonio));

        var result = service.getOrCreate(tenantId);

        assertThat(result.capitalInicial()).isEqualByComparingTo("1000");
        verify(repository, never()).save(any());
    }

    @Test
    void getOrCreate_whenMissing_savesWithZeroCapital() {
        var tenantId = UUID.randomUUID();
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.getOrCreate(tenantId);

        assertThat(result.capitalInicial()).isEqualByComparingTo("0");
        verify(repository).save(any());
    }

    @Test
    void update_existingPatrimony_setsCapitalAndDate() {
        var tenantId = UUID.randomUUID();
        var patrimonio = patrimonio(tenantId, new BigDecimal("1000"));
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.of(patrimonio));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.update(tenantId,
                new PatrimonioRequest(tenantId, new BigDecimal("2500"), LocalDate.of(2026, 6, 1)));

        assertThat(result.capitalInicial()).isEqualByComparingTo("2500");
        assertThat(result.fechaInicio()).isEqualTo(LocalDate.of(2026, 6, 1));
    }

    @Test
    void update_whenMissing_createsPatrimony() {
        var tenantId = UUID.randomUUID();
        when(repository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.update(tenantId,
                new PatrimonioRequest(tenantId, new BigDecimal("500"), null));

        assertThat(result.capitalInicial()).isEqualByComparingTo("500");
        assertThat(result.fechaInicio()).isEqualTo(LocalDate.now());
    }
}
package core_pymes.inversion.service.impl;

import core_pymes.inversion.domain.Patrimonio;
import core_pymes.inversion.dto.PatrimonioRequest;
import core_pymes.inversion.dto.PatrimonioResponse;
import core_pymes.inversion.repository.PatrimonioRepository;
import core_pymes.inversion.service.PatrimonioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatrimonioServiceImpl implements PatrimonioService {

    private final PatrimonioRepository repository;

    @Override
    @Transactional
    @Cacheable(cacheNames = "patrimonio", key = "#tenantId")
    public PatrimonioResponse getOrCreate(UUID tenantId) {
        var patrimonio = repository.findByTenantId(tenantId)
                .orElseGet(() -> repository.save(Patrimonio.builder()
                        .tenantId(tenantId)
                        .initialCapital(BigDecimal.ZERO)
                        .startDate(LocalDate.now())
                        .build()));
        return toResponse(patrimonio);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "patrimonio", allEntries = true)
    public PatrimonioResponse update(UUID tenantId, PatrimonioRequest request) {
        var patrimonio = repository.findByTenantId(tenantId)
                .orElse(Patrimonio.builder()
                        .tenantId(tenantId)
                        .build());
        patrimonio.setInitialCapital(request.capitalInicial());
        patrimonio.setStartDate(request.fechaInicio() != null ? request.fechaInicio() : LocalDate.now());
        patrimonio = repository.save(patrimonio);
        return toResponse(patrimonio);
    }

    private PatrimonioResponse toResponse(Patrimonio p) {
        return new PatrimonioResponse(
                p.getTenantId(), p.getInitialCapital(),
                p.getStartDate(), p.getNotes(), p.getCreatedAt());
    }
}

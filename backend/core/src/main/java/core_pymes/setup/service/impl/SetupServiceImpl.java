package core_pymes.setup.service.impl;

import core_pymes.setup.domain.TenantSetup;
import core_pymes.setup.dto.SetupResponse;
import core_pymes.setup.mapper.SetupMapper;
import core_pymes.setup.repository.TenantSetupRepository;
import core_pymes.setup.service.SetupService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class SetupServiceImpl implements SetupService {

    private final TenantSetupRepository repository;
    private final JdbcTemplate jdbc;
    private final SetupMapper mapper;

    public SetupServiceImpl(TenantSetupRepository repository, JdbcTemplate jdbc, SetupMapper mapper) {
        this.repository = repository;
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Transactional
    @Override
    public SetupResponse getOrInitialize(UUID tenantId) {
        var config = repository.findByTenantId(tenantId)
                .orElseGet(() -> repository.save(new TenantSetup(tenantId)));
        return buildResponse(config);
    }

    @Transactional
    @Override
    public SetupResponse completeOnboarding(UUID tenantId, String industry) {
        var count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM industries WHERE code = ?", Integer.class, industry);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("Industry not found: " + industry);
        }
        var config = repository.findByTenantId(tenantId)
                .orElseGet(() -> repository.save(new TenantSetup(tenantId)));
        config.completeOnboarding(industry);
        repository.save(config);
        return buildResponse(config);
    }

    @Override
    public SetupResponse previewIndustry(String industry) {
        validateIndustry(industry);
        var data = loadIndustryData(industry);
        return new SetupResponse(null, null, industry, false, data.categories(), data.units(), data.locations());
    }

    private SetupResponse buildResponse(TenantSetup config) {
        var industry = config.getIndustry();
        if (industry == null) {
            return mapper.toResponse(config, List.of(), List.of(), List.of());
        }
        var data = loadIndustryData(industry);
        return mapper.toResponse(config, data.categories(), data.units(), data.locations());
    }

    private record IndustryData(List<SetupResponse.ItemDTO> categories,
                                List<SetupResponse.ItemDTO> units,
                                List<SetupResponse.ItemDTO> locations) {}

    private IndustryData loadIndustryData(String industry) {
        var categories = buildCategoryTree(industry);
        var units = jdbc.query(
                "SELECT id AS code, name FROM template_units WHERE industry_code = ? ORDER BY sort_order",
                (rs, row) -> SetupResponse.ItemDTO.flat(rs.getString("code"), rs.getString("name")),
                industry);
        var locations = jdbc.query(
                "SELECT id AS code, name FROM template_locations WHERE industry_code = ? ORDER BY sort_order",
                (rs, row) -> SetupResponse.ItemDTO.flat(rs.getString("code"), rs.getString("name")),
                industry);
        return new IndustryData(categories, units, locations);
    }

    private void validateIndustry(String industry) {
        var count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM industries WHERE code = ?", Integer.class, industry);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("Industry not found: " + industry);
        }
    }

    // ponytail: flat list → tree via parent_id, O(n) with map. Good enough for ~60 categories per industry.
    public List<SetupResponse.ItemDTO> buildCategoryTree(String industry) {
        var flat = jdbc.query(
            "SELECT id AS code, name, parent_id FROM template_categories WHERE industry_code = ? ORDER BY sort_order",
            (rs, row) -> new SetupResponse.ItemDTO(
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("parentId"),
                null),
            industry);

        var map = new LinkedHashMap<String, SetupResponse.ItemDTO>();
        var roots = new ArrayList<SetupResponse.ItemDTO>();

        // first pass: index by code, with empty children
        for (var item : flat) {
            map.put(item.code(), new SetupResponse.ItemDTO(item.code(), item.name(), item.parentId(), new ArrayList<>()));
        }

        // second pass: wire children
        for (var item : map.values()) {
            var parentId = item.parentId();
            if (parentId != null && map.containsKey(parentId)) {
                var children = (ArrayList<SetupResponse.ItemDTO>) map.get(parentId).children();
                children.add(item);
            } else {
                roots.add(item);
            }
        }

        return roots;
    }
}

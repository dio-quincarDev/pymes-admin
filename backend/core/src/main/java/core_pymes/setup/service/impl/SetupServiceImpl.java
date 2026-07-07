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

        // ponytail: direct JDBC batch copy from template → tenant, SKU auto P-0001
        var templateProducts = jdbc.query(
            "SELECT tp.id, tp.name, tp.base_unit, tp.min_quantity, tp.max_quantity, " +
            "COALESCE(tp.category_id::text, tc.id::text) AS category_code " +
            "FROM template_products tp " +
            "LEFT JOIN template_categories tc ON tp.category_id = tc.id " +
            "WHERE tp.industry_code = ? ORDER BY tp.sort_order",
            (rs, row) -> new TemplateProductRow(
                rs.getObject("id", java.util.UUID.class),
                rs.getString("name"),
                rs.getString("base_unit"),
                rs.getBigDecimal("min_quantity"),
                rs.getBigDecimal("max_quantity"),
                rs.getString("category_code")),
            industry);

        var templatePres = jdbc.query(
            "SELECT template_product_id, name, conversion FROM template_product_presentations WHERE template_product_id IN " +
            "(SELECT id FROM template_products WHERE industry_code = ?) ORDER BY sort_order",
            (rs, row) -> new TemplatePresentationRow(
                rs.getObject("template_product_id", java.util.UUID.class),
                rs.getString("name"),
                rs.getInt("conversion")),
            industry);

        if (!templateProducts.isEmpty()) {
            var prodBatch = new ArrayList<Object[]>();
            var presBatch = new ArrayList<Object[]>();
            int seq = 0;
            for (var tp : templateProducts) {
                seq++;
                var newProdId = UUID.randomUUID();
                var sku = String.format("P-%04d", seq);
                prodBatch.add(new Object[]{newProdId, tenantId, tp.name, sku, tp.categoryCode, tp.baseUnit, tp.minQuantity, tp.maxQuantity});

                for (var pp : templatePres) {
                    if (pp.templateProductId.equals(tp.id)) {
                        presBatch.add(new Object[]{UUID.randomUUID(), newProdId, pp.name, pp.conversion});
                    }
                }
            }

            jdbc.batchUpdate(
                "INSERT INTO core.products (id, tenant_id, name, sku, category, base_unit, min_quantity, max_quantity, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, true, NOW(), NOW())",
                prodBatch);
            jdbc.batchUpdate(
                "INSERT INTO core.product_presentations (id, product_id, name, conversion, is_active, created_at) VALUES (?, ?, ?, ?, true, NOW())",
                presBatch);
        }

        return buildResponse(config);
    }

    @Override
    public SetupResponse previewIndustry(String industry) {
        validateIndustry(industry);
        var data = loadIndustryData(industry);
        return SetupResponse.preview(industry, data.categories(), data.units(), data.locations(), data.products());
    }

    private SetupResponse buildResponse(TenantSetup config) {
        var industry = config.getIndustry();
        if (industry == null) {
            return mapper.toResponse(config, List.of(), List.of(), List.of(), List.of());
        }
        var data = loadIndustryData(industry);
        return mapper.toResponse(config, data.categories(), data.units(), data.locations(), data.products());
    }

    private record IndustryData(List<SetupResponse.ItemDTO> categories,
                                List<SetupResponse.ItemDTO> units,
                                List<SetupResponse.ItemDTO> locations,
                                List<SetupResponse.ProductTemplateDTO> products) {}

    private record TemplateProductRow(java.util.UUID id, String name, String baseUnit, java.math.BigDecimal minQuantity, java.math.BigDecimal maxQuantity, String categoryCode) {}
    private record TemplatePresentationRow(java.util.UUID templateProductId, String name, int conversion) {}

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
        var products = jdbc.query(
            "SELECT tp.id, tp.name, tp.base_unit, tc.name AS category_name " +
            "FROM template_products tp " +
            "LEFT JOIN template_categories tc ON tp.category_id = tc.id " +
            "WHERE tp.industry_code = ? ORDER BY tp.sort_order",
            (rs, row) -> new SetupResponse.ProductTemplateDTO(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("base_unit"),
                rs.getString("category_name")),
            industry);
        return new IndustryData(categories, units, locations, products);
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
                rs.getString("parent_id"),
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

package core_pymes.setup.dto;

import java.util.List;
import java.util.UUID;

public record SetupResponse(
    UUID id,
    UUID tenantId,
    String industry,
    boolean onboardingCompleted,
    List<ItemDTO> categories,
    List<ItemDTO> units,
    List<ProductTemplateDTO> products
) {
    public static SetupResponse preview(String industry, List<ItemDTO> categories, List<ItemDTO> units, List<ProductTemplateDTO> products) {
        return new SetupResponse(null, null, industry, false, categories, units, products);
    }

    public record ItemDTO(String code, String name, String parentId, List<ItemDTO> children) {
        public static ItemDTO flat(String code, String name) {
            return new ItemDTO(code, name, null, List.of());
        }
    }

    public record ProductTemplateDTO(String id, String name, String baseUnit, String categoryName) {}
}

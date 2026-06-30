package core_pymes.setup.mapper;

import core_pymes.setup.domain.TenantSetup;
import core_pymes.setup.dto.SetupResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SetupMapper {

    public SetupResponse toResponse(TenantSetup entity, List<SetupResponse.ItemDTO> categories,
                                     List<SetupResponse.ItemDTO> units,
                                     List<SetupResponse.ItemDTO> locations,
                                     List<SetupResponse.ProductTemplateDTO> products) {
        return new SetupResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getIndustry(),
                entity.isOnboardingCompleted(),
                categories,
                units,
                locations,
                products
        );
    }
}

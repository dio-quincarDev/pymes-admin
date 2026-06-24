package core_pymes.setup.controller.impl;

import core_pymes.setup.controller.SetupApi;
import core_pymes.setup.dto.SetupResponse;
import core_pymes.setup.service.SetupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SetupController implements SetupApi {

    private final SetupService setupService;

    @Override
    public ResponseEntity<SetupResponse> getOrInitialize(UUID tenantId) {
        log.debug("Getting or initializing setup for tenant {}", tenantId);
        return ResponseEntity.ok(setupService.getOrInitialize(tenantId));
    }

    @Override
    public ResponseEntity<SetupResponse> completeOnboarding(UUID tenantId, Map<String, String> body) {
        var industry = body.get("industry");
        log.debug("Completing onboarding for tenant {} with industry {}", tenantId, industry);
        return ResponseEntity.ok(setupService.completeOnboarding(tenantId, industry));
    }
}

package core_pymes.setup.controller;

import core_pymes.common.constant.CorePath;
import core_pymes.setup.dto.SetupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Setup", description = "Tenant onboarding and configuration")
@RequestMapping(CorePath.V1_ROUTE + CorePath.CORE_ROUTE + CorePath.CORE_SETUP)
public interface SetupApi {

    @Operation(summary = "Get or initialize tenant setup")
    @GetMapping("/{tenantId}")
    ResponseEntity<SetupResponse> getOrInitialize(@PathVariable UUID tenantId);

    @Operation(summary = "Complete tenant onboarding with industry")
    @PostMapping("/{tenantId}/onboarding")
    ResponseEntity<SetupResponse> completeOnboarding(
            @PathVariable UUID tenantId,
            @RequestBody Map<String, String> body);

    @Operation(summary = "Preview categories/units/locations for an industry (read-only)")
    @GetMapping("/preview/{industry}")
    ResponseEntity<SetupResponse> preview(@PathVariable String industry);
}

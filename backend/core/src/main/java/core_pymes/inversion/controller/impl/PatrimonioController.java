package core_pymes.inversion.controller.impl;

import core_pymes.inversion.controller.PatrimonioApi;
import core_pymes.inversion.dto.PatrimonioRequest;
import core_pymes.inversion.dto.PatrimonioResponse;
import core_pymes.inversion.service.PatrimonioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PatrimonioController implements PatrimonioApi {

    private final PatrimonioService patrimonioService;

    @Override
    public ResponseEntity<PatrimonioResponse> getOrCreate(UUID tenantId) {
        return ResponseEntity.ok(patrimonioService.getOrCreate(tenantId));
    }

    @Override
    public ResponseEntity<PatrimonioResponse> update(UUID tenantId, PatrimonioRequest request) {
        return ResponseEntity.ok(patrimonioService.update(tenantId, request));
    }
}

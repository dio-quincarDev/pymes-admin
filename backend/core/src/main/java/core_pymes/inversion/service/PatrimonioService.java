package core_pymes.inversion.service;

import core_pymes.inversion.dto.PatrimonioRequest;
import core_pymes.inversion.dto.PatrimonioResponse;

import java.util.UUID;

public interface PatrimonioService {

    PatrimonioResponse getOrCreate(UUID tenantId);

    PatrimonioResponse update(UUID tenantId, PatrimonioRequest request);
}

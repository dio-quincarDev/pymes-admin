package auth.pymes.service;

import auth.pymes.common.models.dto.request.OAuth2IntentRequest;
import auth.pymes.common.models.dto.response.OAuth2IntentResponse;

import java.util.Optional;

public interface OAuth2IntentService {
    OAuth2IntentResponse createIntent(OAuth2IntentRequest request);
    Optional<OAuth2IntentRequest> getIntent(String intentId);
    void deleteIntent(String intentId);
}

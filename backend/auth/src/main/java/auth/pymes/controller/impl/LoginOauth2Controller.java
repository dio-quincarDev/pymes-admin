package auth.pymes.controller.impl;

import auth.pymes.common.constants.ApiPathConstants;
import auth.pymes.common.models.dto.request.OAuth2IntentRequest;
import auth.pymes.common.models.dto.response.ApiResponse;
import auth.pymes.common.models.dto.response.OAuth2IntentResponse;
import auth.pymes.controller.OAuth2Api;
import auth.pymes.service.OAuth2IntentService;
import auth.pymes.utils.exception.CodigoError;
import auth.pymes.utils.exception.custom.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConstants.V1_ROUTE + ApiPathConstants.AUTH_ROUTE + ApiPathConstants.AUTH_OAUTH2)
public class LoginOauth2Controller implements OAuth2Api {

    private final OAuth2IntentService oauth2IntentService;

    @Override
    @org.springframework.web.bind.annotation.PostMapping(ApiPathConstants.OAUTH2_INTENT)
    public ResponseEntity<ApiResponse<OAuth2IntentResponse>> createOAuth2Intent(
            @Valid @RequestBody OAuth2IntentRequest request) {
        OAuth2IntentResponse response = oauth2IntentService.createIntent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @Override
    @org.springframework.web.bind.annotation.GetMapping(ApiPathConstants.OAUTH2_INTENT_GET)
    public ResponseEntity<ApiResponse<OAuth2IntentRequest>> getOAuth2Intent(
            @PathVariable String intentId) {
        return oauth2IntentService.getIntent(intentId)
                .<ResponseEntity<ApiResponse<OAuth2IntentRequest>>>map(intent ->
                        ResponseEntity.ok(ApiResponse.ok(intent)))
                .orElseThrow(() -> new ResourceNotFoundException(CodigoError.RESOURCE_NOT_FOUND, "OAuth2 Intent"));
    }
}
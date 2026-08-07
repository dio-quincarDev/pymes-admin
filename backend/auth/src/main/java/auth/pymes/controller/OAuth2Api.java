package auth.pymes.controller;

import auth.pymes.common.constants.ApiPathConstants;
import auth.pymes.common.models.dto.request.OAuth2IntentRequest;
import auth.pymes.common.models.dto.response.ApiResponse;
import auth.pymes.common.models.dto.response.OAuth2IntentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "OAuth2", description = "Endpoints OAuth2")
@RequestMapping(ApiPathConstants.V1_ROUTE + ApiPathConstants.AUTH_ROUTE + ApiPathConstants.AUTH_OAUTH2)
public interface OAuth2Api {

    @Operation(summary = "Create OAuth2 Intent", description = "Crea un intento de registro OAuth2 con datos de empresa para ser procesado tras el login")
    @PostMapping(ApiPathConstants.OAUTH2_INTENT)
    ResponseEntity<ApiResponse<OAuth2IntentResponse>> createOAuth2Intent(
            @Valid @RequestBody OAuth2IntentRequest request);

    @Operation(summary = "Get OAuth2 Intent", description = "Obtiene los datos de un intento OAuth2 existente")
    @GetMapping(ApiPathConstants.OAUTH2_INTENT_GET)
    ResponseEntity<ApiResponse<OAuth2IntentRequest>> getOAuth2Intent(@PathVariable String intentId);
}
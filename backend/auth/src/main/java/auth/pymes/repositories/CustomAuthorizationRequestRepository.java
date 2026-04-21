package auth.pymes.repositories;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_PREFIX = "oauth2:auth_request:";
    private static final long TTL_MINUTES = 10;
    private static final String STATE_SEPARATOR = ":";
    public static final String INTENT_PARAM = "intentId";

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            String state = request.getParameter("state");
            if (state != null) redisTemplate.delete(REDIS_PREFIX + state);
            return;
        }

        String intentId = request.getParameter(INTENT_PARAM);
        String finalState = authorizationRequest.getState();

        OAuth2AuthorizationRequest requestToStore = authorizationRequest;
        if (intentId != null && !intentId.isBlank()) {
            finalState = authorizationRequest.getState() + STATE_SEPARATOR + intentId;
            requestToStore = OAuth2AuthorizationRequest
                    .from(authorizationRequest)
                    .state(finalState)
                    .build();
            log.debug("State compuesto generado: {} (intentId: {})", finalState, intentId);
        }

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("authorizationUri", requestToStore.getAuthorizationUri());
            data.put("clientId", requestToStore.getClientId());
            data.put("redirectUri", requestToStore.getRedirectUri());
            data.put("state", requestToStore.getState());
            data.put("scopes", requestToStore.getScopes());
            data.put("additionalParameters", requestToStore.getAdditionalParameters());
            data.put("authorizationGrantType", requestToStore.getGrantType().getValue());

            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(REDIS_PREFIX + finalState, json, TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("Authorization request guardado en Redis con state: {}", finalState);
        } catch (Exception e) {
            log.error("Error guardando authorization request en Redis", e);
        }
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String state = request.getParameter("state");
        if (state == null) return null;
        return getFromRedis(state);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        String state = request.getParameter("state");
        if (state == null) return null;

        OAuth2AuthorizationRequest stored = getFromRedis(state);
        if (stored != null) {
            redisTemplate.delete(REDIS_PREFIX + state);
            log.debug("Authorization request eliminado de Redis, state: {}", state);
        }
        return stored;
    }

    @SuppressWarnings("unchecked")
    private OAuth2AuthorizationRequest getFromRedis(String state) {
        try {
            String json = redisTemplate.opsForValue().get(REDIS_PREFIX + state);
            if (json == null) {
                log.warn("Authorization request no encontrado en Redis para state: {}", state);
                return null;
            }

            Map<String, Object> data = objectMapper.readValue(json, Map.class);

            return OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri((String) data.get("authorizationUri"))
                    .clientId((String) data.get("clientId"))
                    .redirectUri((String) data.get("redirectUri"))
                    .state((String) data.get("state"))
                    .scopes(Set.copyOf((java.util.List<String>) data.get("scopes")))
                    .additionalParameters((Map<String, Object>) data.get("additionalParameters"))
                    .build();
        } catch (Exception e) {
            log.error("Error leyendo authorization request de Redis para state: {}", state, e);
            return null;
        }
    }

    public static String extractIntentId(String compositeState) {
        if (compositeState == null) return null;
        int idx = compositeState.lastIndexOf(STATE_SEPARATOR);
        if (idx == -1) return null;
        String intentId = compositeState.substring(idx + 1);
        return intentId.isBlank() ? null : intentId;
    }
}
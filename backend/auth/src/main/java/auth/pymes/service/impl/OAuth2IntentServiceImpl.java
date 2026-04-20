package auth.pymes.service.impl;

import auth.pymes.common.models.dto.request.OAuth2IntentRequest;
import auth.pymes.common.models.dto.response.OAuth2IntentResponse;
import auth.pymes.service.OAuth2IntentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2IntentServiceImpl implements OAuth2IntentService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_PREFIX = "oauth2_intent:";
    private static final long EXPIRATION_MINUTES = 10;

    @Override
    public OAuth2IntentResponse createIntent(OAuth2IntentRequest request) {
        String intentId = UUID.randomUUID().toString();
        String key = REDIS_PREFIX + intentId;

        try {
            String value = objectMapper.writeValueAsString(request);
            redisTemplate.opsForValue().set(key, value, EXPIRATION_MINUTES, TimeUnit.MINUTES);
            log.info("OAuth2 intent created with ID: {}", intentId);
            return new OAuth2IntentResponse(intentId);
        } catch (JsonProcessingException e) {
            log.error("Error serializing OAuth2IntentRequest", e);
            throw new RuntimeException("Error creating OAuth2 intent");
        }
    }

    @Override
    public Optional<OAuth2IntentRequest> getIntent(String intentId) {
        String key = REDIS_PREFIX + intentId;
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(value, OAuth2IntentRequest.class));
        } catch (JsonProcessingException e) {
            log.error("Error deserializing OAuth2IntentRequest", e);
            return Optional.empty();
        }
    }

    @Override
    public void deleteIntent(String intentId) {
        String key = REDIS_PREFIX + intentId;
        redisTemplate.delete(key);
        log.info("OAuth2 intent deleted with ID: {}", intentId);
    }
}

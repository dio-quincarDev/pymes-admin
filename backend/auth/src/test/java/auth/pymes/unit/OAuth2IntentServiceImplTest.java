package auth.pymes.unit;

import auth.pymes.common.models.dto.request.OAuth2IntentRequest;
import auth.pymes.common.models.dto.response.OAuth2IntentResponse;
import auth.pymes.service.impl.OAuth2IntentServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OAuth2IntentServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OAuth2IntentServiceImpl oauth2IntentService;

    @Test
    void createIntent_ShouldSaveToRedisAndReturnId() throws JsonProcessingException {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        OAuth2IntentRequest request = new OAuth2IntentRequest("Test Co", "test-co");
        String json = "{\"companyName\":\"Test Co\",\"companySlug\":\"test-co\"}";
        
        when(objectMapper.writeValueAsString(request)).thenReturn(json);
        
        OAuth2IntentResponse response = oauth2IntentService.createIntent(request);
        
        assertThat(response.intentId()).isNotNull();
        verify(valueOperations).set(contains(response.intentId()), eq(json), anyLong(), eq(TimeUnit.MINUTES));
    }

    @Test
    void getIntent_WhenExists_ShouldReturnRequest() throws JsonProcessingException {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String intentId = "test-uuid";
        OAuth2IntentRequest request = new OAuth2IntentRequest("Test Co", "test-co");
        String json = "{\"companyName\":\"Test Co\",\"companySlug\":\"test-co\"}";
        
        when(valueOperations.get(anyString())).thenReturn(json);
        when(objectMapper.readValue(json, OAuth2IntentRequest.class)).thenReturn(request);
        
        Optional<OAuth2IntentRequest> result = oauth2IntentService.getIntent(intentId);
        
        assertThat(result).isPresent();
        assertThat(result.get().companyName()).isEqualTo("Test Co");
    }

    @Test
    void getIntent_WhenNotExists_ShouldReturnEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        
        Optional<OAuth2IntentRequest> result = oauth2IntentService.getIntent("non-existent");
        
        assertThat(result).isEmpty();
    }

    @Test
    void deleteIntent_ShouldCallRedisDelete() {
        String intentId = "test-uuid";
        
        oauth2IntentService.deleteIntent(intentId);
        
        verify(redisTemplate).delete(contains(intentId));
    }
}

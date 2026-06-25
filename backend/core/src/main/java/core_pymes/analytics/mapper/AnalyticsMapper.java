package core_pymes.analytics.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import core_pymes.analytics.domain.AnalisisGasto;
import core_pymes.analytics.dto.AnalyticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AnalyticsMapper {

    private final ObjectMapper objectMapper;

    public AnalyticsResponse toResponse(AnalisisGasto a) {
        return new AnalyticsResponse(
                a.getId(), a.getTenantId(), a.getPeriod(),
                parseJsonArray(a.getAbc()),
                parseJsonArray(a.getTrend()),
                parseJsonArray(a.getMargin()),
                parseJsonArray(a.getOpexPct()),
                parseJsonArray(a.getProjection()),
                parseJsonArray(a.getAlerts()));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            try {
                var single = objectMapper.readValue(json, Map.class);
                return List.of((Map<String, Object>) single);
            } catch (Exception ex) {
                return List.of();
            }
        }
    }
}

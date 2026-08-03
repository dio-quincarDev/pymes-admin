package core_pymes.analytics.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AnalyticsResponse(
        UUID id,
        UUID tenantId,
        String period,
        List<Map<String, Object>> abc,
        List<Map<String, Object>> trend,
        List<Map<String, Object>> margin,
        List<Map<String, Object>> opexPct,
        List<Map<String, Object>> projection,
        List<Map<String, Object>> alerts,
        List<Map<String, Object>> supplierComparison,
        List<Map<String, Object>> supplierRecommendations,
        List<Map<String, Object>> pricePrediction,
        Map<String, Object> financialHealth
) {}

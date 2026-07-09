package core_pymes.common.service;

import core_pymes.analytics.service.AnalyticsService;
import core_pymes.accounting.service.MetricasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecomputeDebounceService {

    private final StringRedisTemplate stringRedisTemplate;
    private final MetricasService metricasService;
    private final AnalyticsService analyticsService;

    private static final String KEY_PREFIX = "recompute:";

    public void markMetricsDirty(UUID tenantId, String period) {
        var key = KEY_PREFIX + "metrics:" + tenantId + ":" + period;
        stringRedisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofHours(1));
    }

    public void markAnalyticsDirty(UUID tenantId, String period) {
        var key = KEY_PREFIX + "analytics:" + tenantId + ":" + period;
        stringRedisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofHours(1));
    }

    // ponytail: KEYS is fine for PYME-scale (<10k keys), switch to SCAN if scale grows
    @Scheduled(fixedDelay = 30000)
    public void processPending() {
        var keys = stringRedisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) return;

        log.debug("Processing {} pending recompute keys", keys.size());

        for (var key : keys) {
            try {
                var parts = key.split(":");
                if (parts.length < 4) {
                    stringRedisTemplate.delete(key);
                    continue;
                }
                var type = parts[1];
                var tenantId = UUID.fromString(parts[2]);
                var period = parts[3];

                switch (type) {
                    case "metrics" -> {
                        log.debug("Recomputing metrics for tenant {} period {}", tenantId, period);
                        metricasService.recalcular(tenantId, period);
                    }
                    case "analytics" -> {
                        log.debug("Recomputing analytics for tenant {} period {}", tenantId, period);
                        analyticsService.ejecutarCompleto(tenantId, period);
                    }
                    default -> log.warn("Unknown recompute type: {}", type);
                }
                stringRedisTemplate.delete(key);
            } catch (Exception e) {
                log.error("Failed to process recompute key {}: {}", key, e.getMessage());
                // ponytail: keep key in Redis for retry on next cycle
            }
        }
    }
}

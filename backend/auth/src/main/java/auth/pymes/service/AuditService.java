package auth.pymes.service;

import auth.pymes.common.models.entities.AuditLog;
import auth.pymes.repositories.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Registra quién hizo qué, cuándo y desde dónde. Nunca rompe el flujo del usuario.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    // Los fallos se anotan aparte: si la operación se cancela, la nota no debe desaparecer con ella.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String action, UUID tenantId, UUID userId, String resource, UUID resourceId,
                              Map<String, Object> details, HttpServletRequest request) {
        record(action, tenantId, userId, resource, resourceId, details, request);
    }

    public void record(String action, UUID tenantId, UUID userId, String resource, UUID resourceId,
                       Map<String, Object> details, HttpServletRequest request) {
        try {
            auditLogRepository.save(AuditLog.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .action(action)
                    .resource(resource)
                    .resourceId(resourceId)
                    .details(details)
                    .ipAddress(extractIpAddress(request))
                    .userAgent(request == null ? null : request.getHeader("User-Agent"))
                    .createdAt(ZonedDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("No se pudo registrar auditoría {}: {}", action, e.getMessage());
        }
    }

    private String extractIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}

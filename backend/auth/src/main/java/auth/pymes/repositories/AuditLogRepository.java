package auth.pymes.repositories;

import auth.pymes.common.models.entities.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByTenantId(UUID tenantId);
    List<AuditLog> findByUserId(UUID userId);
}

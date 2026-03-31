package auth.pymes.repositories;

import auth.pymes.common.models.entities.UserTenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserTenantRepository extends JpaRepository<UserTenant, UUID> {
    
    List<UserTenant> findByUserId(UUID userId);
    
    List<UserTenant> findByUserIdAndIsActiveTrue(UUID userId);
    
    Page<UserTenant> findByUserIdAndIsActiveTrue(UUID userId, Pageable pageable);
    
    Optional<UserTenant> findByUserIdAndTenantId(UUID userId, UUID tenantId);
    
    boolean existsByUserIdAndTenantId(UUID userId, UUID tenantId);
}

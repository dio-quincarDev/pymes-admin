package auth.pymes.repositories;

import auth.pymes.common.models.entities.Invitation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
    
    Optional<Invitation> findByToken(String token);
    
    Optional<Invitation> findByTokenAndAcceptedAtIsNull(String token);
    
    Optional<Invitation> findByEmailAndTenantId(String email, UUID tenantId);
    
    Page<Invitation> findByEmailAndAcceptedAtIsNull(String email, Pageable pageable);
    
    boolean existsByTenantIdAndEmailAndAcceptedAtIsNull(UUID tenantId, String email);
}

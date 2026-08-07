package core_pymes.setup.domain;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_setup", schema = "core")
public class TenantSetup {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID tenantId;

    private String industry;

    @Column(nullable = false)
    private boolean onboardingCompleted;

    @Column(nullable = false)
    private ZonedDateTime createdAt;

    private ZonedDateTime updatedAt;

    public TenantSetup() {}

    public TenantSetup(UUID tenantId) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.onboardingCompleted = false;
        this.createdAt = ZonedDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getIndustry() { return industry; }
    public boolean isOnboardingCompleted() { return onboardingCompleted; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
    public ZonedDateTime getUpdatedAt() { return updatedAt; }

    public void completeOnboarding(String industry) {
        this.industry = industry;
        this.onboardingCompleted = true;
        this.updatedAt = ZonedDateTime.now();
    }
}

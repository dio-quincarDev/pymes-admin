package core_pymes.analytics.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "expense_analysis", schema = "core")
public class AnalisisGasto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 7)
    private String period;

    @Column(columnDefinition = "JSONB")
    private String abc;

    @Column(columnDefinition = "JSONB")
    private String trend;

    @Column(columnDefinition = "JSONB")
    private String margin;

    @Column(name = "opex_pct", columnDefinition = "JSONB")
    private String opexPct;

    @Column(columnDefinition = "JSONB")
    private String projection;

    @Column(columnDefinition = "JSONB")
    private String alerts;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;
}

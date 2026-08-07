package core_pymes.costos.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "collaboradores", schema = "core")
@SQLDelete(sql = "UPDATE core.collaboradores SET activo = false WHERE id = ?")
@Where(clause = "activo = true")
public class Collaborador {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "tipo_pago", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TipoPago tipoPago;

    @Column(nullable = false)
    private BigDecimal monto;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;
}

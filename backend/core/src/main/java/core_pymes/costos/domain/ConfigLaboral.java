package core_pymes.costos.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "config_laboral", schema = "core")
public class ConfigLaboral {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "dias_laborales", nullable = false)
    private Integer diasLaborales;
}

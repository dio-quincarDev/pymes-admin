package core_pymes.costos.domain;

import core_pymes.gasto.domain.CategoriaGasto;
import core_pymes.invoice.domain.Proveedor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "gastos_fijos_recurrentes", schema = "core")
@SQLDelete(sql = "UPDATE core.gastos_fijos_recurrentes SET activo = false WHERE id = ?")
@Where(clause = "activo = true")
public class GastoFijoRecurrente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private CategoriaGasto categoria;

    @Column(nullable = false)
    private BigDecimal monto;

    private String descripcion;

    @Column(name = "dia_ejecucion", nullable = false)
    private Integer diaEjecucion;

    @Column(name = "metodo_pago")
    private String metodoPago;

    @Column(name = "provider_id", nullable = true)
    private UUID providerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", insertable = false, updatable = false)
    private Proveedor proveedor;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;
}

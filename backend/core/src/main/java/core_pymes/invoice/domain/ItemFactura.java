package core_pymes.invoice.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "invoice_items", schema = "core")
public class ItemFactura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Factura factura;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "presentacion_id")
    private UUID presentacionId;

    @Column(name = "conversion_factor", nullable = false)
    private Integer conversionFactor;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private BigDecimal discount;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(name = "cantidad_presentacion")
    private BigDecimal cantidadPresentacion;

    @Column(name = "valor_presentacion")
    private BigDecimal valorPresentacion;

    @Column(name = "precio_unitario_input")
    private BigDecimal precioUnitarioInput;

    @Column(name = "descuento_input")
    private BigDecimal descuentoInput;

    @Column(name = "descuento_es_porcentaje")
    private Boolean descuentoEsPorcentaje;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;
}

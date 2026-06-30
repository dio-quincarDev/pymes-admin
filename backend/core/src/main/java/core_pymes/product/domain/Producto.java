package core_pymes.product.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "products", schema = "core")
@SQLDelete(sql = "UPDATE core.products SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    private String sku;

    private String category;

    @Column(name = "base_unit")
    private String baseUnit;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "last_unit_price")
    private BigDecimal lastUnitPrice;

    @Column(name = "total_investment", nullable = false)
    @Builder.Default
    private BigDecimal totalInvestment = BigDecimal.ZERO;

    @Column(name = "last_purchase_date")
    private LocalDate lastPurchaseDate;

    @Column(name = "min_quantity")
    private BigDecimal minQuantity;

    @Column(name = "max_quantity")
    private BigDecimal maxQuantity;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;
}

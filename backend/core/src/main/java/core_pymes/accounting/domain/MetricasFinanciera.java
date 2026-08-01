package core_pymes.accounting.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tenant_financial_metrics", schema = "core",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "period"}))
public class MetricasFinanciera {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 7)
    private String period;

    @Column(name = "total_income", nullable = false)
    private BigDecimal totalIncome;

    @Column(name = "cost_of_goods", nullable = false)
    private BigDecimal costOfGoods;

    @Column(name = "operating_expenses", nullable = false)
    private BigDecimal operatingExpenses;

    @Column(name = "loan_payments", nullable = false)
    private BigDecimal loanPayments;

    @Column(name = "total_expenses", nullable = false)
    private BigDecimal totalExpenses;

    @Column(name = "gross_margin", nullable = false)
    private BigDecimal grossMargin;

    @Column(name = "gross_margin_pct", nullable = false)
    private BigDecimal grossMarginPct;

    @Column(name = "operating_margin", nullable = false)
    private BigDecimal operatingMargin;

    @Column(name = "operating_margin_pct", nullable = false)
    private BigDecimal operatingMarginPct;

    @Column(name = "net_margin", nullable = false)
    private BigDecimal netMargin;

    @Column(name = "net_margin_pct", nullable = false)
    private BigDecimal netMarginPct;

    @Column(name = "costo_operativo_diario")
    private BigDecimal costoOperativoDiario;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;
}

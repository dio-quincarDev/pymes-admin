-- V10: Performance indexes for costos CTE + analytics queries.

-- ============ 1. Costos CTE (MetricasServiceImpl) ============
-- Partial indexes: only active rows, include monto for index-only scan.

CREATE INDEX IF NOT EXISTS idx_gastos_fijos_tenant_active
    ON core.gastos_fijos_recurrentes(tenant_id, monto)
    WHERE activo = true;

CREATE INDEX IF NOT EXISTS idx_collaboradores_tenant_active
    ON core.collaboradores(tenant_id, monto, tipo_pago)
    WHERE activo = true;

-- ============ 2. Invoice number lookup ============
-- FacturaRepository.findMaxInvoiceNumber uses LIKE prefix on invoice_number.

CREATE INDEX IF NOT EXISTS idx_invoices_tenant_number
    ON core.invoices(tenant_id, invoice_number);

-- ============ 3. Analytics covering indexes ============
-- AnalyticsServiceImpl runs 7+ CTEs with 3-4 way joins on invoices + invoice_items.
-- Covering indexes avoid heap lookups for the most frequent access patterns.

CREATE INDEX IF NOT EXISTS idx_invoices_analytics
    ON core.invoices(tenant_id, issue_date)
    INCLUDE (id, provider_id, type, status);

CREATE INDEX IF NOT EXISTS idx_invoice_items_analytics
    ON core.invoice_items(invoice_id)
    INCLUDE (product_id, subtotal, unit_price, conversion_factor);

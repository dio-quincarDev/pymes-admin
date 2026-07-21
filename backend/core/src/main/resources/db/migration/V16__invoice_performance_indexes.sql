-- Add indexes for query performance in reverseProductStats and product stats updates
-- idx_invoice_items_product_id skipped: already created as idx_invoice_items_product in V6

-- Composite index for the correlated subqueries (tenant + status filter + ordering)
CREATE INDEX IF NOT EXISTS idx_invoices_tenant_status_dates 
ON core.invoices(tenant_id, status, issue_date DESC, created_at DESC);

-- Products tenant_id is partial-covered by idx_products_tenant_category (V14) — the inverseProductStats
-- UPDATE uses WHERE id=? AND tenant_id=? and PostgreSQL can use the PK + tenant_id filter.
-- Skipping separate idx_products_tenant_id as redundant.

-- Composite index for invoice_items join with invoices
CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice_product 
ON core.invoice_items(invoice_id, product_id);
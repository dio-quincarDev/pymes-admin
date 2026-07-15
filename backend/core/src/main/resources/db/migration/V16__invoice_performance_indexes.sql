-- Add indexes for query performance in reverseProductStats and product stats updates
CREATE INDEX IF NOT EXISTS idx_invoice_items_product_id ON core.invoice_items(product_id);

-- Composite index for the correlated subqueries (tenant + status filter + ordering)
CREATE INDEX IF NOT EXISTS idx_invoices_tenant_status_dates 
ON core.invoices(tenant_id, status, issue_date DESC, created_at DESC);

-- Products tenant_id index for UPDATE WHERE clause
CREATE INDEX IF NOT EXISTS idx_products_tenant_id ON core.products(tenant_id);

-- Composite index for invoice_items join with invoices
CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice_product 
ON core.invoice_items(invoice_id, product_id);
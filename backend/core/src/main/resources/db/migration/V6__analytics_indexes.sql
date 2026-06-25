CREATE INDEX IF NOT EXISTS idx_invoices_tenant_date ON core.invoices(tenant_id, issue_date);
CREATE INDEX IF NOT EXISTS idx_invoice_items_product ON core.invoice_items(product_id);

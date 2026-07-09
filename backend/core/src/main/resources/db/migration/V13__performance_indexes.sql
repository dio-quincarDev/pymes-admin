-- Performance: covering indexes for new module queries
-- These support MetricasServiceImpl and new module list queries

-- Covering index for invoices type-filtered queries (MetricasServiceImpl)
-- Supports: SUM(total) WHERE tenant_id = ? AND issue_date >= ? AND issue_date < ? AND type = ?
-- ponytail: keeps idx_invoices_tenant_date from V6 — analytics queries don't filter by type
CREATE INDEX IF NOT EXISTS idx_invoices_tenant_date_type
    ON core.invoices(tenant_id, issue_date, type)
    INCLUDE (total);

-- Covering index for loan payments date-filtered aggregation
-- Supports: SUM(lp.amount) JOIN loans WHERE l.tenant_id = ? AND lp.payment_date >= ? AND lp.payment_date < ?
CREATE INDEX IF NOT EXISTS idx_loan_payments_loan_date
    ON core.loan_payments(loan_id, payment_date)
    INCLUDE (amount);


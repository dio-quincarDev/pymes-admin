-- V5: cubrir status en índice de facturas por tenant/date/type
-- Nuevo CTE invoices_opex filtra type='GASTO_OPERATIVO' AND status='PAGADA'
-- (Modelo de Gastos, 2026-08-02). Sin status el plan hace heap lookup por fila.

DROP INDEX IF EXISTS core.idx_invoices_tenant_date_type;

CREATE INDEX IF NOT EXISTS idx_invoices_tenant_date_type
    ON core.invoices(tenant_id, issue_date, type, status)
    INCLUDE (total);

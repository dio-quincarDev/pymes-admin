-- V4: estado factura enum + check constraints (compatible sql-optimization skill)
-- Estado: REGISTRADA | PAGADA | ANULADA (solo PAGADA alimenta analytics/métricas)
-- ponytail: NOT VALID -> VALIDATE evita bloqueo en tabla grande

ALTER TABLE core.invoices
  ADD CONSTRAINT chk_invoices_status CHECK (status IN ('REGISTRADA','PAGADA','ANULADA')) NOT VALID,
  ADD CONSTRAINT chk_invoices_type CHECK (type IN ('FACTURA','GASTO_OPERATIVO')) NOT VALID;

ALTER TABLE core.invoices VALIDATE CONSTRAINT chk_invoices_status, VALIDATE CONSTRAINT chk_invoices_type;

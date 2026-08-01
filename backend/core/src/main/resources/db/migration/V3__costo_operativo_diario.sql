ALTER TABLE core.tenant_financial_metrics
ADD COLUMN IF NOT EXISTS costo_operativo_diario DECIMAL(12,2);

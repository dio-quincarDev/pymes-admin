-- Motor #10 (Financial Health Engine): columna JSONB nullable.
-- Filas existentes → null, mapper retorna objeto vacío.

ALTER TABLE core.expense_analysis ADD COLUMN IF NOT EXISTS financial_health JSONB;

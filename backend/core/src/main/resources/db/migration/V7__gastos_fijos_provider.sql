ALTER TABLE core.gastos_fijos_recurrentes
    ADD COLUMN IF NOT EXISTS provider_id UUID REFERENCES core.providers(id);

CREATE INDEX IF NOT EXISTS idx_gastos_fijos_provider ON core.gastos_fijos_recurrentes(provider_id);

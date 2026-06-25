ALTER TABLE core.invoice_items
    ADD COLUMN presentacion_id UUID REFERENCES core.product_presentations(id),
    ADD COLUMN conversion_factor INTEGER NOT NULL DEFAULT 1;

CREATE INDEX IF NOT EXISTS idx_invoice_items_presentacion ON core.invoice_items(presentacion_id);

CREATE TABLE IF NOT EXISTS expense_analysis (
    id          UUID PRIMARY KEY,
    tenant_id   UUID NOT NULL,
    period      VARCHAR(7) NOT NULL,
    abc         JSONB,
    trend       JSONB,
    margin      JSONB,
    opex_pct    JSONB,
    projection  JSONB,
    alerts      JSONB,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_expense_analysis_tenant_period ON expense_analysis(tenant_id, period);

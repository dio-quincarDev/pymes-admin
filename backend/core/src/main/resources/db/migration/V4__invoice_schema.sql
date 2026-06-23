CREATE TABLE IF NOT EXISTS providers (
    id         UUID PRIMARY KEY,
    tenant_id  UUID NOT NULL,
    name       VARCHAR(150) NOT NULL,
    ruc        VARCHAR(50),
    is_active  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_providers_tenant ON providers(tenant_id);

CREATE TABLE IF NOT EXISTS invoices (
    id               UUID PRIMARY KEY,
    tenant_id        UUID NOT NULL,
    provider_id      UUID NOT NULL REFERENCES providers(id),
    invoice_number   VARCHAR(50) NOT NULL,
    issue_date       DATE NOT NULL,
    type             VARCHAR(20) NOT NULL,
    global_discount  DECIMAL(12,2) NOT NULL DEFAULT 0,
    payment_method   VARCHAR(50),
    status           VARCHAR(20) NOT NULL DEFAULT 'REGISTRADA',
    total            DECIMAL(12,2) NOT NULL,
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_invoices_tenant ON invoices(tenant_id);
CREATE INDEX IF NOT EXISTS idx_invoices_provider ON invoices(provider_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_invoices_number_tenant ON invoices(tenant_id, invoice_number);

CREATE TABLE IF NOT EXISTS invoice_items (
    id           UUID PRIMARY KEY,
    invoice_id   UUID NOT NULL REFERENCES invoices(id),
    product_id   UUID NOT NULL REFERENCES products(id),
    product_name VARCHAR(150) NOT NULL,
    quantity     DECIMAL(12,2) NOT NULL,
    unit_price   DECIMAL(12,2) NOT NULL,
    discount     DECIMAL(12,2) NOT NULL DEFAULT 0,
    subtotal     DECIMAL(12,2) NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice ON invoice_items(invoice_id);

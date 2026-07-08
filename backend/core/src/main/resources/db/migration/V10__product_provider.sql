ALTER TABLE core.products
    ADD COLUMN provider_id UUID REFERENCES core.providers(id);

CREATE INDEX IF NOT EXISTS idx_products_provider ON core.products(provider_id);

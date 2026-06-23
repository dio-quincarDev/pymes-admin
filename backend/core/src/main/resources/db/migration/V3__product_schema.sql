CREATE TABLE IF NOT EXISTS products (
    id         UUID PRIMARY KEY,
    tenant_id  UUID NOT NULL,
    name       VARCHAR(150) NOT NULL,
    sku        VARCHAR(50),
    category   VARCHAR(100),
    base_unit  VARCHAR(50),
    image_url  VARCHAR(500),
    is_active  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_products_tenant ON products(tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_products_tenant_sku ON products(tenant_id, sku) WHERE sku IS NOT NULL;

CREATE TABLE IF NOT EXISTS product_presentations (
    id          UUID PRIMARY KEY,
    product_id  UUID NOT NULL REFERENCES products(id),
    name        VARCHAR(100) NOT NULL,
    conversion  INTEGER NOT NULL,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_presentations_product ON product_presentations(product_id);

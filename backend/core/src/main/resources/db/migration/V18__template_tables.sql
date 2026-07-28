-- Template tables for industry seed data (moved from SeedDataRunner.createTables())

CREATE TABLE IF NOT EXISTS template_units (
    id UUID PRIMARY KEY,
    industry_code VARCHAR(50) NOT NULL REFERENCES industries(code),
    name VARCHAR(100) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_template_units_industry ON template_units(industry_code);

CREATE TABLE IF NOT EXISTS template_payment_methods (
    id UUID PRIMARY KEY,
    industry_code VARCHAR(50) NOT NULL REFERENCES industries(code),
    name VARCHAR(100) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_template_payment_methods_industry ON template_payment_methods(industry_code);

CREATE TABLE IF NOT EXISTS template_products (
    id UUID PRIMARY KEY,
    industry_code VARCHAR(50) NOT NULL REFERENCES industries(code),
    category_id UUID,
    name VARCHAR(150) NOT NULL,
    base_unit VARCHAR(50),
    min_quantity DECIMAL(12,2),
    max_quantity DECIMAL(12,2),
    sort_order INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_template_products_industry ON template_products(industry_code);

CREATE TABLE IF NOT EXISTS template_product_presentations (
    id UUID PRIMARY KEY,
    template_product_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    conversion INTEGER NOT NULL DEFAULT 1,
    sort_order INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_template_product_presentations_fk ON template_product_presentations(template_product_id);

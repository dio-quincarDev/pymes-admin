CREATE TABLE IF NOT EXISTS industries (
    code VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS template_categories (
    id UUID PRIMARY KEY,
    industry_code VARCHAR(50) NOT NULL REFERENCES industries(code),
    name VARCHAR(100) NOT NULL,
    parent_id UUID REFERENCES template_categories(id),
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_template_categories_industry ON template_categories(industry_code);
CREATE INDEX IF NOT EXISTS idx_template_categories_parent ON template_categories(parent_id);

CREATE TABLE IF NOT EXISTS template_locations (
    id UUID PRIMARY KEY,
    industry_code VARCHAR(50) NOT NULL REFERENCES industries(code),
    name VARCHAR(100) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_template_locations_industry ON template_locations(industry_code);

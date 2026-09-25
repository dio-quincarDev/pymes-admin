-- V5: pagination A-Z indexes (idx_products_tenant_name / idx_providers_tenant_name)
-- ponytail: functional index on lower(name) avoids Sort for ORDER BY name ASC pagination
CREATE INDEX IF NOT EXISTS idx_products_tenant_name ON core.products(tenant_id, lower(name));
CREATE INDEX IF NOT EXISTS idx_providers_tenant_name ON core.providers(tenant_id, lower(name));

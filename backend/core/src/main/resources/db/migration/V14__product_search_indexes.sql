-- Performance: composite + partial indexes for paginated product search
-- Supports: findByTenantIdAndCategory, findByTenantIdAndCategoryAndNameContainingIgnoreCase
CREATE INDEX IF NOT EXISTS idx_products_tenant_category
    ON core.products(tenant_id, category);

-- Partial index: only active products — reduces index size + speeds up COUNT for Page
-- Supports: all product queries that filter by is_active = true (all of them)
CREATE INDEX IF NOT EXISTS idx_products_active_tenant
    ON core.products(tenant_id)
    WHERE is_active = true;

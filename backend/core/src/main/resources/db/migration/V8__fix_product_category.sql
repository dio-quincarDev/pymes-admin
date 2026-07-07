UPDATE core.products
SET category = tc.id::text
FROM template_categories tc, core.tenant_setup ts
WHERE core.products.tenant_id = ts.tenant_id
  AND ts.industry = tc.industry_code
  AND tc.name = core.products.category
  AND core.products.category IS NOT NULL
  AND core.products.category !~ '^[0-9a-f]{8}-';

-- V7: normalizar unidades de producto (base_unit nombre -> ID, conversion a decimal) + suelto explicito.
-- Idempotente: guards IF NOT EXISTS / NOT uuid-like, misma receta que la vieja V8 de categorias.

-- ============ A0. vacios ("") -> NULL (Pimienta, Brillos) ============
UPDATE core.products SET base_unit = NULL WHERE base_unit = '';
UPDATE core.template_products SET base_unit = NULL WHERE base_unit = '';

-- ============ A0b. puente "Botella": agregarla solo donde haya productos que la usan ============
-- (el nombre solo se usa como llave durante esta migracion; despues todo queda en codigo)
INSERT INTO core.template_units (id, industry_code, name, sort_order)
SELECT gen_random_uuid(), need.industry_code, 'Botella',
       COALESCE((SELECT MAX(tu.sort_order) FROM core.template_units tu WHERE tu.industry_code = need.industry_code), 0) + 1
FROM (
    SELECT DISTINCT ts.industry AS industry_code
    FROM core.tenant_setup ts
    WHERE EXISTS (SELECT 1 FROM core.products p WHERE p.tenant_id = ts.tenant_id AND p.base_unit = 'Botella')
    UNION
    SELECT DISTINCT tp.industry_code
    FROM core.template_products tp
    WHERE tp.base_unit = 'Botella'
) need
WHERE NOT EXISTS (SELECT 1 FROM core.template_units tu
                  WHERE tu.industry_code = need.industry_code AND tu.name = 'Botella');

-- ============ A. base_unit: nombre ("Lb", "Kilo") -> ID de template_units ============
UPDATE core.products
SET base_unit = tu.id::text
FROM core.template_units tu, core.tenant_setup ts
WHERE core.products.tenant_id = ts.tenant_id
  AND ts.industry = tu.industry_code
  AND tu.name = core.products.base_unit
  AND core.products.base_unit IS NOT NULL
  AND core.products.base_unit !~ '^[0-9a-f]{8}-';

UPDATE core.template_products
SET base_unit = tu.id::text
FROM core.template_units tu
WHERE tu.industry_code = core.template_products.industry_code
  AND tu.name = core.template_products.base_unit
  AND core.template_products.base_unit IS NOT NULL
  AND core.template_products.base_unit !~ '^[0-9a-f]{8}-';

-- ============ B. conversion: INTEGER -> NUMERIC(19,6) (galon 3.785, etc.) ============
ALTER TABLE core.product_presentations ALTER COLUMN conversion TYPE NUMERIC(19,6) USING conversion::NUMERIC(19,6);
ALTER TABLE core.invoice_items ALTER COLUMN conversion_factor TYPE NUMERIC(19,6) USING conversion_factor::NUMERIC(19,6);
ALTER TABLE core.template_product_presentations ALTER COLUMN conversion TYPE NUMERIC(19,6) USING conversion::NUMERIC(19,6);

-- ============ C. suelto explicito en items ============
ALTER TABLE core.invoice_items ADD COLUMN IF NOT EXISTS fue_suelto BOOLEAN NOT NULL DEFAULT FALSE;

-- ============ D. unidades globales: mismo ID en todas las industrias (IDs = seedGlobalUnits) ============
INSERT INTO core.industries (code, name) VALUES ('global', 'Global') ON CONFLICT DO NOTHING;

INSERT INTO core.template_units (id, industry_code, name, sort_order)
SELECT v.id::uuid, 'global', v.name, v.ord
FROM (VALUES
    ('4f53e997-a24e-471d-9301-78b656e3708c', 'Kg', 1),
    ('8ac6e70b-cffa-4987-9aa8-9e8d0b4e7275', 'Gr', 2),
    ('6e75362d-3e00-460f-82bb-8a1a2a771348', 'Lb', 3),
    ('e26aac0f-a0b3-4ee4-8409-38a6a8573757', 'Oz', 4),
    ('8c3b5709-cc1d-4b30-a33c-e7c4585eeacf', 'Ml', 5),
    ('b5124ee3-6255-4d0b-b458-84444986b7cf', 'Litro', 6),
    ('8633021b-621c-48e4-ba9b-2c897f686c84', 'Galón', 7),
    ('d68cf1b9-9b0f-43ca-9e08-f052fb89919d', 'Unidad', 8)
) v(id, name, ord)
WHERE NOT EXISTS (SELECT 1 FROM core.template_units tu WHERE tu.industry_code = 'global' AND tu.name = v.name);

-- re-apuntar IDs viejos por industria -> ID global (por nombre); lo propio de industria (Botella...) no se toca
UPDATE core.products p SET base_unit = g.id::text
FROM core.template_units old, core.template_units g
WHERE p.base_unit = old.id::text
  AND old.industry_code <> 'global'
  AND g.industry_code = 'global'
  AND old.name = g.name;

UPDATE core.template_products tp SET base_unit = g.id::text
FROM core.template_units old, core.template_units g
WHERE tp.base_unit = old.id::text
  AND old.industry_code <> 'global'
  AND g.industry_code = 'global'
  AND old.name = g.name;

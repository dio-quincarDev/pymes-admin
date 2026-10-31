-- V9: desglose de una venta diaria por activo (metodo de pago).
-- Aditiva: no consolida V1 (ver CORE_MIGRATIONS_STRATEGY.md).
-- Una venta diaria es el cierre del dia y puede tener varios activos adentro,
-- por eso se modela como tabla hija y no como columna.

CREATE TABLE IF NOT EXISTS core.daily_sale_payments (
    id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sale_id UUID NOT NULL REFERENCES core.daily_sales(id) ON DELETE CASCADE,
    metodo  VARCHAR(20) NOT NULL CHECK (metodo IN ('EFECTIVO','ACH','YAPPY','TARJETA','CHEQUE','CREDITO')),
    monto   DECIMAL(12,2) NOT NULL CHECK (monto > 0),
    UNIQUE (sale_id, metodo)
);

CREATE INDEX IF NOT EXISTS idx_daily_sale_payments_sale ON core.daily_sale_payments(sale_id);
CREATE INDEX IF NOT EXISTS idx_daily_sale_payments_metodo ON core.daily_sale_payments(metodo);

-- Backfill: convierte la descripcion libre en filas estructuradas.
-- Whitelist hardcodeada (data migration congelada; no depende de template_payment_methods,
-- que se siembra en Java por SeedDataRunner despues de Flyway).
-- Solo se insertan los dias cuyo desglose parsea completo y suma exactamente gross_amount.
WITH split AS (
    SELECT ds.id AS sale_id, ds.gross_amount, p.part
    FROM core.daily_sales ds
    CROSS JOIN LATERAL regexp_split_to_table(
        coalesce(ds.description, ''), ',\s*|\.\s+(?=[A-Za-z])') AS p(part)
    WHERE ds.is_active AND ds.description IS NOT NULL
),
split_counts AS (
    SELECT sale_id, count(*) AS n FROM split GROUP BY sale_id
),
parsed AS (
    SELECT s.sale_id, s.gross_amount, x.m[1] AS raw_label, x.m[2] AS raw_amount
    FROM split s
    CROSS JOIN LATERAL (
        SELECT regexp_match(btrim(s.part),
            '^\s*([A-Za-z. ]+?)\s*[:;.,]?\s*\$?\s*(\d+(?:[.,]\d{1,2})?)') AS m
    ) x
    WHERE x.m IS NOT NULL
),
mapped AS (
    SELECT sale_id, gross_amount,
           CASE lower(btrim(regexp_replace(raw_label, '[.;]+$', '')))
               WHEN 'efectivo' THEN 'EFECTIVO'
               WHEN 'yappy'    THEN 'YAPPY'
               WHEN 'yappi'    THEN 'YAPPY'
               WHEN 'ben'      THEN 'YAPPY'
               WHEN 'ban'      THEN 'YAPPY'
               WHEN 'pventa'   THEN 'TARJETA'
               WHEN 'p.venta'  THEN 'TARJETA'
               WHEN 'punto de venta' THEN 'TARJETA'
               WHEN 'punto venta'    THEN 'TARJETA'
               WHEN 'pcobro'   THEN 'TARJETA'
               WHEN 'ach'      THEN 'ACH'
               WHEN 'cheque'   THEN 'CHEQUE'
               WHEN 'credito'  THEN 'CREDITO'
               WHEN 'crédito'  THEN 'CREDITO'
               ELSE NULL
           END AS metodo,
           replace(raw_amount, ',', '.')::numeric AS monto
    FROM parsed
),
valid_sales AS (
    SELECT m.sale_id
    FROM mapped m
    JOIN split_counts sc ON sc.sale_id = m.sale_id
    GROUP BY m.sale_id, m.gross_amount, sc.n
    HAVING count(*) FILTER (WHERE m.metodo IS NULL) = 0
       AND sum(m.monto) = m.gross_amount
       AND count(*) = sc.n
),
agg AS (
    SELECT m.sale_id, m.metodo, sum(m.monto) AS monto
    FROM mapped m
    JOIN valid_sales v ON v.sale_id = m.sale_id
    GROUP BY m.sale_id, m.metodo
)
INSERT INTO core.daily_sale_payments (sale_id, metodo, monto)
SELECT sale_id, metodo, monto
FROM agg
ON CONFLICT (sale_id, metodo) DO NOTHING;

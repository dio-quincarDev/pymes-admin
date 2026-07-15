ALTER TABLE core.invoice_items
    ADD COLUMN IF NOT EXISTS cantidad_presentacion NUMERIC(19,6),
    ADD COLUMN IF NOT EXISTS valor_presentacion NUMERIC(19,6),
    ADD COLUMN IF NOT EXISTS precio_unitario_input NUMERIC(19,6),
    ADD COLUMN IF NOT EXISTS descuento_input NUMERIC(19,6),
    ADD COLUMN IF NOT EXISTS descuento_es_porcentaje BOOLEAN;

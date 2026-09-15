-- ITBMS por item: 0% exento, 7% general, 10% alcohol/hospedaje. 15% reservado futuro.
ALTER TABLE core.invoice_items ADD COLUMN IF NOT EXISTS itbms_tasa SMALLINT NOT NULL DEFAULT 7 CHECK (itbms_tasa IN (0, 7, 10));
ALTER TABLE core.invoice_items ADD COLUMN IF NOT EXISTS itbms_monto DECIMAL(12,2) NOT NULL DEFAULT 0;

ALTER TABLE core.invoices ADD COLUMN IF NOT EXISTS subtotal_exento DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE core.invoices ADD COLUMN IF NOT EXISTS subtotal_gravado DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE core.invoices ADD COLUMN IF NOT EXISTS itbms_total DECIMAL(12,2) NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_invoice_items_itbms ON core.invoice_items(itbms_tasa);

-- V6: facturas GASTO_OPERATIVO sin proveedor (monto directo: salarios, luz, gas, agua...)
-- El proveedor deja de ser obligatorio; solo aplica a facturas con items de producto.

ALTER TABLE core.invoices ALTER COLUMN provider_id DROP NOT NULL;

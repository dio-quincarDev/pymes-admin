-- Costos engine: colaboradores + gastos fijos recurrentes + config laboral.

CREATE TABLE IF NOT EXISTS core.collaboradores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    tipo_pago VARCHAR(20) NOT NULL CHECK (tipo_pago IN ('DIARIO', 'SEMANAL', 'QUINCENAL', 'MENSUAL')),
    monto DECIMAL(12,2) NOT NULL CHECK (monto > 0),
    activo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_collaboradores_tenant ON core.collaboradores(tenant_id);

-- FK from invoices.colaborador_id (defined in V1 without FK since collaboradores didn't exist yet)
ALTER TABLE core.invoices
    ADD CONSTRAINT fk_invoices_colaborador FOREIGN KEY (colaborador_id) REFERENCES core.collaboradores(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS core.gastos_fijos_recurrentes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    categoria VARCHAR(50) NOT NULL,
    monto DECIMAL(12,2) NOT NULL CHECK (monto > 0),
    descripcion VARCHAR(255),
    dia_ejecucion INT NOT NULL CHECK (dia_ejecucion BETWEEN 1 AND 31),
    metodo_pago VARCHAR(50),
    provider_id UUID REFERENCES core.providers(id),
    activo BOOLEAN DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_gastos_fijos_tenant ON core.gastos_fijos_recurrentes(tenant_id);
CREATE INDEX IF NOT EXISTS idx_gastos_fijos_provider ON core.gastos_fijos_recurrentes(provider_id);

CREATE TABLE IF NOT EXISTS core.config_laboral (
    tenant_id UUID PRIMARY KEY,
    dias_laborales INT NOT NULL DEFAULT 26 CHECK (dias_laborales BETWEEN 1 AND 31)
);

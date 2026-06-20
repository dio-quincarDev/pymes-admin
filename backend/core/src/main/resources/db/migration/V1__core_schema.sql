CREATE TABLE IF NOT EXISTS tenant_setup (
    id                   UUID PRIMARY KEY,
    tenant_id            UUID NOT NULL UNIQUE,
    industry             VARCHAR(50),
    onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_tenant_setup_tenant_id ON tenant_setup(tenant_id);

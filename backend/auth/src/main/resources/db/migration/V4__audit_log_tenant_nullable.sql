-- Permitir audit_log sin tenant (eventos de seguridad pre-login: login fallido, etc.)
ALTER TABLE audit_log ALTER COLUMN tenant_id DROP NOT NULL;
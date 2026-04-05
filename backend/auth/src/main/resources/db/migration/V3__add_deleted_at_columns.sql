-- ============================================================
-- V3: Agregar columna deleted_at para auditoría forense (IA Ready)
-- ============================================================

-- Users: timestamp de desvinculación para análisis histórico
ALTER TABLE users
    ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

-- Tenants: timestamp de baja para auditoría
ALTER TABLE tenants
    ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

-- UserTenants: timestamp de desvinculación usuario-tenant
ALTER TABLE user_tenants
    ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

COMMENT ON COLUMN users.deleted_at IS 'Timestamp de desvinculación lógica (soft delete forense)';
COMMENT ON COLUMN tenants.deleted_at IS 'Timestamp de baja del tenant (soft delete forense)';
COMMENT ON COLUMN user_tenants.deleted_at IS 'Timestamp de desvinculación usuario-tenant (soft delete forense)';

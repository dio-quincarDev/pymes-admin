-- ============================================================
-- PyMes Auth Microservice - Index Optimizations
-- ============================================================

-- Composite index for user_tenants queries filtering by tenant + is_active
-- Covers: countByTenantIdAndIsActiveTrue, findByTenantIdAndIsActiveTrue
CREATE INDEX IF NOT EXISTS idx_user_tenants_tenant_active
    ON user_tenants(tenant_id, is_active);

-- Composite index for user_tenants queries filtering by user + is_active
-- Covers: findByUserIdAndIsActiveTrue (2 variants)
CREATE INDEX IF NOT EXISTS idx_user_tenants_user_active
    ON user_tenants(user_id, is_active);

-- Composite index for refresh_tokens queries filtering by user + revoked
-- Covers: deleteByUserId (non-revoked tokens), revoked status lookups
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_revoked
    ON refresh_tokens(user_id, revoked);

-- Composite index for invitations filtering by tenant + email
-- Covers: findByEmailAndTenantId, existsByTenantIdAndEmailAndAcceptedAtIsNull
CREATE INDEX IF NOT EXISTS idx_invitations_tenant_email
    ON invitations(tenant_id, email);

-- Partial index for pending invitations (not yet accepted)
CREATE INDEX IF NOT EXISTS idx_invitations_pending
    ON invitations(tenant_id)
    WHERE accepted_at IS NULL;

-- Composite index for audit_log queries by tenant + action
-- Covers: common audit trail queries filtering by tenant and action type
CREATE INDEX IF NOT EXISTS idx_audit_log_tenant_action
    ON audit_log(tenant_id, action, created_at);

-- GIN index for JSONB queries on audit_log.details (only if JSONB queries are used)
-- Skipped: add only when queries filter on details @> '{"key":"val"}'

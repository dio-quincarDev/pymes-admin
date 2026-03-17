-- ============================================================
-- PyMes Auth Microservice - Initial Schema
-- ============================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- USERS (usuarios globales, pueden pertenecer a múltiples tenants)
-- ============================================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    picture_url TEXT,
    phone VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT users_provider_provider_id_unique UNIQUE (provider, provider_id)
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_provider ON users(provider, provider_id);

-- ============================================================
-- TENANTS (empresas/organizaciones)
-- ============================================================
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(50) UNIQUE NOT NULL,
    industry VARCHAR(50),
    plan VARCHAR(50) DEFAULT 'free' NOT NULL,
    plan_expires_at TIMESTAMP WITH TIME ZONE,
    max_users INTEGER DEFAULT 1 NOT NULL,
    stripe_customer_id VARCHAR(255),
    logo_url TEXT,
    timezone VARCHAR(50) DEFAULT 'America/Panama' NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD' NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_tenants_slug ON tenants(slug);
CREATE INDEX idx_tenants_plan ON tenants(plan);
CREATE INDEX idx_tenants_active ON tenants(is_active);

-- ============================================================
-- USER_TENANTS (relación usuario-tenant con rol)
-- ============================================================
CREATE TABLE user_tenants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    invited_by UUID REFERENCES users(id),
    invited_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT user_tenants_user_tenant_unique UNIQUE (user_id, tenant_id),
    CONSTRAINT user_tenants_role_check CHECK (role IN ('OWNER', 'ADMIN', 'CONTABLE', 'VIEWER'))
);

CREATE INDEX idx_user_tenants_user ON user_tenants(user_id);
CREATE INDEX idx_user_tenants_tenant ON user_tenants(tenant_id);
CREATE INDEX idx_user_tenants_role ON user_tenants(role);

-- ============================================================
-- INVITATIONS (invitaciones pendientes)
-- ============================================================
CREATE TABLE invitations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    invited_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT invitations_role_check CHECK (role IN ('OWNER', 'ADMIN', 'CONTABLE', 'VIEWER'))
);

CREATE INDEX idx_invitations_token ON invitations(token);
CREATE INDEX idx_invitations_email ON invitations(email);
CREATE INDEX idx_invitations_tenant ON invitations(tenant_id);

-- ============================================================
-- REFRESH_TOKENS (tokens JWT revocables)
-- ============================================================
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id UUID REFERENCES tenants(id) ON DELETE SET NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_revoked ON refresh_tokens(revoked);

-- ============================================================
-- AUDIT_LOG (auditoría de acciones)
-- ============================================================
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    resource VARCHAR(100),
    resource_id UUID,
    details JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_audit_log_tenant ON audit_log(tenant_id);
CREATE INDEX idx_audit_log_user ON audit_log(user_id);
CREATE INDEX idx_audit_log_action ON audit_log(action);
CREATE INDEX idx_audit_log_created ON audit_log(created_at);

-- ============================================================
-- SEED DATA - Roles iniciales (información en código)
-- ============================================================
-- Los roles y permisos están hardcodeados en la aplicación
-- Ver: auth.pymes.common.enums.RoleName y Permission

-- ============================================================
-- TRIGGERS - Actualizar updated_at automáticamente
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_tenants_updated_at
    BEFORE UPDATE ON tenants
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- COMENTARIOS
-- ============================================================
COMMENT ON TABLE users IS 'Usuarios globales del sistema (pueden pertenecer a múltiples tenants)';
COMMENT ON TABLE tenants IS 'Empresas/organizaciones (multi-tenant)';
COMMENT ON TABLE user_tenants IS 'Relación usuario-tenant con rol asignado';
COMMENT ON TABLE invitations IS 'Invitaciones pendientes a tenants';
COMMENT ON TABLE refresh_tokens IS 'Tokens JWT refresh (para revocar logout)';
COMMENT ON TABLE audit_log IS 'Log de auditoría de todas las acciones';

COMMENT ON COLUMN users.provider IS 'Proveedor OAuth2: google, facebook, email';
COMMENT ON COLUMN users.provider_id IS 'ID del usuario en el proveedor OAuth2';
COMMENT ON COLUMN tenants.plan IS 'Plan de suscripción: free, starter, pro, enterprise';
COMMENT ON COLUMN tenants.slug IS 'Identificador URL-friendly único';
COMMENT ON COLUMN user_tenants.role IS 'Rol del usuario en el tenant: OWNER, ADMIN, CONTABLE, VIEWER';
COMMENT ON COLUMN invitations.token IS 'Token único para aceptar invitación';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'Hash del token (no se guarda plain text)';

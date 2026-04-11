-- ============================================================
-- Agregar columna email_verified_at para verificación de email
-- ============================================================

ALTER TABLE users
    ADD COLUMN email_verified_at TIMESTAMP WITH TIME ZONE;

-- Índice para consultas de usuarios no verificados
CREATE INDEX idx_users_email_verified ON users(email_verified_at)
    WHERE email_verified_at IS NULL;

COMMENT ON COLUMN users.email_verified_at IS 'Fecha de verificación del email (null = no verificado)';

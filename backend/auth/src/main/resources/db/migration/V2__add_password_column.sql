-- ============================================================
-- V2: Agregar columna password para auth local (user/password)
-- ============================================================

ALTER TABLE users
    ADD COLUMN password VARCHAR(255);

-- Los usuarios OAuth2 existentes tendrán password = NULL
-- Los usuarios locales tendrán password con hash BCrypt
-- El provider ya incluye 'LOCAL' en el CHECK existente

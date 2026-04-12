-- ============================================================
-- PyMes Auth Microservice - Add unique constraint to refresh_tokens
-- ============================================================

-- First, ensure there are no duplicates (though with the JTI fix, new ones won't be created)
-- For existing data, we could just delete old ones or keep them.
-- In local/test environment, we'll just add the constraint.
ALTER TABLE refresh_tokens ADD CONSTRAINT refresh_tokens_token_hash_unique UNIQUE (token_hash);

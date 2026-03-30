-- ============================================================
-- V2: Normalize Enums to Uppercase and add Check Constraints
-- ============================================================

-- 1. Normalize 'tenants' table
-- Update existing lowercase 'free' to 'FREE'
UPDATE tenants SET plan = UPPER(plan);

-- Change default to uppercase 'FREE'
ALTER TABLE tenants ALTER COLUMN plan SET DEFAULT 'FREE';

-- Add check constraint for plans
ALTER TABLE tenants ADD CONSTRAINT tenants_plan_check 
    CHECK (plan IN ('FREE', 'STARTER', 'PRO', 'ENTERPRISE'));


-- 2. Normalize 'users' table
-- Update existing providers to uppercase
UPDATE users SET provider = UPPER(provider);

-- Add check constraint for providers
ALTER TABLE users ADD CONSTRAINT users_provider_check 
    CHECK (provider IN ('GOOGLE', 'FACEBOOK', 'LOCAL'));


-- 3. Normalize 'user_tenants' and 'invitations' (just in case)
UPDATE user_tenants SET role = UPPER(role);
UPDATE invitations SET role = UPPER(role);

-- Note: user_tenants and invitations already had uppercase CHECKS in V1, 
-- so this ensures data consistency if any was inserted manually in lowercase.

ALTER TABLE core.providers
    ADD COLUMN contact_name  VARCHAR(150),
    ADD COLUMN contact_phone VARCHAR(30),
    ADD COLUMN contact_email VARCHAR(150);

ALTER TABLE core.providers DROP COLUMN ruc;

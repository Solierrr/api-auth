ALTER TABLE auth_user
    ADD COLUMN locale VARCHAR(5) NOT NULL DEFAULT 'pt-BR',
    ADD CONSTRAINT ck_auth_user_locale CHECK (locale IN ('pt-BR', 'en-US', 'es-ES'));

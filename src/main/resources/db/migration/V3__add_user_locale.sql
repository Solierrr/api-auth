-- Idempotente de propósito: a qa começa de V1__baseline.sql e recebeu esta mudança antes como V2__add_user_locale.sql;
-- o mesmo arquivo V3 nas duas branches evita conflito nas sincronizações e roda sem efeito onde a coluna já existe.
ALTER TABLE auth_user
    ADD COLUMN IF NOT EXISTS locale VARCHAR(5) NOT NULL DEFAULT 'pt-BR';

ALTER TABLE auth_user
    DROP CONSTRAINT IF EXISTS ck_auth_user_locale;

ALTER TABLE auth_user
    ADD CONSTRAINT ck_auth_user_locale CHECK (locale IN ('pt-BR', 'en-US', 'es-ES'));

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE auth_user (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    primary_email CITEXT NOT NULL,
    email_verified_at TIMESTAMPTZ,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_auth_user PRIMARY KEY (id),
    CONSTRAINT uq_auth_user_primary_email UNIQUE (primary_email),
    CONSTRAINT ck_auth_user_failed_login_attempts CHECK (failed_login_attempts >= 0),
    CONSTRAINT ck_auth_user_status CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED'))
);

CREATE TABLE local_credential (
    auth_user_id UUID NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    password_changed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_password_change_required BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_local_credential PRIMARY KEY (auth_user_id),
    CONSTRAINT fk_local_credential_auth_user FOREIGN KEY (auth_user_id)
        REFERENCES auth_user (id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE federated_identity (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    auth_user_id UUID NOT NULL,
    authority VARCHAR(30) NOT NULL,
    issuer TEXT NOT NULL,
    subject TEXT NOT NULL,
    email CITEXT,
    is_email_verified BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_federated_identity PRIMARY KEY (id),
    CONSTRAINT fk_federated_identity_auth_user FOREIGN KEY (auth_user_id)
        REFERENCES auth_user (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT uq_federated_identity_issuer_subject UNIQUE (issuer, subject),
    CONSTRAINT uq_federated_identity_auth_user_issuer UNIQUE (auth_user_id, issuer),
    CONSTRAINT ck_federated_identity_authority CHECK (authority IN ('FIREBASE'))
);

CREATE TABLE federated_provider_link (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    federated_identity_id UUID NOT NULL,
    provider VARCHAR(30) NOT NULL,
    provider_subject TEXT NOT NULL,
    email CITEXT,
    linked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_federated_provider_link PRIMARY KEY (id),
    CONSTRAINT fk_federated_provider_link_federated_identity FOREIGN KEY (federated_identity_id)
        REFERENCES federated_identity (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT uq_federated_provider_link_federated_identity_provider UNIQUE (federated_identity_id, provider),
    CONSTRAINT uq_federated_provider_link_provider_subject UNIQUE (provider, provider_subject),
    CONSTRAINT ck_federated_provider_link_provider CHECK (provider IN ('GOOGLE', 'MICROSOFT'))
);

CREATE TABLE totp_factor (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    auth_user_id UUID NOT NULL,
    secret_ciphertext BYTEA NOT NULL,
    secret_nonce BYTEA NOT NULL,
    encryption_key_id VARCHAR(100) NOT NULL,
    algorithm VARCHAR(16) NOT NULL DEFAULT 'SHA1',
    digits SMALLINT NOT NULL DEFAULT 6,
    period_seconds SMALLINT NOT NULL DEFAULT 30,
    enabled_at TIMESTAMPTZ,
    last_used_counter BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_totp_factor PRIMARY KEY (id),
    CONSTRAINT uq_totp_factor_auth_user UNIQUE (auth_user_id),
    CONSTRAINT fk_totp_factor_auth_user FOREIGN KEY (auth_user_id)
        REFERENCES auth_user (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_totp_factor_algorithm CHECK (algorithm IN ('SHA1', 'SHA256', 'SHA512')),
    CONSTRAINT ck_totp_factor_digits CHECK (digits IN (6, 8)),
    CONSTRAINT ck_totp_factor_period CHECK (period_seconds BETWEEN 15 AND 60)
);

CREATE TABLE mfa_recovery_code (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    totp_factor_id UUID NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    used_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_mfa_recovery_code PRIMARY KEY (id),
    CONSTRAINT fk_mfa_recovery_code_totp_factor FOREIGN KEY (totp_factor_id)
        REFERENCES totp_factor (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT uq_mfa_recovery_code_totp_factor_code_hash UNIQUE (totp_factor_id, code_hash)
);

CREATE TABLE auth_session (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    auth_user_id UUID NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    device TEXT,
    authentication_methods TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    mfa_completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_access_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    revocation_reason VARCHAR(60),
    client_id VARCHAR(100) NOT NULL DEFAULT 'solaria-web',
    context VARCHAR(30) NOT NULL DEFAULT 'web',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_auth_session PRIMARY KEY (id),
    CONSTRAINT fk_auth_session_auth_user FOREIGN KEY (auth_user_id)
        REFERENCES auth_user (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_auth_session_expiration CHECK (expires_at > created_at)
);

CREATE TABLE refresh_token (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    auth_session_id UUID NOT NULL,
    token_hash BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    replaced_by_refresh_token_id UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_refresh_token PRIMARY KEY (id),
    CONSTRAINT uq_refresh_token_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_auth_session FOREIGN KEY (auth_session_id)
        REFERENCES auth_session (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_refresh_token_replaced_by_refresh_token FOREIGN KEY (replaced_by_refresh_token_id)
        REFERENCES refresh_token (id) ON DELETE SET NULL,
    CONSTRAINT ck_refresh_token_expiration CHECK (expires_at > created_at),
    CONSTRAINT ck_refresh_token_consumption CHECK (consumed_at IS NULL OR consumed_at >= created_at),
    CONSTRAINT ck_refresh_token_revocation CHECK (revoked_at IS NULL OR revoked_at >= created_at)
);

CREATE TABLE one_time_token (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    auth_user_id UUID NOT NULL,
    type VARCHAR(24) NOT NULL,
    token_hash BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_one_time_token PRIMARY KEY (id),
    CONSTRAINT uq_one_time_token_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_one_time_token_auth_user FOREIGN KEY (auth_user_id)
        REFERENCES auth_user (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_one_time_token_type CHECK (type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET', 'ACCOUNT_LINK')),
    CONSTRAINT ck_one_time_token_expiration CHECK (expires_at > created_at)
);

CREATE TABLE security_event (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    auth_user_id UUID,
    auth_session_id UUID,
    event_type VARCHAR(32) NOT NULL,
    is_successful BOOLEAN NOT NULL DEFAULT true,
    ip_address VARCHAR(45),
    user_agent TEXT,
    details JSONB,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_security_event PRIMARY KEY (id),
    CONSTRAINT fk_security_event_auth_user FOREIGN KEY (auth_user_id)
        REFERENCES auth_user (id) ON DELETE SET NULL,
    CONSTRAINT fk_security_event_auth_session FOREIGN KEY (auth_session_id)
        REFERENCES auth_session (id) ON DELETE SET NULL,
    CONSTRAINT ck_security_event_event_type CHECK (event_type IN (
        'USER_REGISTERED',
        'LOGIN_SUCCEEDED',
        'LOGIN_FAILED',
        'LOGOUT',
        'PASSWORD_CHANGED',
        'PASSWORD_RESET_REQUESTED',
        'PASSWORD_RESET_COMPLETED',
        'FEDERATED_IDENTITY_LINKED',
        'FEDERATED_IDENTITY_UNLINKED',
        'MFA_ENABLED',
        'MFA_DISABLED',
        'MFA_CHALLENGE_FAILED',
        'REFRESH_TOKEN_REUSED',
        'SESSION_REVOKED'
    ))
);

CREATE TABLE outbox_event (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(60) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_outbox_event PRIMARY KEY (id),
    CONSTRAINT ck_outbox_event_attempts CHECK (attempts >= 0)
);

CREATE TABLE mobile_auth_transaction (
    id VARCHAR(43) NOT NULL,
    client_id VARCHAR(100) NOT NULL,
    redirect_uri TEXT NOT NULL,
    state VARCHAR(128) NOT NULL,
    code_challenge VARCHAR(43) NOT NULL,
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    auth_user_id UUID,
    authorizing_auth_session_id UUID,
    code_hash BYTEA,
    code_expires_at TIMESTAMPTZ,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_mobile_auth_transaction PRIMARY KEY (id),
    CONSTRAINT fk_mobile_auth_transaction_auth_user FOREIGN KEY (auth_user_id)
        REFERENCES auth_user (id),
    CONSTRAINT fk_mobile_auth_transaction_authorizing_auth_session FOREIGN KEY (authorizing_auth_session_id)
        REFERENCES auth_session (id),
    CONSTRAINT uq_mobile_auth_transaction_code_hash UNIQUE (code_hash)
);

CREATE TABLE google_calendar_token (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    technician_id VARCHAR(100) NOT NULL,
    access_token BYTEA,
    refresh_token BYTEA,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_google_calendar_token PRIMARY KEY (id),
    CONSTRAINT uq_google_calendar_token_technician UNIQUE (technician_id),
    CONSTRAINT ck_google_calendar_token_status CHECK (status IN ('CONNECTED', 'DISCONNECTED', 'REVOKED')),
    CONSTRAINT ck_google_calendar_token_secrets CHECK (
        (status = 'CONNECTED' AND access_token IS NOT NULL AND refresh_token IS NOT NULL)
        OR (status <> 'CONNECTED' AND access_token IS NULL AND refresh_token IS NULL)
    )
);

CREATE INDEX idx_auth_session_auth_user_active
    ON auth_session (auth_user_id, created_at DESC) WHERE revoked_at IS NULL;
CREATE INDEX idx_refresh_token_auth_session_created
    ON refresh_token (auth_session_id, created_at DESC);
CREATE INDEX idx_one_time_token_auth_user_type
    ON one_time_token (auth_user_id, type, created_at DESC) WHERE consumed_at IS NULL;
CREATE INDEX idx_security_event_auth_user_occurred
    ON security_event (auth_user_id, occurred_at DESC);
CREATE INDEX idx_outbox_event_pending
    ON outbox_event (created_at) WHERE published_at IS NULL;
CREATE INDEX idx_mobile_auth_transaction_expires
    ON mobile_auth_transaction (expires_at);

CREATE SCHEMA export;

CREATE VIEW export.auth_user AS
SELECT id, status, email_verified_at, created_at, updated_at
FROM public.auth_user;

CREATE VIEW export.federated_identity AS
SELECT id, auth_user_id, authority, created_at, updated_at
FROM public.federated_identity;

CREATE VIEW export.federated_provider_link AS
SELECT id, federated_identity_id, provider, linked_at, created_at, updated_at
FROM public.federated_provider_link;

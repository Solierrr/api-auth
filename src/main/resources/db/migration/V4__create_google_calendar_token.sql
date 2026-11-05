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

-- DLQ do provisionamento de User em api-core
CREATE TABLE core_provisioning_dlq (
    auth_user_id UUID NOT NULL,
    last_error TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_core_provisioning_dlq PRIMARY KEY (auth_user_id),
    CONSTRAINT fk_core_provisioning_dlq_user FOREIGN KEY (auth_user_id)
        REFERENCES auth_user (id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE app_users (
    id               BIGSERIAL    NOT NULL PRIMARY KEY,
    uuid             UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    keycloak_subject VARCHAR(255) NOT NULL,
    email            VARCHAR(255),
    display_name     VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_app_users_keycloak_subject UNIQUE (keycloak_subject)
);

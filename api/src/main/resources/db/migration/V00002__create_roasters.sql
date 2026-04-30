CREATE TABLE roasters (
    id                BIGSERIAL    NOT NULL PRIMARY KEY,
    uuid              UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    name              VARCHAR(255) NOT NULL,
    website_url       VARCHAR(512),
    email_list_url    VARCHAR(512),
    url_hints         JSONB,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    submitted_by      BIGINT       REFERENCES app_users(id),
    moderation_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    city              VARCHAR(100),
    state             VARCHAR(100),
    logo_url          VARCHAR(512),
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL
);

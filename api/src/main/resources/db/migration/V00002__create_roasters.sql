CREATE TABLE roasters (
    id                UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    website_url       VARCHAR(512),
    email_list_url    VARCHAR(512),
    url_hints         JSONB,
    is_active         BOOLEAN     NOT NULL DEFAULT TRUE,
    submitted_by      UUID        REFERENCES app_users(id),
    moderation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL
);

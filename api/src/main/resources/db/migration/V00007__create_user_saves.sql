CREATE TABLE user_saves (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES app_users(id),
    product_id UUID        NOT NULL REFERENCES products(id),
    saved_at   TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_user_saves_user_product UNIQUE (user_id, product_id)
);

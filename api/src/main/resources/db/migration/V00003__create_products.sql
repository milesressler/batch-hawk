CREATE TABLE products (
    id                UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    roaster_id        UUID        NOT NULL REFERENCES roasters(id),
    name              VARCHAR(255) NOT NULL,
    roast_level       VARCHAR(100),
    product_type      VARCHAR(100),
    origin_country    VARCHAR(100),
    origin_region     VARCHAR(100),
    process           VARCHAR(100),
    brew_methods      TEXT[],
    flavor_profile    TEXT[],
    is_decaf          BOOLEAN     NOT NULL DEFAULT FALSE,
    availability_type VARCHAR(100),
    description       TEXT,
    is_active         BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_products_roaster_id ON products(roaster_id);

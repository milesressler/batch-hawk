CREATE TABLE product_observations (
    id            BIGSERIAL    NOT NULL PRIMARY KEY,
    uuid          UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    product_id    BIGINT       NOT NULL REFERENCES products(id),
    bag_size      INTEGER,
    bag_size_unit VARCHAR(10),
    bag_size_oz   NUMERIC(6,2),
    price_usd     NUMERIC(8,2),
    price_per_oz  NUMERIC(8,4),
    value_tier    VARCHAR(20),
    in_stock      BOOLEAN,
    observed_at   TIMESTAMPTZ  NOT NULL,
    agent_run_id  BIGINT       REFERENCES agent_runs(id),
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_product_observations_product_observed ON product_observations(product_id, observed_at DESC);

CREATE TABLE field_observations (
    id           BIGSERIAL    NOT NULL PRIMARY KEY,
    uuid         UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    roaster_id   BIGINT       NOT NULL REFERENCES roasters(id),
    field_name   VARCHAR(100) NOT NULL,
    value        JSONB,
    observed_at  TIMESTAMPTZ  NOT NULL,
    source       VARCHAR(20)  NOT NULL,
    confidence   NUMERIC(3,2),
    agent_run_id BIGINT       REFERENCES agent_runs(id),
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_field_observations_roaster_field_observed ON field_observations(roaster_id, field_name, observed_at DESC);

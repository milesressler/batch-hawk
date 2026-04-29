CREATE TABLE promos (
    id             UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    roaster_id     UUID         NOT NULL REFERENCES roasters(id),
    code           VARCHAR(100) NOT NULL,
    discount_type  VARCHAR(30)  NOT NULL,
    discount_value NUMERIC(8,2),
    applies_to     VARCHAR(255),
    expires_at     TIMESTAMPTZ,
    source         VARCHAR(20)  NOT NULL,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    discovered_at  TIMESTAMPTZ  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_promos_roaster_code UNIQUE (roaster_id, code)
);

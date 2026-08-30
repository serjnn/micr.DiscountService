CREATE TABLE IF NOT EXISTS discount_entity (
    id SERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    discount NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT check_discount_range CHECK (discount >= 0.00 AND discount <= 100.00)
);

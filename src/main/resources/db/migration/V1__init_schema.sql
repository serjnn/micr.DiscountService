CREATE TABLE IF NOT EXISTS discount_entity (
    id SERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    discount NUMERIC(10, 2) NOT NULL
);

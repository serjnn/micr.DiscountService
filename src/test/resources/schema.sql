CREATE TABLE IF NOT EXISTS discount_entity (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT,
    discount DOUBLE PRECISION
);

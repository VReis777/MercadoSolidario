ALTER TABLE distributions
    ALTER COLUMN product_id DROP NOT NULL,
    ALTER COLUMN quantity DROP NOT NULL,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADA',
    ADD COLUMN created_by VARCHAR(100),
    ADD COLUMN completed_at TIMESTAMP,
    ADD COLUMN reference VARCHAR(255),
    ADD COLUMN idempotency_key VARCHAR(100),
    ADD COLUMN cancellation_reason TEXT;

UPDATE distributions
SET created_by = users.username,
    completed_at = COALESCE(distributions.distribution_date, distributions.created_at)
FROM users
WHERE distributions.user_id = users.id;

CREATE TABLE distribution_items (
    id BIGSERIAL PRIMARY KEY,
    distribution_id BIGINT NOT NULL REFERENCES distributions(id) ON DELETE RESTRICT,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit VARCHAR(50) NOT NULL,
    stock_movement_id BIGINT REFERENCES stock_movements(id) ON DELETE RESTRICT
);

INSERT INTO distribution_items (distribution_id, product_id, quantity, unit)
SELECT d.id, d.product_id, d.quantity, p.unit
FROM distributions d
JOIN products p ON p.id = d.product_id
WHERE d.product_id IS NOT NULL AND d.quantity IS NOT NULL;

CREATE UNIQUE INDEX uq_distributions_idempotency_key
    ON distributions(idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX idx_distributions_family_created
    ON distributions(family_id, created_at DESC);

CREATE INDEX idx_distribution_items_distribution
    ON distribution_items(distribution_id);

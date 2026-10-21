ALTER TABLE distributions ALTER COLUMN product_id DROP NOT NULL;
ALTER TABLE distributions ALTER COLUMN quantity DROP NOT NULL;
ALTER TABLE distributions ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADA';
ALTER TABLE distributions ADD COLUMN created_by VARCHAR(100);
ALTER TABLE distributions ADD COLUMN completed_at TIMESTAMP;
ALTER TABLE distributions ADD COLUMN reference VARCHAR(255);
ALTER TABLE distributions ADD COLUMN idempotency_key VARCHAR(100);
ALTER TABLE distributions ADD COLUMN cancellation_reason TEXT;

UPDATE distributions
SET created_by = (SELECT users.username FROM users WHERE users.id = distributions.user_id),
    completed_at = COALESCE(distribution_date, created_at)
WHERE EXISTS (SELECT 1 FROM users WHERE users.id = distributions.user_id);

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
    ON distributions(idempotency_key);

CREATE INDEX idx_distributions_family_created
    ON distributions(family_id, created_at DESC);

CREATE INDEX idx_distribution_items_distribution
    ON distribution_items(distribution_id);

ALTER TABLE stock_movements
    ADD COLUMN previous_balance INTEGER,
    ADD COLUMN new_balance INTEGER,
    ADD COLUMN actor_username VARCHAR(100);

CREATE INDEX idx_stock_movements_product_created
    ON stock_movements(product_id, created_at DESC);

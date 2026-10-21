ALTER TABLE donations
    ADD COLUMN donation_type VARCHAR(20) NOT NULL DEFAULT 'FINANCEIRA',
    ADD COLUMN product_id BIGINT,
    ADD COLUMN quantity INTEGER,
    ADD COLUMN unit VARCHAR(50),
    ADD COLUMN observation TEXT,
    ADD COLUMN external_reference VARCHAR(100),
    ADD COLUMN confirmed_by VARCHAR(100);

ALTER TABLE donations
    ALTER COLUMN amount DROP NOT NULL,
    ADD CONSTRAINT fk_donations_product FOREIGN KEY (product_id) REFERENCES products(id);

CREATE UNIQUE INDEX uq_donations_external_reference
    ON donations(external_reference)
    WHERE external_reference IS NOT NULL;

CREATE INDEX idx_donations_type_status_created
    ON donations(donation_type, status, created_at DESC);

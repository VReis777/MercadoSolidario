ALTER TABLE donations ADD COLUMN donation_type VARCHAR(20) NOT NULL DEFAULT 'FINANCEIRA';
ALTER TABLE donations ADD COLUMN product_id BIGINT;
ALTER TABLE donations ADD COLUMN quantity INTEGER;
ALTER TABLE donations ADD COLUMN unit VARCHAR(50);
ALTER TABLE donations ADD COLUMN observation TEXT;
ALTER TABLE donations ADD COLUMN external_reference VARCHAR(100);
ALTER TABLE donations ADD COLUMN confirmed_by VARCHAR(100);

ALTER TABLE donations ALTER COLUMN amount DROP NOT NULL;
ALTER TABLE donations
    ADD CONSTRAINT fk_donations_product FOREIGN KEY (product_id) REFERENCES products(id);

CREATE UNIQUE INDEX uq_donations_external_reference
    ON donations(external_reference);

CREATE INDEX idx_donations_type_status_created
    ON donations(donation_type, status, created_at DESC);

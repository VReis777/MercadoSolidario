ALTER TABLE products ADD COLUMN basket_quantity INTEGER;
ALTER TABLE products ADD CONSTRAINT chk_products_basket_quantity CHECK (basket_quantity IS NULL OR basket_quantity >= 0);

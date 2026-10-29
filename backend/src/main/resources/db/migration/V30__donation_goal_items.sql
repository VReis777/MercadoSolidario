-- Lista pública de itens necessários para a meta de 500 famílias (2026).
-- per_family_quantity = quantidade por cesta; package_size = apresentação típica
-- (ex.: arroz em pacotes de 5kg). product_id liga o item ao produto do estoque
-- quando existir (preenchido depois pelo admin ao receber a doação).

CREATE TABLE donation_goal_items (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    per_family_quantity NUMERIC(10,2) NOT NULL,
    package_size NUMERIC(10,2) NOT NULL,
    product_id BIGINT REFERENCES products(id),
    sort_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO donation_goal_items (name, unit, per_family_quantity, package_size, sort_order) VALUES
    ('Arroz', 'kg', 5, 5, 1),
    ('Feijão', 'kg', 4, 4, 2),
    ('Açúcar', 'kg', 4, 4, 3),
    ('Sal', 'kg', 1, 1, 4),
    ('Óleo de soja', 'L', 3, 3, 5),
    ('Café', 'kg', 1, 1, 6),
    ('Leite', 'L', 4, 4, 7),
    ('Farinha de trigo', 'kg', 2, 2, 8),
    ('Macarrão', 'kg', 2, 2, 9),
    ('Molho/extrato de tomate', 'kg', 1, 1, 10);

ALTER TABLE donations
    ADD COLUMN donation_goal_item_id BIGINT REFERENCES donation_goal_items(id);

ALTER TABLE donations
    ADD COLUMN donor_contact VARCHAR(160);

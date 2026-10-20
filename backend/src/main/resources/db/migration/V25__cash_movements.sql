-- Caixa/transparência: entradas e saídas de valores do projeto,
-- com comprovante anexado (PDF/JPG/PNG) guardado no próprio banco.

CREATE TABLE IF NOT EXISTS cash_movements (
    id BIGSERIAL PRIMARY KEY,
    movement_type VARCHAR(10) NOT NULL CHECK (movement_type IN ('ENTRADA', 'SAIDA')),
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    description TEXT NOT NULL,
    attachment_name VARCHAR(255),
    attachment_content_type VARCHAR(100),
    attachment_data BYTEA,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

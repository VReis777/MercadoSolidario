-- Ajuste manual de prioridade: a equipe pode mover uma família na lista
-- quando a situação dela muda (desemprego recente, melhora de renda etc.).

ALTER TABLE families ADD COLUMN IF NOT EXISTS priority_adjustment NUMERIC(12, 2) NOT NULL DEFAULT 0;
ALTER TABLE families ADD COLUMN IF NOT EXISTS priority_adjustment_note TEXT;
ALTER TABLE families ADD COLUMN IF NOT EXISTS priority_adjusted_by VARCHAR(100);
ALTER TABLE families ADD COLUMN IF NOT EXISTS priority_adjusted_at TIMESTAMP;

-- Inscrições importadas de bancos anteriores não pagaram a taxa de cartão
-- de R$7,00 — amountDue() não deve somá-la para elas.
ALTER TABLE run_registrations ADD COLUMN legacy BOOLEAN NOT NULL DEFAULT FALSE;

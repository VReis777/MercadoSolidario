-- QR de verificação da inscrição paga (retirada do kit):
-- checkin_code identifica a inscrição no QR; kit_delivered_at marca
-- a retirada para impedir duplicidade.
ALTER TABLE run_registrations
    ADD COLUMN checkin_code VARCHAR(40);

ALTER TABLE run_registrations
    ADD COLUMN kit_delivered_at TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS uq_run_registrations_checkin
    ON run_registrations (checkin_code);

-- Inscrições já pagas ganham código agora (determinístico e único;
-- as novas recebem UUID gerado na aplicação).
UPDATE run_registrations
    SET checkin_code = 'LEG-' || id
WHERE status = 'PAGA' AND checkin_code IS NULL;

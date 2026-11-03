-- Quem entregou o kit (usuário logado que confirmou a retirada),
-- para rastreabilidade sem "disse-me-disse".
ALTER TABLE run_registrations
    ADD COLUMN kit_delivered_by VARCHAR(160);

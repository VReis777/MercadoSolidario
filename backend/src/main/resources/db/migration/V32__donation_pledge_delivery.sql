-- self_delivery: true quando o doador promete levar a doação ao MEVAM
-- (contato opcional); false quando precisa combinar retirada (contato obrigatório).
ALTER TABLE donations
    ADD COLUMN self_delivery BOOLEAN NOT NULL DEFAULT FALSE;

-- access_key: segredo por inscricao exigido na consulta publica de status,
-- impedindo enumeracao por id sequencial.
ALTER TABLE run_registrations
    ADD COLUMN access_key VARCHAR(40);

CREATE UNIQUE INDEX idx_run_registrations_access_key
    ON run_registrations (access_key);

CREATE TABLE priority_configurations (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(150) NOT NULL,
    version_number INTEGER NOT NULL,
    explanation TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_priority_configuration_code_version UNIQUE (code, version_number)
);

CREATE UNIQUE INDEX uq_priority_configuration_single_active
    ON priority_configurations (active)
    WHERE active = TRUE;

CREATE TABLE priority_rules (
    id BIGSERIAL PRIMARY KEY,
    configuration_id BIGINT NOT NULL REFERENCES priority_configurations(id) ON DELETE RESTRICT,
    factor_code VARCHAR(50) NOT NULL,
    label VARCHAR(150) NOT NULL,
    operator VARCHAR(20) NOT NULL,
    reference_value VARCHAR(255) NOT NULL,
    weight DECIMAL(12, 2) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    explanation TEXT NOT NULL
);

CREATE INDEX idx_priority_rules_configuration ON priority_rules(configuration_id);

CREATE TABLE priority_calculation_history (
    id BIGSERIAL PRIMARY KEY,
    family_id BIGINT NOT NULL REFERENCES families(id) ON DELETE RESTRICT,
    configuration_id BIGINT NOT NULL REFERENCES priority_configurations(id) ON DELETE RESTRICT,
    score DECIMAL(12, 2) NOT NULL,
    priority_position INTEGER,
    calculation_version VARCHAR(100) NOT NULL,
    factor_snapshot TEXT NOT NULL,
    calculated_by VARCHAR(100) NOT NULL,
    calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_priority_history_family_date
    ON priority_calculation_history(family_id, calculated_at DESC);

CREATE TABLE welcome_availability (
    id BIGSERIAL PRIMARY KEY,
    available_date DATE NOT NULL UNIQUE,
    note VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE public_service_requests ADD COLUMN welcome_preference VARCHAR(10);
ALTER TABLE public_service_requests ADD COLUMN welcome_date DATE;
ALTER TABLE public_service_requests ADD COLUMN welcome_time VARCHAR(5);

ALTER TABLE public_service_requests
    ADD CONSTRAINT chk_welcome_preference
        CHECK (welcome_preference IS NULL OR welcome_preference IN ('VISIT', 'PHONE'));

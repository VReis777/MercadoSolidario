CREATE TABLE public_service_requests (
    id BIGSERIAL PRIMARY KEY,
    protocol_hash VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255),
    phone VARCHAR(30),
    email VARCHAR(254),
    preferred_contact VARCHAR(10),
    city VARCHAR(120) NOT NULL,
    neighborhood VARCHAR(120) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    help_type VARCHAR(100) NOT NULL,
    message VARCHAR(2000),
    consent BOOLEAN NOT NULL CHECK (consent = TRUE),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_service_request_contact_preference
        CHECK (preferred_contact IS NULL OR preferred_contact IN ('PHONE', 'EMAIL'))
);

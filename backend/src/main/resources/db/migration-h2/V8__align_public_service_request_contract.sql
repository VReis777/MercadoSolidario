ALTER TABLE public_service_requests ADD COLUMN location VARCHAR(255);
ALTER TABLE public_service_requests ADD COLUMN contact_value VARCHAR(254);

UPDATE public_service_requests
SET location = LEFT(city || ', ' || neighborhood, 255),
    contact_value = CASE
        WHEN preferred_contact = 'PHONE' THEN phone
        WHEN preferred_contact = 'EMAIL' THEN email
        ELSE COALESCE(phone, email)
    END;

ALTER TABLE public_service_requests ALTER COLUMN location SET NOT NULL;
ALTER TABLE public_service_requests RENAME COLUMN name TO full_name;
ALTER TABLE public_service_requests RENAME COLUMN preferred_contact TO contact_preference;
ALTER TABLE public_service_requests RENAME COLUMN quantity TO household_size;
ALTER TABLE public_service_requests RENAME COLUMN help_type TO assistance_type;
ALTER TABLE public_service_requests RENAME COLUMN consent TO consent_accepted;
ALTER TABLE public_service_requests ALTER COLUMN message SET DATA TYPE VARCHAR(1000);
ALTER TABLE public_service_requests DROP COLUMN city;
ALTER TABLE public_service_requests DROP COLUMN neighborhood;
ALTER TABLE public_service_requests DROP COLUMN phone;
ALTER TABLE public_service_requests DROP COLUMN email;

ALTER TABLE public_service_requests
    ADD CONSTRAINT chk_service_request_consent
        CHECK (consent_accepted = TRUE);

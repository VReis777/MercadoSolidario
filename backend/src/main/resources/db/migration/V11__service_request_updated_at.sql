ALTER TABLE public_service_requests
    ADD COLUMN updated_at TIMESTAMP;

UPDATE public_service_requests
SET updated_at = created_at;

ALTER TABLE public_service_requests
    ALTER COLUMN updated_at SET NOT NULL;

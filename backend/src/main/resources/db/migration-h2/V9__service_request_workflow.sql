ALTER TABLE public_service_requests ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'RECEBIDA';
ALTER TABLE public_service_requests ADD COLUMN assigned_user_id BIGINT;

ALTER TABLE public_service_requests
    ADD CONSTRAINT fk_service_request_assigned_user
        FOREIGN KEY (assigned_user_id) REFERENCES users(id);

CREATE TABLE public_service_request_history (
    id BIGSERIAL PRIMARY KEY,
    service_request_id BIGINT NOT NULL REFERENCES public_service_requests(id),
    action VARCHAR(20) NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    actor_username VARCHAR(100) NOT NULL,
    observation VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_service_request_history_request
    ON public_service_request_history(service_request_id, created_at, id);
CREATE INDEX idx_service_requests_status_created
    ON public_service_requests(status, created_at);

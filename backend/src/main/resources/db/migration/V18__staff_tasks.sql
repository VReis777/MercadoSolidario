CREATE TABLE staff_tasks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    assignee_id BIGINT NOT NULL REFERENCES users(id),
    created_by_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'ABERTA',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_staff_tasks_assignee ON staff_tasks(assignee_id);
CREATE INDEX idx_staff_tasks_status ON staff_tasks(status);

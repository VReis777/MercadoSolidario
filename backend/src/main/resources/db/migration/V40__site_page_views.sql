-- Contador simples de acessos por pagina publica (sem cookies/dados pessoais).
CREATE TABLE site_page_views (
    path VARCHAR(60) PRIMARY KEY,
    views BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- MERCADO SOLIDÁRIO - Initial Database Schema
-- ============================================

-- Roles Table
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Users Table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User-Role Junction Table
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Family Origin Enum
CREATE TYPE family_origin AS ENUM (
    'CADASTRO_PROPRIO',
    'INDICACAO',
    'IGREJA',
    'DIACONO',
    'PASTOR',
    'MEMBRO_IGREJA',
    'INSTITUICAO_PARCEIRA',
    'OUTRO'
);

-- Family Request Reason Enum
CREATE TYPE request_reason AS ENUM (
    'DESEMPREGO',
    'BAIXA_RENDA',
    'EMERGENCIA_FINANCEIRA',
    'DOENCA_FAMILIA',
    'PERDA_RENDA',
    'SITUACAO_HABITACIONAL',
    'FAMILIA_NUMEROSA',
    'OUTRO'
);

-- Family Status Enum
CREATE TYPE family_status AS ENUM (
    'CADASTRADA',
    'EM_ANALISE',
    'APROVADA',
    'CONTEMPLADA',
    'ATENDIDA',
    'INATIVA',
    'CANCELADA'
);

-- Families Table
CREATE TABLE families (
    id BIGSERIAL PRIMARY KEY,
    responsible_name VARCHAR(255) NOT NULL,
    cpf VARCHAR(14) NOT NULL UNIQUE,
    spouse_name VARCHAR(255),
    dependents INTEGER DEFAULT 0,
    household_size INTEGER NOT NULL,
    total_income DECIMAL(12, 2) NOT NULL,
    request_reason request_reason NOT NULL,
    origin family_origin NOT NULL,
    referrer_name VARCHAR(255),
    observations TEXT,
    status family_status DEFAULT 'CADASTRADA',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Family Members Table (for future expansion)
CREATE TABLE family_members (
    id BIGSERIAL PRIMARY KEY,
    family_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    cpf VARCHAR(14),
    birth_date DATE,
    relationship VARCHAR(100),
    observations TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
);

-- Family Priority Score Table
CREATE TABLE family_priority_scores (
    id BIGSERIAL PRIMARY KEY,
    family_id BIGINT NOT NULL UNIQUE,
    score DECIMAL(10, 2) NOT NULL,
    priority_position INTEGER,
    calculation_version VARCHAR(50) DEFAULT '1.0',
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
);

-- Products Table
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    unit VARCHAR(50) NOT NULL,
    current_quantity INTEGER DEFAULT 0,
    minimum_quantity INTEGER DEFAULT 0,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Stock Movement Type Enum
CREATE TYPE movement_type AS ENUM (
    'ENTRADA_DOACAO',
    'ENTRADA_COMPRA',
    'AJUSTE',
    'SAIDA_DISTRIBUICAO',
    'PERDA',
    'DEVOLUCAO'
);

-- Stock Movements Table
CREATE TABLE stock_movements (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    type movement_type NOT NULL,
    quantity INTEGER NOT NULL,
    user_id BIGINT,
    family_id BIGINT,
    reason TEXT,
    observation TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (family_id) REFERENCES families(id)
);

-- Sponsor Type Enum
CREATE TYPE sponsor_type AS ENUM (
    'PESSOA_FISICA',
    'EMPRESA',
    'INSTITUICAO',
    'ANONIMO'
);

-- Sponsors Table
CREATE TABLE sponsors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type sponsor_type NOT NULL,
    company_name VARCHAR(255),
    phone VARCHAR(20),
    whatsapp VARCHAR(20),
    email VARCHAR(255),
    observations TEXT,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Donation Status Enum
CREATE TYPE donation_status AS ENUM (
    'CRIADA',
    'AGUARDANDO_PAGAMENTO',
    'PAGA',
    'CANCELADA',
    'EXPIRADA',
    'CONFIRMADA_MANUALMENTE'
);

-- Donations Table
CREATE TABLE donations (
    id BIGSERIAL PRIMARY KEY,
    amount DECIMAL(12, 2) NOT NULL,
    donor_name VARCHAR(255),
    is_anonymous BOOLEAN DEFAULT false,
    status donation_status DEFAULT 'CRIADA',
    sponsor_id BIGINT,
    payment_method VARCHAR(50),
    pix_payload TEXT,
    confirmation_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sponsor_id) REFERENCES sponsors(id)
);

-- Distribution Table
CREATE TABLE distributions (
    id BIGSERIAL PRIMARY KEY,
    family_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    user_id BIGINT NOT NULL,
    distribution_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    observations TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (family_id) REFERENCES families(id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Audit Log Table
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    details TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- System Settings Table
CREATE TABLE system_settings (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    description VARCHAR(255),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Import Batch Table
CREATE TABLE import_batches (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    total_records INTEGER DEFAULT 0,
    imported_records INTEGER DEFAULT 0,
    ignored_records INTEGER DEFAULT 0,
    duplicate_records INTEGER DEFAULT 0,
    invalid_records INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'PROCESSING',
    user_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Import Errors Table
CREATE TABLE import_errors (
    id BIGSERIAL PRIMARY KEY,
    import_batch_id BIGINT NOT NULL,
    row_number INTEGER,
    error_message TEXT NOT NULL,
    data TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (import_batch_id) REFERENCES import_batches(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_families_status ON families(status);
CREATE INDEX idx_families_cpf ON families(cpf);
CREATE INDEX idx_families_origin ON families(origin);
CREATE INDEX idx_family_priority_score ON family_priority_scores(score DESC);
CREATE INDEX idx_stock_movements_product ON stock_movements(product_id);
CREATE INDEX idx_stock_movements_created ON stock_movements(created_at);
CREATE INDEX idx_donations_status ON donations(status);
CREATE INDEX idx_donations_created ON donations(created_at);
CREATE INDEX idx_distributions_family ON distributions(family_id);
CREATE INDEX idx_distributions_date ON distributions(distribution_date);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at);

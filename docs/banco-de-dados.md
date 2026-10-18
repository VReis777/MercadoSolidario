# Banco de Dados - Mercado Solidário

## Visão Geral

O sistema utiliza PostgreSQL 16 como banco de dados relacional.

## Estrutura das Tabelas

### users
Armazena usuários do sistema administrativo.

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | BIGSERIAL | PK |
| username | VARCHAR(100) | Nome de usuário (único) |
| password | VARCHAR(255) | Senha (BCrypt) |
| email | VARCHAR(255) | Email (único) |
| full_name | VARCHAR(255) | Nome completo |
| active | BOOLEAN | Status ativo/inativo |
| created_at | TIMESTAMP | Data de criação |
| updated_at | TIMESTAMP | Data de atualização |

### roles
Define perfis de acesso (ADMIN, RH_TESOURARIA, DIACONO, VISITANTE).

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | BIGSERIAL | PK |
| name | VARCHAR(50) | Nome do papel (único) |
| description | VARCHAR(255) | Descrição |
| created_at | TIMESTAMP | Data de criação |
| updated_at | TIMESTAMP | Data de atualização |

### user_roles
Tabela de junção entre usuários e roles (N:N).

### families
Armazena informações das famílias beneficiárias.

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | BIGSERIAL | PK |
| responsible_name | VARCHAR(255) | Nome do responsável |
| cpf | VARCHAR(14) | CPF (único) |
| spouse_name | VARCHAR(255) | Nome do cônjuge |
| dependents | INTEGER | Número de dependentes |
| household_size | INTEGER | Total de pessoas na residência |
| total_income | DECIMAL(12,2) | Renda total familiar |
| request_reason | ENUM | Motivo da solicitação |
| origin | ENUM | Origem do cadastro |
| referrer_name | VARCHAR(255) | Quem indicou |
| observations | TEXT | Observações |
| status | ENUM | Status da família |
| created_at | TIMESTAMP | Data de criação |
| updated_at | TIMESTAMP | Data de atualização |

**Enums:**
- `family_origin`: CADASTRO_PROPRIO, INDICACAO, IGREJA, DIACONO, PASTOR, MEMBRO_IGREJA, INSTITUICAO_PARCEIRA, OUTRO
- `request_reason`: DESEMPREGO, BAIXA_RENDA, EMERGENCIA_FINANCEIRA, DOENCA_FAMILIA, PERDA_RENDA, SITUACAO_HABITACIONAL, FAMILIA_NUMEROSA, OUTRO
- `family_status`: CADASTRADA, EM_ANALISE, APROVADA, CONTEMPLADA, ATENDIDA, INATIVA, CANCELADA

### family_members
Armazena membros individuais da família (para expansão futura).

### family_priority_scores
Armazena pontuação de prioridade calculada.

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | BIGSERIAL | PK |
| family_id | BIGINT | FK para families (único) |
| score | DECIMAL(10,2) | Pontuação calculada |
| priority_position | INTEGER | Posição no ranking |
| calculation_version | VARCHAR(50) | Versão da regra usada |
| calculated_at | TIMESTAMP | Data do cálculo |

### products
Produtos do estoque.

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | BIGSERIAL | PK |
| name | VARCHAR(255) | Nome do produto |
| category | VARCHAR(100) | Categoria |
| unit | VARCHAR(50) | Unidade (kg, un, cx, etc) |
| current_quantity | INTEGER | Quantidade atual |
| minimum_quantity | INTEGER | Quantidade mínima |
| active | BOOLEAN | Ativo/inativo |
| created_at | TIMESTAMP | Data de criação |
| updated_at | TIMESTAMP | Data de atualização |

### stock_movements
Histórico de movimentações de estoque.

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | BIGSERIAL | PK |
| product_id | BIGINT | FK para products |
| type | ENUM | Tipo de movimentação |
| quantity | INTEGER | Quantidade |
| user_id | BIGINT | FK para users |
| family_id | BIGINT | FK para families |
| reason | TEXT | Motivo |
| observation | TEXT | Observação |
| created_at | TIMESTAMP | Data da movimentação |

**Enum:**
- `movement_type`: ENTRADA_DOACAO, ENTRADA_COMPRA, AJUSTE, SAIDA_DISTRIBUICAO, PERDA, DEVOLUCAO

### sponsors
Patrocinadores do projeto.

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | BIGSERIAL | PK |
| name | VARCHAR(255) | Nome |
| type | ENUM | Tipo de patrocinador |
| company_name | VARCHAR(255) | Nome da empresa |
| phone | VARCHAR(20) | Telefone |
| whatsapp | VARCHAR(20) | WhatsApp |
| email | VARCHAR(255) | Email |
| observations | TEXT | Observações |
| active | BOOLEAN | Ativo/inativo |
| created_at | TIMESTAMP | Data de criação |
| updated_at | TIMESTAMP | Data de atualização |

**Enum:**
- `sponsor_type`: PESSOA_FISICA, EMPRESA, INSTITUICAO, ANONIMO

### donations
Doações financeiras e de produtos.

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | BIGSERIAL | PK |
| donation_type | VARCHAR(20) | FINANCEIRA ou PRODUTO |
| amount | DECIMAL(12,2), nullable | Valor da doação financeira |
| donor_name | VARCHAR(255) | Nome do doador |
| is_anonymous | BOOLEAN | Doação anônima |
| status | ENUM | Status |
| sponsor_id | BIGINT | FK para sponsors |
| product_id | BIGINT, nullable | FK para products em doações de produtos |
| quantity | INTEGER, nullable | Quantidade doada |
| unit | VARCHAR(50), nullable | Unidade copiada do produto |
| observation | TEXT, nullable | Observação operacional |
| external_reference | VARCHAR(100), nullable/unique | Chave idempotente da solicitação |
| payment_method | VARCHAR(50) | Método de pagamento |
| pix_payload | TEXT | Payload PIX |
| confirmation_date | TIMESTAMP | Data de confirmação |
| confirmed_by | VARCHAR(100), nullable | Usuário que confirmou manualmente ou registrou a doação de produto |
| created_at | TIMESTAMP | Data de criação |
| updated_at | TIMESTAMP | Data de atualização |

**Enum:**
- `donation_status`: CRIADA, AGUARDANDO_PAGAMENTO, PAGA, CANCELADA, EXPIRADA, CONFIRMADA_MANUALMENTE

Doações financeiras novas iniciam em `AGUARDANDO_PAGAMENTO`. O QR PIX nunca marca a doação como paga; confirmação financeira manual é restrita ao perfil ADMIN. Doações de produtos registram também uma movimentação de entrada no estoque, atomicamente.

### distributions
Registro de distribuições realizadas.

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | BIGSERIAL | PK |
| family_id | BIGINT | FK para families |
| product_id | BIGINT, nullable após V6 | Campo legado da distribuição de produto único |
| quantity | INTEGER, nullable após V6 | Quantidade legada |
| user_id | BIGINT | FK para users |
| distribution_date | TIMESTAMP | Data da distribuição |
| observations | TEXT | Observações |
| status | VARCHAR(20) | CRIADA, CONFIRMADA ou CANCELADA |
| created_by | VARCHAR(100) | Username responsável |
| completed_at | TIMESTAMP | Data/hora da confirmação |
| reference | VARCHAR(255) | Referência/comprovante operacional |
| idempotency_key | VARCHAR(100), unique | Impede distribuição duplicada |
| cancellation_reason | TEXT | Motivo do cancelamento sem retorno de estoque |
| created_at | TIMESTAMP | Data de criação |

### distribution_items
Itens individuais de uma distribuição confirmada.

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | BIGSERIAL | PK |
| distribution_id | BIGINT | FK restrita para distributions |
| product_id | BIGINT | FK restrita para products |
| quantity | INTEGER | Quantidade positiva entregue |
| unit | VARCHAR(50) | Unidade registrada no momento da entrega |
| stock_movement_id | BIGINT, nullable | Movimento SAIDA_DISTRIBUICAO associado |

A migration V6 preserva distribuições de produto único existentes como item histórico. Cancelar uma distribuição não devolve saldo automaticamente.

### audit_logs
Log de auditoria para rastreabilidade.

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | BIGSERIAL | PK |
| user_id | BIGINT | FK para users |
| action | VARCHAR(100) | Ação realizada |
| entity_type | VARCHAR(100) | Tipo de entidade |
| entity_id | BIGINT | ID da entidade |
| details | TEXT | Detalhes da ação |
| ip_address | VARCHAR(45) | IP do usuário |
| created_at | TIMESTAMP | Data do log |

### system_settings
Configurações do sistema.

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | BIGSERIAL | PK |
| key | VARCHAR(100) | Chave (única) |
| value | TEXT | Valor |
| description | VARCHAR(255) | Descrição |
| updated_at | TIMESTAMP | Data de atualização |

**Chaves comuns:**
- `PIX_KEY` - Chave PIX oficial (acesso administrativo restrito; nunca incluída em auditoria ou resposta de leitura)
- `PIX_MERCHANT_NAME` - Nome do recebedor PIX
- `PIX_MERCHANT_CITY` - Cidade do recebedor PIX
- `WHATSAPP_CONTACT_URL` - URL de contato WhatsApp
- `SPONSORSHIP_QUOTA_VALUE` - Valor da cota de patrocínio
- `HOME_MESSAGE` - Mensagem da página inicial
- `MINIMUM_STOCK_LEVEL` - Nível mínimo de estoque

### import_batches
Controle de batches de importação Excel.

### import_errors
Erros encontrados durante importação.

## Índices

Índices foram criados para otimizar as consultas mais frequentes:
- `idx_families_status` - Filtragem por status
- `idx_families_cpf` - Busca por CPF
- `idx_family_priority_score` - Ordenação por pontuação
- `idx_stock_movements_product` - Movimentações por produto
- `idx_donations_status` - Filtragem de doações
- `idx_audit_logs_entity` - Auditoria por entidade

## Migrations

O sistema usa Flyway para versionamento do banco.

**Convenção de nomes:**
- `V{numero}__descricao.sql`
- Exemplo: `V1__initial_schema.sql`, `V2__add_family_phone.sql`

**Execução automática:**
As migrations são executadas automaticamente ao iniciar a aplicação Spring Boot.

## Backup

### Backup completo
```bash
docker exec mercado-solidario-db pg_dump -U postgres mercado_solidario > backup.sql
```

### Restauração
```bash
docker exec -i mercado-solidario-db psql -U postgres mercado_solidario < backup.sql
```

### Backup apenas dados
```bash
docker exec mercado-solidario-db pg_dump -U postgres --data-only mercado_solidario > data_backup.sql
```

## Manutenção

### Vaccum e Analyze
```bash
docker exec mercado-solidario-db psql -U postgres -c "VACUUM ANALYZE;" mercado_solidario
```

### Verificar tamanho das tabelas
```sql
SELECT
  schemaname,
  tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

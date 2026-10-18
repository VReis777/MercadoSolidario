# Arquitetura do Mercado Solidário

## Visão Geral

O Mercado Solidário é uma aplicação web dividida em três componentes principais:

1. **Backend** - API REST com Spring Boot
2. **Frontend** - SPA com React + TypeScript
3. **Banco de Dados** - PostgreSQL

## Componentes

### Backend

**Tecnologias:**
- Java 21 LTS
- Spring Boot 3.2.0
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL
- Flyway (migrations)
- Maven Wrapper

**Camadas da Aplicação:**
- `controller/` - Endpoints REST
- `service/` - Lógica de negócio
- `repository/` - Acesso a dados (JPA)
- `model/` - Entidades do banco
- `dto/` - Objetos de transferência
- `config/` - Configurações

**Módulos Principais:**
- `auth/` - Autenticação e autorização
- `family/` - Gestão de famílias
- `priority/` - Sistema de prioridade
- `stock/` - Gestão de estoque
- `donation/` - Doações
- `distribution/` - Distribuição
- `sponsor/` - Patrocinadores
- `report/` - Relatórios
- `import/` - Importação Excel
- `export/` - Exportação Excel
- `config/` - Configurações do sistema

### Frontend

**Tecnologias:**
- React 18
- TypeScript
- Vite
- React Router
- Material UI (MUI)
- Axios

**Estrutura:**
- `components/` - Componentes reutilizáveis
- `pages/` - Páginas da aplicação
- `services/` - Chamadas à API
- `contexts/` - Contextos globais (auth, etc)
- `types/` - Definições TypeScript
- `utils/` - Funções utilitárias
- `styles/` - Estilos globais

### Banco de Dados

**Tecnologia:** PostgreSQL 16

**Principais Tabelas:**
- `users` - Usuários do sistema
- `roles` - Perfis de acesso
- `families` - Famílias beneficiárias
- `family_members` - Membros da família
- `family_priority_scores` - Pontuação de prioridade
- `products` - Produtos do estoque
- `stock_movements` - Movimentações de estoque
- `donations` - Doações financeiras
- `sponsors` - Patrocinadores
- `distributions` - Distribuições realizadas
- `audit_logs` - Log de auditoria
- `system_settings` - Configurações do sistema
- `import_batches` - Batches de importação
- `import_errors` - Erros de importação

## Fluxo de Autenticação

1. Usuário faz login com usuário e senha
2. Backend valida credenciais
3. Backend gera token JWT
4. Frontend armazena token
5. Frontend envia token em cada requisição (header Authorization)
6. Backend valida token e verifica permissões

## Sistema de Prioridade

**Componente:** `PriorityCalculationService`

**Processo:**
1. Coleta dados da família
2. Aplica regras configuráveis
3. Calcula pontuação
4. Ordena famílias por pontuação
5. Armazena score e posição

**Fatores (futuros):**
- Renda familiar
- Quantidade de pessoas
- Dependentes
- Desemprego
- Situação emergencial
- Idosos
- Pessoas com deficiência
- Situação habitacional

## Sistema de Estoque

**Regra Importante:** Nunca alterar quantidade diretamente sem registrar movimentação.

**Fluxo:**
1. Produto cadastrado com quantidade inicial
2. Toda alteração gera `StockMovement`
3. Saldo atual = movimentações (ou materializado)
4. Tipo de movimentação: ENTRADA_DOACAO, ENTRADA_COMPRA, AJUSTE, SAIDA_DISTRIBUICAO, PERDA, DEVOLUCAO

## Doações

- Doações financeiras públicas são criadas em `AGUARDANDO_PAGAMENTO`; não existe confirmação bancária automática nesta fase.
- O PIX/BR Code e o QR são gerados localmente somente quando ADMIN configura a chave oficial, o nome e a cidade do recebedor. Gerar o QR não altera o estado financeiro.
- Somente ADMIN pode confirmar manualmente, cancelar ou expirar uma doação financeira pendente. RH_TESOURARIA pode consultar e registrar doações de produtos, mas não confirmar pagamentos.
- Uma doação de produto e sua movimentação `ENTRADA_DOACAO` são persistidas pela mesma transação via `StockService`; referências idempotentes evitam repetir a entrada em reenvios.
- O perfil DIACONO não acessa o módulo financeiro.

## Distribuição manual

- A entidade legada `Distribution` é o cabeçalho; `DistributionItem` representa cada produto. A migration V6 preserva as linhas antigas convertendo cada distribuição de produto único em um item histórico.
- ADMIN, RH_TESOURARIA e DIACONO podem consultar e registrar entregas manuais. Cancelar o registro é exclusivo do ADMIN e não devolve produtos ao estoque.
- A criação bloqueia os produtos em ordem crescente de ID, valida todos os saldos e, na mesma transação, salva cabeçalho/itens, registra `SAIDA_DISTRIBUICAO` via `StockService` e grava auditoria.
- `idempotency_key` tem índice único; repetição equivalente retorna o primeiro registro, e a restrição protege reenvios concorrentes.
- A distribuição não consulta pontuação, não seleciona automaticamente famílias e não altera o status familiar.

## Integrações Futuras

### IA Local
- `AIService` (interface)
- `NoOpAIService` (implementação padrão)
- `LocalGGUFAIService` (implementação futura)

### Kafka (opcional)
- Eventos: DonationCreated, StockEntry, FamilyRegistered, etc
- MVP funciona sem Kafka
- Preparado para uso futuro

### Pagamento Automático
- Arquitetura preparada para integração com PSP/banco
- Inicialmente: confirmação manual
- Futuro: webhook/websocket

## Segurança

- Senhas com BCrypt
- JWT com expiração
- CORS configurado
- Validação de entrada
- Logs sem dados sensíveis
- HTTPS em produção
- CPF mascarado na interface

## Escalabilidade

A arquitetura foi projetada para:
- Escalar horizontalmente (backend em múltiplos containers)
- Suportar carga crescente (cache, índices otimizados)
- Adicionar novos módulos sem impactar existentes
- Migrar para microservices se necessário

## Portabilidade

- Docker Compose para deploy completo
- Variáveis de ambiente para configuração
- Maven Wrapper (sem Maven global)
- npm para frontend
- Independentemente de OS específico

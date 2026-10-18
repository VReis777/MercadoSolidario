# MERCADO SOLIDÁRIO

**Natal com Dignidade para todas as famílias**

Sistema de gestão para projeto social de distribuição de benefícios, cadastramento de famílias, gerenciamento de doações, estoque e distribuição.

## 📋 Índice

- [Visão Geral](#visão-geral)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Pré-requisitos](#pré-requisitos)
- [Instalação Local](#instalação-local)
- [Execução com Docker](#execução-com-docker)
- [Configuração do Banco de Dados](#configuração-do-banco-de-dados)
- [Migrations](#migrations)
- [Criar Usuário Administrador](#criar-usuário-administrador)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Módulos](#módulos)
- [API Documentation](#api-documentation)
- [Backup e Restauração](#backup-e-restauração)
- [Deploy em Produção](#deploy-em-produção)
- [Segurança](#segurança)

## 🎯 Visão Geral

O Mercado Solidário é um sistema completo para gerenciar:

- Cadastro de famílias beneficiárias
- Motor técnico de prioridade configurável (desativado até aprovação explícita das regras do projeto)
- Gestão de doações financeiras e de produtos
- Controle de estoque em tempo real
- Distribuição de benefícios
- Importação/Exportação via Excel
- Dashboard administrativo
- Página pública para doações

## 🛠 Tecnologias

### Backend
- Java 21 LTS
- Spring Boot 3.2.0
- Spring Security com JWT
- Spring Data JPA
- PostgreSQL 16
- Flyway (migrations)
- Maven Wrapper
- SpringDoc OpenAPI (Swagger)

### Frontend
- React 18
- TypeScript
- Vite
- React Router
- Material UI (MUI)
- Axios

### Infraestrutura
- Docker & Docker Compose
- Nginx

## 🏗 Arquitetura

```
mercado-solidario/
├── backend/                 # API REST com Spring Boot
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   └── test/
│   ├── pom.xml
│   ├── Dockerfile
│   └── mvnw
├── frontend/                # React + TypeScript
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── services/
│   │   ├── contexts/
│   │   └── types/
│   ├── package.json
│   └── Dockerfile
├── database/
│   └── migrations/         # Migrations Flyway
├── docker-compose.yml
├── .env.example
└── docs/
```

## 📦 Pré-requisitos

### Para execução local:
- Java 21 ou superior
- Node.js 20 ou superior
- PostgreSQL 16 ou superior
- Maven (opcional, o projeto usa Maven Wrapper)

### Para Docker:
- Docker 20.10+
- Docker Compose 2.0+

## 🚀 Instalação Local

### 1. Clone o repositório

```bash
cd "C:\Users\Home\OneDrive\Área de Trabalho\Mercado Solidário"
```

### 2. Configurar variáveis de ambiente

```bash
cp .env.example .env
```

Edite o arquivo `.env` com suas configurações.

### 3. Configurar PostgreSQL

Crie o banco de dados:

```sql
CREATE DATABASE mercado_solidario;
```

### 4. Executar Backend

```bash
cd backend
./mvnw clean install
./mvnw spring-boot:run
```

O backend estará disponível em: `http://localhost:8080`

### 5. Executar Frontend

```bash
cd frontend
npm install
npm run dev
```

O frontend estará disponível em: `http://localhost:5173`

## 🐳 Execução com Docker

### 1. Configurar variáveis de ambiente

```bash
cp .env.example .env
```

### 2. Iniciar todos os serviços

```bash
docker-compose up -d
```

Isso iniciará:
- PostgreSQL (porta 5432)
- Backend (porta 8080)
- Frontend (porta 5173)

### 3. Verificar logs

```bash
docker-compose logs -f
```

### 4. Parar serviços

```bash
docker-compose down
```

## 💾 Configuração do Banco de Dados

O sistema usa PostgreSQL com as seguintes configurações padrão:

- Host: `localhost`
- Porta: `5432`
- Database: `mercado_solidario`
- User: `postgres`
- Password: `postgres`

**Importante:** Altere essas credenciais em produção.

## 🔄 Migrations

O sistema usa Flyway para gerenciamento de migrations. As migrations são executadas automaticamente ao iniciar a aplicação.

As migrations comuns ficam em `backend/src/main/resources/db/migration`; as específicas de
PostgreSQL ficam em `db/migration-postgresql` e as alternativas de teste H2 em `db/migration-h2`.
O caminho de produção continua usando o índice parcial PostgreSQL da V3. A variante H2 expressa
a mesma regra com coluna gerada e constraint única; índices únicos de referências opcionais usam
índices únicos sem predicado, equivalentes porque ambos os bancos permitem múltiplos valores NULL.
V12 cria a tabela de tokens de definição de senha, armazenando somente o hash do token.
O perfil `migration-test` aplica V1–V12 em H2, verifica schema e unicidade e executa `migrate()`
repetidamente. Essa validação H2 não substitui uma execução real em PostgreSQL.

### Criar nova migration

1. Crie um arquivo SQL em `backend/src/main/resources/db/migration/` com formato: `V{numero}__descricao.sql`
2. Exemplo: `V2__add_family_phone.sql`
3. Reinicie a aplicação

### Verificar status das migrations

```bash
cd backend
./mvnw flyway:info
```

## Pedido público de atendimento

O formulário público envia pedidos para `POST /api/public/service-requests`, sem autenticação.
O endpoint valida o consentimento e os dados informados, registra o pedido para análise e
retorna `201 Created` com um protocolo aleatório. O pedido não cria nem aprova uma família.
O protocolo é armazenado como hash e não há, nesta versão, endpoint público para consultá-lo.

Exemplo de corpo:

```json
{
  "fullName": "Pessoa solicitante",
  "contactValue": "(11) 99999-9999",
  "contactPreference": "PHONE",
  "location": "Cidade, bairro",
  "householdSize": 3,
  "assistanceType": "FOOD",
  "message": "Preciso de apoio com alimentos.",
  "consentAccepted": true
}
```

`contactPreference` aceita `PHONE` ou `EMAIL`; o contato deve corresponder à preferência.
`message` pode ter até 1.000 caracteres. A resposta contém somente `{ "protocol": "..." }`.
O protocolo aleatório é armazenado somente como hash. Para consultar o próprio pedido, envie
`POST /api/public/service-requests/lookup` com `{ "protocol": "...", "contactValue": "..." }`.
O contato deve corresponder ao informado na solicitação; ausência do pedido e fator incorreto
retornam a mesma resposta. A resposta contém apenas status, datas essenciais e contato mascarado.
As tentativas são limitadas por endereço de origem; consulte a seção de segurança para limites e
as considerações de proxy.

A área administrativa autenticada está disponível em `/service-requests`:

- `GET /api/service-requests` lista pedidos em páginas e aceita filtros `status`, `fromDate`,
  `toDate` e `protocol`. O protocolo é usado apenas para filtrar; nunca é retornado pela API.
- `GET /api/service-requests/{id}` consulta detalhes permitidos ao perfil.
- `PATCH /api/service-requests/{id}/status` aceita `{ "newStatus": "...", "observation": "..." }`.
- `POST /api/service-requests/{id}/notes` registra observação interna (ADMIN).
- `POST /api/service-requests/{id}/assign` atribui responsável (ADMIN/RH_TESOURARIA).
- `GET /api/service-requests/{id}/history` consulta o histórico com campos filtrados por perfil.

Os status são `RECEBIDA`, `EM_ANALISE`, `AGUARDANDO_CONTATO`, `ATENDIDA`, `ENCAMINHADA` e
`CANCELADA`. Transições são validadas no servidor e registradas com ator e data; RH_TESOURARIA
pode encaminhar somente pedidos em análise e DIACONO pode iniciar a triagem somente de pedidos
atribuídos ao próprio usuário. Diáconos recebem dados minimizados. Pedidos não criam nem aprovam
famílias automaticamente.

## 👤 Criar Usuário Administrador

O primeiro usuário ADMIN deve ser provisionado por um operador autorizado durante a implantação. Não há
cadastro público de administradores. Depois de entrar como ADMIN, use **Usuários** no painel para convidar
perfis RH/Tesouraria ou Diácono.

O convite exige `USER_MANAGEMENT_OWNER_USERNAME` com o e-mail de login do administrador responsável,
SMTP configurado por `SMTP_HOST` e `SMTP_FROM`, e `APP_FRONTEND_BASE_URL` com a URL real do frontend.
`SMTP_PORT` (padrão `587`), `SMTP_USERNAME` e `SMTP_PASSWORD` (devem ser informados juntos) e
`SMTP_STARTTLS` (padrão `true`) são opcionais conforme o provedor. Sem essas configurações, o endpoint
retorna erro e não cria a conta.

O convite envia um link aleatório de uso único, válido por 24 horas. O banco armazena somente o hash do
token; a senha é definida pelo próprio usuário e nunca é enviada por e-mail. O cadastro não permite criar
outro ADMIN, e o perfil Pastor não existe no conjunto atual de roles.

## 📁 Estrutura do Projeto

### Backend Modules

- `auth/` - Autenticação e autorização
- `family/` - Cadastro de famílias
- `priority/` - Sistema de prioridade
- `stock/` - Gestão de estoque
- `donation/` - Doações
- `distribution/` - Distribuição de benefícios
- `sponsor/` - Patrocinadores
- `report/` - Relatórios
- `import/` - Importação Excel
- `export/` - Exportação Excel
- `config/` - Configurações do sistema

### Frontend Pages

- `/` - Página pública
- `/login` - Login
- `/set-password` - Definição inicial de senha por link de convite
- `/admin/users` - Cadastro de pessoas por ADMIN
- `/dashboard` - Dashboard administrativo
- `/families` - Gestão de famílias
- `/priority` - Ranking de prioridade
- `/stock` - Gestão de estoque
- `/donate` - Doação financeira pública
- `/donations` - Administração de doações (ADMIN/RH_TESOURARIA)
- `/distributions` - Distribuição manual de produtos (ADMIN/RH_TESOURARIA/DIACONO)
- `/reports` - Relatórios
- `/settings` - Configurações

## 🎛 Módulos

### Dashboard e indicadores
- `GET /api/dashboard/summary` fornece agregados dos módulos existentes para ADMIN, RH_TESOURARIA e DIACONO; sem token responde 401 e outros perfis recebem 403
- O filtro aceita `from` e `to` no formato ISO `YYYY-MM-DD`, ambos obrigatórios em conjunto, com intervalo máximo de 366 dias; sem datas usa os últimos 30 dias
- ADMIN e RH_TESOURARIA recebem indicadores de doações e valores financeiros confirmados; DIACONO não recebe a seção `donations`
- Os demais indicadores são agregados de famílias, estoque e distribuições; nomes de famílias, CPF, chave PIX, dados de doadores e ranking de prioridade não são incluídos
- Consultas são somente leitura; o dashboard não altera registros de negócio. Prioridade não configurada é sinalizada sem pontuação ou ranking
- Alertas operacionais são agregados; o filtro de período limita indicadores, enquanto alertas de pendências mostram o estado atual

### Módulo de Famílias
- Cadastro de famílias
- Edição e consulta
- Importação/Exportação Excel
- Filtros avançados
- Controle de status

### Módulo de Prioridade
- Cálculo automático de pontuação
- Ranking de necessidade
- Fatores configuráveis
- Transparência nos critérios

### Módulo de Estoque
- Cadastro de produtos
- Movimentações (entrada/saída)
- Alertas de estoque baixo
- Histórico completo

### Módulo de Doações
- Doações financeiras iniciadas como `AGUARDANDO_PAGAMENTO`
- QR Code BR Code/PIX somente após configuração administrativa da chave oficial
- Confirmação manual de recebimento apenas por ADMIN; gerar/copiar PIX não confirma pagamento
- Doações de produtos integradas à entrada auditável no estoque na mesma transação
- `DONATIONS_MAXIMUM_AMOUNT` configura o limite técnico (padrão: `100000.00`)
- Não há integração bancária, webhook ou confirmação automática

### Módulo de Distribuição
- Registro manual de um ou mais produtos para família, sem usar score/prioridade como critério
- Saídas de estoque e histórico de itens persistidos atomicamente, com baixa via `StockService`
- Chave de idempotência única por distribuição; repetição igual retorna o registro original
- ADMIN, RH_TESOURARIA e DIACONO podem consultar e registrar distribuições; somente ADMIN pode cancelar o registro
- Cancelamento não restaura saldo automaticamente; retorno físico deve ser registrado separadamente

## 📚 API Documentation

A API é documentada com Swagger/OpenAPI.

Acesse: `http://localhost:8080/swagger-ui.html`

O endpoint público de consulta de solicitações (`POST /api/public/service-requests/lookup`) limita cada IP de origem a 10 tentativas por janela fixa de 15 minutos. Os limites podem ser ajustados por `service-requests.lookup.rate-limit`, `service-requests.lookup.window-seconds` e `service-requests.lookup.maximum-tracked-addresses` (padrão: 10.000 IPs acompanhados). O limitador é em memória, por instância, limitado em tamanho e não distribuído/persistente; ao atingir a capacidade, novas origens são recusadas até a expiração das janelas. Ele usa o endereço do socket (`remoteAddr`), não cabeçalhos encaminhados, portanto deve ser considerado ao configurar proxies/load balancers.

## 💾 Backup e Restauração

### Backup do PostgreSQL

```bash
docker exec mercado-solidario-db pg_dump -U postgres mercado_solidario > backup.sql
```

### Restauração

```bash
docker exec -i mercado-solidario-db psql -U postgres mercado_solidario < backup.sql
```

### Backup Automático

Recomenda-se configurar um cron job para backups automáticos diários.

## 🌐 Deploy em Produção

### Preparação

1. Altere todas as senhas e chaves no `.env`
2. Configure HTTPS
3. Configure domínio
4. Configure firewall
5. Ative backups automáticos

### Plataformas Sugeridas

- VPS barata (DigitalOcean, Linode, AWS Lightsail)
- PaaS (Heroku, Railway, Render)
- Servidor próprio

### Passos Gerais

1. Configure o domínio e DNS
2. Configure variáveis de ambiente
3. Configure HTTPS (Let's Encrypt)
4. Execute `docker-compose up -d`
5. Configure backups
6. Monitore os logs

## 🔒 Segurança

- Senhas com BCrypt
- JWT com expiração
- CORS configurado
- Proteção contra SQL Injection
- Validação de entrada
- Logs sem dados sensíveis
- CPF mascarado na interface
- HTTPS em produção
- Sem credenciais no código

## 📝 Licença

Este projeto é desenvolvido para uso social.

## 🤝 Contribuição

Este é um projeto social. Contribuições são bem-vindas.

## 📞 Suporte

Para dúvidas, consulte a documentação em `docs/`.

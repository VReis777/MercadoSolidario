# Deploy gratuito — Vercel + Render + Neon

Resultado:
- Site público: `https://mercadosolidario.vercel.app`
- API: `https://mercadosolidario-api.onrender.com`
- Banco PostgreSQL: Neon (plano gratuito, não expira)

Observação: o backend gratuito do Render "dorme" após ~15 min sem acesso e acorda em ~50s no primeiro acesso. Normal para o uso do projeto.

## 1) Banco — Neon (neon.tech)
1. Criar conta gratuita em https://neon.tech (login com GitHub).
2. Criar projeto `mercadosolidario`, região mais próxima (ex.: `sa-east-1`).
3. Copiar a connection string — formato `postgresql://usuario:senha@ep-xxxx.sa-east-1.aws.neon.tech/mercadosolidario?sslmode=require`.
4. Converter para JDBC (o backend espera este formato):
   `jdbc:postgresql://ep-xxxx.sa-east-1.aws.neon.tech/mercadosolidario?sslmode=require`

## 2) Backend — Render (render.com)
1. Criar conta gratuita em https://render.com com o GitHub.
2. New → Blueprint → apontar para o repositório `MercadoSolidario` (o arquivo `render.yaml` na raiz cria o serviço `mercadosolidario-api` automaticamente a partir do `backend/Dockerfile`).
3. Preencher as variáveis marcadas `sync: false`:
   - `DATABASE_URL` = JDBC do passo 1
   - `DATABASE_USERNAME` / `DATABASE_PASSWORD` = usuário e senha do Neon
   - `CORS_ALLOWED_ORIGINS` = `https://mercadosolidario.vercel.app`
   - `APP_FRONTEND_BASE_URL` = `https://mercadosolidario.vercel.app` (links de senha nos e-mails)
   - `INITIAL_ADMIN_EMAIL` = `h.reis777@gmail.com`, `INITIAL_ADMIN_NAME` = `Victor`
   - `INITIAL_ADMIN2_EMAIL` = `mevampetropolis@hotmail.com`, `INITIAL_ADMIN2_NAME` = `Pr. Filipe Molina`
   - `MAIL_USERNAME` = `mevammercadosolidario@gmail.com`
   - `MAIL_PASSWORD` = senha de app do Gmail
   - `MAIL_FROM` = `mevammercadosolidario@gmail.com`
   - `MAIL_NOTIFY` = `mevammercadosolidario@gmail.com`
4. Deploy. Flyway aplica as migrations sozinho no primeiro start.

## 3) Frontend — Vercel (vercel.com)
1. Criar conta gratuita com GitHub → Import Project → `MercadoSolidario`.
2. Root Directory: `frontend` (o `vercel.json` já tem o build e o rewrite do SPA).
3. Variável de ambiente: `VITE_API_URL` = `https://mercadosolidario-api.onrender.com`
4. Deploy → o site fica em `https://mercadosolidario.vercel.app`.

## 4) Teste final
1. Abrir `https://mercadosolidario.vercel.app/doacao` — chave PIX e QR devem funcionar.
2. Entrar em `/entrar` com o admin; primeiro login envia o link de senha por e-mail.
3. `/sou-empresa` e `/solicitar-atendimento` devem funcionar.

## Domínio próprio (opcional, pago)
Se o Pastor comprar `mercadosolidario.com.br` (~R$40/ano no Registro.br): apontar o frontend na Vercel (Domains → adicionar) e ajustar `CORS_ALLOWED_ORIGINS` e `APP_FRONTEND_BASE_URL` no Render para o domínio novo.

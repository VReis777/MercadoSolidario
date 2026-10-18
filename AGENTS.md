# Mercado Solidário — notas para agentes

## Verificação
- Backend (Windows, Git Bash): `cd backend && ./mvnw test` — testes usam H2 em memória (`application-test.properties`, ddl create-drop); `MigrationIntegrationTest` roda as migrations Flyway em H2 modo PostgreSQL e exige a lista `EXPECTED_VERSIONS`/`EXPECTED_SCRIPTS` atualizada a cada nova migration.
- Frontend: `cd frontend && npm.cmd run build` (tsc + vite). Dev: `npm.cmd run dev -- --host 127.0.0.1 --port 5173 --strictPort`.
- Respostas MockMvc com acentos: ler com `getContentAsString(StandardCharsets.UTF_8)`.

## Ambiente local
- PostgreSQL 16 instalado como serviço Windows `postgresql-x64-16` (porta 5432), banco `mercado_solidario`, usuário `mercado`. Credenciais em `%USERPROFILE%\.mercado-solidario\postgres.txt` (fora do repositório).
- Variáveis de usuário do Windows já definidas: `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `INITIAL_ADMIN_EMAIL`, `INITIAL_ADMIN_NAME`, `APP_FRONTEND_BASE_URL`. SMTP precisa de `MAIL_PASSWORD` (senha de app do Gmail) e `MAIL_ENABLED=true`.
- `mvnw.cmd` falha por causa do espaço em "Área de Trabalho": rodar `./mvnw` pelo Git Bash (ex.: `./mvnw spring-boot:run`).
- Rede local: IP do PC `192.168.18.76`. `frontend/.env.local` tem `VITE_API_URL=http://192.168.18.76:8080` e o dev server roda com `--host 0.0.0.0` (loopback continua funcionando). `CORS_ALLOWED_ORIGINS` (var. de usuário) inclui `http://192.168.18.76:5173`. `APP_FRONTEND_BASE_URL` aponta para `http://192.168.18.76:5173` — HTTP só é aceito em loopback ou rede privada (10.x, 192.168.x, 172.16-31.x). Para abrir no celular/outro PC pode ser preciso liberar as portas 5173 e 8080 no Firewall do Windows (exige admin).
- Segredos (JWT, SMTP `MAIL_PASSWORD`) só via variáveis de ambiente; nunca em arquivos versionados.
- Perfis novos nascem com a senha temporária (`app.users.temporary-password`) e `must_change_password=true`; o login com ela não emite JWT, só envia link de troca (30 min, uso único).

## Próxima tarefa (retomar aqui — dia seguinte)
- **Deploy gratuito** (3 passos, guia completo em `docs/deploy-gratis.md`): 1) Neon — criar banco e gerar JDBC `jdbc:postgresql://...?sslmode=require`; 2) Render — Blueprint com `render.yaml` da raiz (backend Docker), preencher env vars `sync: false` (DATABASE_*, JWT gerado, CORS=`https://mercadosolidario.vercel.app`, APP_FRONTEND_BASE_URL, INITIAL_ADMIN_*, MAIL_*); 3) Vercel — import repo, rootDir `frontend`, `VITE_API_URL=https://mercadosolidario-api.onrender.com`. URLs alvo: site `mercadosolidario.vercel.app`, API `mercadosolidario-api.onrender.com`.
- **Ajustes vindos dos testes do usuário pelo celular** (ele vai testar `http://192.168.18.76:5173` no celular e anotar o que ajustar — rede local já aberta, só falta liberar portas 5173/8080 no Firewall do Windows se não conectar).
- GitHub: repo já publicado em https://github.com/VReis777/MercadoSolidario (branch `main`). `.gitignore` exclui `*.xlsx`/`*.ods` (dados pessoais) e `.env`.
- Já feito e testado: contato mascarado só para não-atribuídos/não-admin; rotas PT com legados redirecionando; página `/sou-empresa` com WhatsApp configurável em Configurações; `crypto.randomUUID` tem fallback (`utils/idempotency.ts`) pois não existe em HTTP fora de localhost.

## Convenções
- Migrations comuns em `db/migration` devem ser compatíveis com PostgreSQL e H2 (modo PostgreSQL).
- Autorização dupla: regra de URL em `SecurityConfig` + `@PreAuthorize` no controller. Perfis: ADMIN, RH_TESOURARIA, DIACONO.
- Histórico de campanhas anteriores fica em `historical_beneficiaries` (separado de `families`); não inventar dados nem marcar contemplação sem evidência.
- axios: uploads precisam de `headers: { 'Content-Type': 'multipart/form-data' }` (a instância usa JSON por padrão).

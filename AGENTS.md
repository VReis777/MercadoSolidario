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

## Próxima tarefa (retomar aqui)
- Fila de solicitações: o contato real (telefone/e-mail) deve ficar **visível só para ADMIN e para o usuário atribuído** ao pedido. Hoje `ServiceRequestResponse.contactMasked` mascara para todos — mudar o mapeamento em `ServiceRequestAdministrationService.toResponse/toListResponse` para mostrar o contato completo quando `hasRole(ADMIN)` ou `assignedUser.username == authentication.getName()`; RH_TESOURARIA e DIACONO não atribuídos continuam vendo mascarado. Cuidar também do histórico/notas. Testar com os três perfis.

## Convenções
- Migrations comuns em `db/migration` devem ser compatíveis com PostgreSQL e H2 (modo PostgreSQL).
- Autorização dupla: regra de URL em `SecurityConfig` + `@PreAuthorize` no controller. Perfis: ADMIN, RH_TESOURARIA, DIACONO.
- Histórico de campanhas anteriores fica em `historical_beneficiaries` (separado de `families`); não inventar dados nem marcar contemplação sem evidência.
- axios: uploads precisam de `headers: { 'Content-Type': 'multipart/form-data' }` (a instância usa JSON por padrão).

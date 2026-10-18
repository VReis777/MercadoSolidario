# Deploy em Produção - Mercado Solidário

## Pré-requisitos

Antes de fazer o deploy em produção, certifique-se de:

1. Alterar todas as senhas e chaves no `.env`
2. Configurar domínio e DNS
3. Configurar HTTPS
4. Configurar firewall
5. Preparar estratégia de backup
6. Testar completamente em ambiente de staging

## Configuração de Variáveis de Ambiente

Crie o arquivo `.env` com as seguintes variáveis:

```bash
# Database
DATABASE_URL=jdbc:postgresql://postgres:5432/mercado_solidario
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=SENHA_FORTE_AQUI

# JWT
JWT_SECRET=CHAVE_SECRETA_MINIMO_256_BITS_MUITO_LONGA_E_ALEATORIA
JWT_EXPIRATION=86400000

# CORS (configure com seu domínio real)
CORS_ALLOWED_ORIGINS=https://seu-dominio.com
APP_FRONTEND_BASE_URL=https://seu-dominio.com
USER_MANAGEMENT_OWNER_USERNAME=admin@seu-dominio.com

# Convites de usuários (sem valores de exemplo para credenciais)
SMTP_HOST=
SMTP_PORT=587
SMTP_USERNAME=
SMTP_PASSWORD=
SMTP_FROM=
SMTP_STARTTLS=true

# Server
SERVER_PORT=8080

# Logging
LOG_LEVEL=INFO
SECURITY_LOG_LEVEL=WARN

# Swagger (desative em produção se desejar)
SWAGGER_ENABLED=false
```

Convites só são habilitados quando `SMTP_HOST`, `SMTP_FROM` e `APP_FRONTEND_BASE_URL` estão
configurados. Se o envio falhar, a transação de cadastro é revertida. O link de definição de senha
é individual, expira em 24 horas e pode ser usado uma única vez; não configure nem envie uma senha
temporária compartilhada.

## Opções de Hospedagem

### 1. VPS (Virtual Private Server)

**Plataformas recomendadas:**
- DigitalOcean (Droplet)
- Linode
- AWS Lightsail
- Hetzner
- Vultr

**Requisitos mínimos:**
- 2 vCPU
- 4GB RAM
- 40GB SSD
- Ubuntu 22.04 LTS ou similar

**Passos:**

1. Provisionar a VPS
2. Configurar domínio (DNS)
3. Instalar Docker e Docker Compose
4. Clonar o repositório
5. Configurar `.env`
6. Executar `docker-compose up -d`
7. Configurar HTTPS

### 2. PaaS (Platform as a Service)

**Opções:**
- Railway
- Render
- Heroku
- Northflank

**Considerações:**
- Plano gratuito pode ter limitações
- Banco de dados é separado
- Build automático
- Escalabilidade simplificada

### 3. Servidor Próprio

Se você tem um servidor físico:
- Instale Linux (Ubuntu 22.04 LTS recomendado)
- Configure firewall
- Instale Docker
- Siga os passos da VPS

## Configuração de Domínio

### 1. Registrar domínio
Use um registrador de domínios (GoDaddy, Namecheap, Registro.br, etc).

### 2. Configurar DNS
Aponte o domínio para o IP do seu servidor:

```
A     @           IP_DO_SEU_SERVIDOR
A     www         IP_DO_SEU_SERVIDOR
```

### 3. Configurar Nginx (opcional)
Se desejar usar Nginx como proxy reverso:

```nginx
server {
    listen 80;
    server_name seu-dominio.com www.seu-dominio.com;

    location / {
        proxy_pass http://localhost:5173;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## Configuração de HTTPS (Let's Encrypt)

### Usando Certbot

```bash
# Instalar Certbot
sudo apt update
sudo apt install certbot python3-certbot-nginx

# Obter certificado
sudo certbot --nginx -d seu-dominio.com -d www.seu-dominio.com

# Renovação automática é configurada automaticamente
```

### Renovação manual
```bash
sudo certbot renew
```

## Deploy com Docker

### 1. Preparar o servidor

```bash
# Atualizar sistema
sudo apt update && sudo apt upgrade -y

# Instalar Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Instalar Docker Compose
sudo apt install docker-compose -y

# Adicionar usuário ao grupo docker
sudo usermod -aG docker $USER
```

### 2. Clonar repositório

```bash
git clone https://github.com/seu-usuario/mercado-solidario.git
cd mercado-solidario
```

### 3. Configurar variáveis de ambiente

```bash
cp .env.example .env
nano .env
# Edite com suas configurações reais
```

### 4. Iniciar serviços

```bash
docker-compose up -d
```

### 5. Verificar status

```bash
docker-compose ps
docker-compose logs -f
```

## Estratégia de Backup

### Backup Automático (Cron)

Crie o script `/usr/local/bin/backup-mercado.sh`:

```bash
#!/bin/bash
BACKUP_DIR="/backups/mercado-solidario"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

# Backup do banco
docker exec mercado-solidario-db pg_dump -U postgres mercado_solidario > $BACKUP_DIR/backup_$DATE.sql

# Comprimir
gzip $BACKUP_DIR/backup_$DATE.sql

# Manter apenas últimos 7 dias
find $BACKUP_DIR -name "backup_*.sql.gz" -mtime +7 -delete
```

Adicionar ao crontab:
```bash
crontab -e
```

Adicionar linha para backup diário às 2h da manhã:
```
0 2 * * * /usr/local/bin/backup-mercado.sh
```

### Backup Off-site

Considere enviar backups para:
- AWS S3
- Google Cloud Storage
- Dropbox
- Outro servidor

## Monitoramento

### Logs

```bash
# Ver todos os logs
docker-compose logs

# Logs específicos
docker-compose logs backend
docker-compose logs frontend
docker-compose logs postgres

# Logs em tempo real
docker-compose logs -f
```

### Métricas

Considere usar:
- Prometheus + Grafana
- Datadog
- New Relic
- Uptime Robot (monitoramento de uptime)

## Atualização

### Atualizar aplicação

```bash
cd mercado-solidario
git pull
docker-compose down
docker-compose build
docker-compose up -d
```

### Rollback

Se algo der errado:

```bash
git checkout versao_anterior
docker-compose down
docker-compose build
docker-compose up -d
```

## Segurança Adicional

### Firewall (UFW)

```bash
# Habilitar firewall
sudo ufw enable

# Permitir SSH
sudo ufw allow 22

# Permitir HTTP
sudo ufw allow 80

# Permitir HTTPS
sudo ufw allow 443

# Bloquear outras portas (opcional)
sudo ufw deny 5432  # Não expor PostgreSQL diretamente
```

### Fail2Ban

```bash
sudo apt install fail2ban -y
```

### SSH Hardening

Edite `/etc/ssh/sshd_config`:
```
PermitRootLogin no
PasswordAuthentication no
```

## Checklist de Deploy

- [ ] Variáveis de ambiente configuradas
- [ ] Senhas fortes definidas
- [ ] Domínio configurado no DNS
- [ ] HTTPS configurado
- [ ] Firewall ativo
- [ ] Backup automático configurado
- [ ] Monitoramento configurado
- [ ] Logs centralizados
- [ ] Testes realizados em staging
- [ ] Plano de rollback documentado
- [ ] Documentação atualizada
- [ ] Equipe treinada

## Troubleshooting

### Container não inicia
```bash
docker-compose logs backend
# Verificar erro e corrigir
```

### Banco de dados não conecta
```bash
# Verificar se PostgreSQL está rodando
docker-compose ps postgres

# Verificar logs
docker-compose logs postgres
```

### 502 Bad Gateway
- Verificar se backend está rodando
- Verificar configuração do proxy reverso
- Verificar logs do Nginx

## Suporte

Para problemas técnicos:
1. Verificar logs: `docker-compose logs`
2. Consultar documentação em `docs/`
3. Verificar status dos serviços: `docker-compose ps`

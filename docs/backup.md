# Backup e Restauração - Mercado Solidário

## Visão Geral

Este documento descreve como fazer backup e restauração do sistema Mercado Solidário.

## Tipos de Backup

### 1. Backup do Banco de Dados

#### Backup Completo
```bash
docker exec mercado-solidario-db pg_dump -U postgres mercado_solidario > backup_completo_$(date +%Y%m%d).sql
```

#### Backup Apenas Dados
```bash
docker exec mercado-solidario-db pg_dump -U postgres --data-only mercado_solidario > backup_dados_$(date +%Y%m%d).sql
```

#### Backup Apenas Schema
```bash
docker exec mercado-solidario-db pg_dump -U postgres --schema-only mercado_solidario > backup_schema_$(date +%Y%m%d).sql
```

#### Backup Comprimido
```bash
docker exec mercado-solidario-db pg_dump -U postgres mercado_solidario | gzip > backup_completo_$(date +%Y%m%d).sql.gz
```

### 2. Backup de Arquivos

#### Backup do Repositório
```bash
tar -czf mercado-solidario-$(date +%Y%m%d).tar.gz /caminho/para/mercado-solidario
```

#### Backup de Arquivos de Upload (quando implementado)
```bash
tar -czf uploads-$(date +%Y%m%d).tar.gz /caminho/para/uploads
```

### 3. Backup de Configurações

```bash
cp .env .env.backup.$(date +%Y%m%d)
```

## Automatização de Backup

### Script de Backup Automático

Crie o arquivo `/usr/local/bin/backup-mercado.sh`:

```bash
#!/bin/bash

# Configurações
BACKUP_DIR="/backups/mercado-solidario"
PROJECT_DIR="/caminho/para/mercado-solidario"
RETENTION_DAYS=30
DATE=$(date +%Y%m%d_%H%M%S)

# Criar diretório de backup
mkdir -p $BACKUP_DIR

echo "[$(date)] Iniciando backup..."

# Backup do banco de dados
echo "[$(date)] Fazendo backup do banco de dados..."
docker exec mercado-solidario-db pg_dump -U postgres mercado_solidario | gzip > $BACKUP_DIR/database_$DATE.sql.gz

# Backup do repositório
echo "[$(date)] Fazendo backup do repositório..."
tar -czf $BACKUP_DIR/repository_$DATE.tar.gz $PROJECT_DIR

# Backup das configurações
echo "[$(date)] Fazendo backup das configurações..."
cp $PROJECT_DIR/.env $BACKUP_DIR/env_$DATE.backup

# Limpar backups antigos
echo "[$(date)] Limpando backups antigos..."
find $BACKUP_DIR -name "database_*.sql.gz" -mtime +$RETENTION_DAYS -delete
find $BACKUP_DIR -name "repository_*.tar.gz" -mtime +$RETENTION_DAYS -delete
find $BACKUP_DIR -name "env_*.backup" -mtime +$RETENTION_DAYS -delete

echo "[$(date)] Backup concluído com sucesso!"
```

Tornar executável:
```bash
chmod +x /usr/local/bin/backup-mercado.sh
```

### Configurar Cron

Para backup diário às 2h da manhã:
```bash
crontab -e
```

Adicionar:
```
0 2 * * * /usr/local/bin/backup-mercado.sh >> /var/log/backup-mercado.log 2>&1
```

## Restauração

### Restaurar Banco de Dados

#### Restaurar de arquivo SQL
```bash
# Parar containers
docker-compose down

# Iniciar apenas o banco
docker-compose up -d postgres

# Aguardar banco estar pronto
sleep 10

# Restaurar
docker exec -i mercado-solidario-db psql -U postgres mercado_solidario < backup_completo.sql

# Reiniciar todos os serviços
docker-compose up -d
```

#### Restaurar de arquivo comprimido
```bash
gunzip -c backup_completo.sql.gz | docker exec -i mercado-solidario-db psql -U postgres mercado_solidario
```

### Restaurar Configurações

```bash
cp .env.backup.20240101 .env
```

### Restaurar Repositório

```bash
tar -xzf mercado-solidario-20240101.tar.gz
```

## Backup Off-site

### Enviar para AWS S3

Instalar AWS CLI:
```bash
sudo apt install awscli -y
aws configure
```

Script de upload:
```bash
#!/bin/bash
BACKUP_FILE=$1
S3_BUCKET="s3://seu-bucket/mercado-solidario-backups"

aws s3 cp $BACKUP_FILE $S3_BUCKET/
```

### Enviar para Google Cloud Storage

Instalar gsutil:
```bash
curl https://sdk.cloud.google.com | bash
gcloud init
```

Upload:
```bash
gsutil cp backup_completo.sql.gz gs://seu-bucket/mercado-solidario-backups/
```

### Rsync para Outro Servidor

```bash
rsync -avz /backups/mercado-solidario/ usuario@outro-servidor:/backups/mercado-solidario/
```

## Backup Pontual (Antes de Mudanças Importantes)

Antes de:
- Atualizar o sistema
- Alterar schema do banco
- Mudar configurações críticas

Execute:
```bash
./backup-mercado.sh
```

## Verificação de Backup

### Testar Integridade do Backup

```bash
# Descomprimir
gunzip -t backup_completo.sql.gz

# Verificar conteúdo
zcat backup_completo.sql.gz | head
```

### Testar Restauração em Ambiente de Teste

Nunca teste restauração em produção! Use um ambiente separado.

## Política de Retenção

Recomendado:
- Diários: manter 7 dias
- Semanais: manter 4 semanas
- Mensais: manter 12 meses
- Anuais: manter por tempo indeterminado

## Monitoramento de Backup

### Verificar se backup foi executado

```bash
# Verificar último backup
ls -lt /backups/mercado-solidario/ | head

# Verificar tamanho do backup
du -sh /backups/mercado-solidario/
```

### Alertas

Configure alertas se:
- Backup falhar
- Backup não for executado
- Espaço em disco ficar baixo

## Recovery Time Objective (RTO) e Recovery Point Objective (RPO)

### RTO (Tempo de Recuperação)
- Meta: 2 horas
- Inclui: tempo de restaurar banco, reconfigurar, testar

### RPO (Ponto de Recuperação)
- Meta: 24 horas
- Máximo de dados perdidos aceitável

## Checklist de Backup

- [ ] Backup automático configurado
- [ ] Cron job ativo
- [ ] Backup off-site configurado
- [ ] Retenção definida
- [ ] Monitoramento configurado
- [ ] Restauração testada
- [ ] Documentação atualizada
- [ ] Equipe treinada

## Emergência

### Passo 1: Avaliar a Situação
- Identificar o problema
- Determinar escopo da perda
- Decidir se é necessário restaurar

### Passo 2: Selecionar Backup
- Escolher backup mais recente íntegro
- Verificar se contém dados necessários

### Passo 3: Restaurar
- Seguir procedimento de restauração
- Documentar o processo
- Testar funcionamento

### Passo 4: Investigar Causa
- Identificar por que ocorreu o problema
- Implementar medidas preventivas

### Passo 5: Comunicar
- Informar equipe
- Documentar incidente
- Criar plano de melhoria

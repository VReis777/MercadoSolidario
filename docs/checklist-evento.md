# Checklist pré-evento — Mercado Solidário Run

Data do evento: **19–20/10/2026** (kit pickup 12/12 — confirmar data real)

## Infraestrutura (planos free)

| Serviço | Plano | Limite | Ação |
|---|---|---|---|
| Vercel (site) | Hobby | 100 GB banda/mês | Sem ação — estável |
| Render (backend) | Free | **750 h/mês** de instância, dorme após 15 min parado, 512 MB RAM | UptimeRobot pinga a cada 5 min → mantém acordado. 1 instância 24/7 ≈ 744 h — **cabe, mas no limite**. Se passar de 1 serviço, estoura |
| Supabase (banco) | Free | **500 MB** de banco, 5 GB banda/mês, **pausa após ~7 dias sem uso** | **Maior risco** — mesmo modelo do Neon. Considerar upgrade Pro (~US$ 25/mês) antes do evento |
| Brevo (e-mails) | Free | **300 e-mails/dia** | Reduzido: QR pro inscrito + aviso pro e-mail principal. Folga suficiente |
| Mercado Pago | — | Sem limite relevante pro volume | — |

## Rotina semanal (a partir de agora)

- [ ] **Backup do banco**: `/configuracoes` → "Exportar banco completo (JSON)" — guardar o arquivo em local seguro (contém dados pessoais)
- [ ] Conferir `/inscritosrum`: pendentes, duplicatas, quem precisa de WhatsApp
- [ ] Conferir "Pagamentos no MP": algum pagamento sem dono?

## 1 semana antes

- [ ] Teste de carga leve: 5–10 inscrições de teste (usar "teste VICTOR", excluir depois)
- [ ] Verificar se Supabase não está pausado (abrir o painel)
- [ ] Conferir UptimeRobot ativo apontando para a API do Render
- [ ] Revisar lista de inscritos: telefones/WhatsApp completos para contato no dia
- [ ] Separar fichas de inscrição em papel (backup se o sistema cair)

## Dia do evento

- [ ] Manhã: abrir `/inscritosrum` e testar um check-in de QR
- [ ] Ter o backup JSON da véspera no celular/pen-drive
- [ ] Se o site demorar ~30s na 1ª abertura: é cold start do Render, normal — aguardar
- [ ] Pagamentos órfãos: ficam no e-mail h.reis777@gmail.com com dados do pagamento — vincular em "Baixar pagamentos"

## Se o Supabase pausar/cair

1. Painel Supabase → restaurar projeto (alguns minutos)
2. Dados continuam lá — não se perdem, só ficam inacessíveis temporariamente
3. Se pior: restaurar backup JSON (manual — me acionar)

## Contatos de emergência

- Alertas do sistema: h.reis777@gmail.com
- E-mail transacional: mevammercadosolidario@gmail.com

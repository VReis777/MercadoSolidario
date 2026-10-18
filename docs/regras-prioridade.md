# Motor de prioridade configurável

## Estado e salvaguardas

O motor é uma ferramenta técnica de apoio à revisão humana. Não concede, nega,
aprova, contempla ou atende benefícios, e não altera o status da família.

Não há pesos, limites, classificação social ou configuração ativa predefinidos.
O comportamento correto antes de uma configuração ativa é
`PRIORIDADE_NAO_CONFIGURADA`; a API não calcula nem ordena famílias nesse
estado. A documentação não estabelece critérios oficiais: fatores e parâmetros
precisam ser revisados e aprovados pelos responsáveis do projeto antes de uma
pessoa administradora ativar a versão correspondente.

Não é utilizado nenhum serviço ou modelo de IA.

## Campos técnicos disponíveis

O configurador permite selecionar somente campos já existentes em `Family`:

| Código | Valor lido | Operadores |
| --- | --- | --- |
| `TOTAL_INCOME` | renda total cadastrada | `EQ`, `NE`, `GT`, `GTE`, `LT`, `LTE` |
| `HOUSEHOLD_SIZE` | pessoas no domicílio | `EQ`, `NE`, `GT`, `GTE`, `LT`, `LTE` |
| `DEPENDENTS` | dependentes cadastrados | `EQ`, `NE`, `GT`, `GTE`, `LT`, `LTE` |
| `REQUEST_REASON` | código do motivo informado | `EQ`, `NE` |
| `ORIGIN` | código da origem do cadastro | `EQ`, `NE` |

Essa lista descreve campos técnicos, não recomenda seu uso como critério social.
Todos os fatores começam desabilitados. Cada contribuição é o peso configurado
quando a comparação configurada corresponde ao valor cadastrado; caso contrário,
a contribuição é zero. A soma é apenas uma pontuação técnica derivada da
configuração ativa. A interpretação e decisão final são humanas.

## Ciclo de configuração

Somente `ADMIN` pode consultar, criar, versionar, ativar ou desativar
configurações. Criar ou editar gera um registro de versão inativo; editar nunca
sobrescreve a versão anterior. A ativação é uma ação explícita, exige ao menos
um fator habilitado e desativa qualquer outra configuração ativa.

Uma única versão pode estar ativa. A migration `V3__priority_configuration_and_history.sql`
cria as tabelas de configuração, regras e histórico sem alterar as migrations
anteriores. A restrição no banco impede mais de uma configuração ativa.
`FamilyPriorityScore` e `family_priority_scores` são estruturas legadas: este
motor não as consulta nem grava nelas, pois não carregam a configuração nem a
explicação necessárias para validar resultados históricos.

## Cálculo e histórico

O cálculo do ranking é explícito (`POST /api/families/priority/recalculate`),
restrito a `ADMIN`. Consultas (`GET /api/families/priority` e
`GET /api/families/{id}/priority`) apenas leem resultados já calculados da
versão atualmente ativa; se ainda não houver cálculo, retornam
`CALCULO_PENDENTE`. `ADMIN` e `RH_TESOURARIA` podem consultar resultados;
`DIACONO` não recebe acesso a dados de pontuação nesta fase, pois essa política
de consulta não foi aprovada.

Empates recebem a mesma posição técnica (ranking de competição); a ordem visual
entre famílias empatadas não define desempate social. Os resultados persistem
com a versão, a data, o usuário e um snapshot dos fatores e contribuições.
Desativar ou criar nova versão não remove cálculos históricos. `ADMIN` pode
consultar os últimos 100 registros em `/api/priority/configuration/history`.

Auditoria registra criação, versionamento, ativação, desativação e recálculo.
Os detalhes de auditoria não incluem CPF, token ou dados familiares; o snapshot
histórico do cálculo contém somente os valores dos fatores efetivamente
habilitados e é acessível somente a `ADMIN` e `RH_TESOURARIA`.

## Endpoints

| Método e rota | Permissão | Efeito |
| --- | --- | --- |
| `GET /api/priority/configuration` | `ADMIN` | Lista versões |
| `POST /api/priority/configuration` | `ADMIN` | Cria versão 1 inativa |
| `PUT /api/priority/configuration/{id}` | `ADMIN` | Cria versão seguinte inativa |
| `POST /api/priority/configuration/{id}/activate` | `ADMIN` | Ativa versão explicitamente |
| `POST /api/priority/configuration/{id}/deactivate` | `ADMIN` | Desativa versão |
| `GET /api/priority/configuration/history` | `ADMIN` | Lê histórico preservado |
| `GET /api/families/priority` | `ADMIN`, `RH_TESOURARIA` | Lê ranking calculado |
| `GET /api/families/{id}/priority` | `ADMIN`, `RH_TESOURARIA` | Lê cálculo da família |
| `POST /api/families/priority/recalculate` | `ADMIN` | Calcula e audita ranking |

## Interface

O painel `/priority` é acessível a `ADMIN` e `RH_TESOURARIA`. `ADMIN` configura
versões e solicita recálculo; `RH_TESOURARIA` somente consulta resultados.
Quando não existe configuração ativa, o painel declara que a prioridade ainda
não foi configurada. Valores e contribuições são acompanhados das regras,
versões e explicações configuradas. A interface não altera status nem promete
benefício.

## Decisões ainda necessárias

Antes de ativar qualquer versão, a equipe responsável deve definir e documentar:

- quais fatores são adequados e proporcionais;
- operadores, valores de referência e pesos oficiais;
- revisão dos efeitos e de possíveis vieses;
- responsáveis e processo formal de aprovação;
- periodicidade de revisão e validade das regras;
- política de comunicação dos resultados às famílias;
- eventual necessidade de acesso de `DIACONO` aos resultados.

Até que essas decisões sejam aprovadas, manter o motor sem configuração ativa.

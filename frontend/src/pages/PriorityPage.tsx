import { FormEvent, useCallback, useEffect, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControl,
  Grid,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Toolbar,
  Typography,
  useMediaQuery,
} from '@mui/material'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import priorityService, {
  PriorityConfiguration,
  PriorityConfigurationRequest,
  PriorityFactorConfiguration,
  FamilyPriorityResult,
  PriorityRanking,
} from '../services/priorityService'
import familyService from '../services/familyService'
import { getApiErrorMessage } from '../utils/apiError'

const factorOptions = [
  { value: 'TOTAL_INCOME', label: 'Renda familiar (valor cadastrado)' },
  { value: 'EFFECTIVE_PER_CAPITA_INCOME', label: 'Renda por pessoa menos aluguel' },
  { value: 'NO_RENT_INCOME', label: 'Renda familiar sem pagar aluguel' },
  { value: 'NO_RENT_INCOME_SMALL', label: 'Renda sem aluguel em casa pequena (≤3 pessoas, sem pai/mãe solo)' },
  { value: 'SINGLE_PARENT_DEPENDENTS', label: 'Pai/mãe solo — nº de dependentes' },
  { value: 'PER_CAPITA_INCOME', label: 'Renda por pessoa (renda ÷ pessoas da família)' },
  { value: 'HOUSEHOLD_SIZE', label: 'Número de pessoas no domicílio' },
  { value: 'DEPENDENTS', label: 'Número de dependentes' },
  { value: 'REQUEST_REASON', label: 'Motivo informado no cadastro' },
  { value: 'ORIGIN', label: 'Origem do cadastro' },
]

const operatorLabels: Record<string, string> = {
  EQ: 'igual a',
  NE: 'diferente de',
  GT: 'maior que',
  GTE: 'maior ou igual a',
  LT: 'menor que',
  LTE: 'menor ou igual a',
}

const operatorLabel = (operator: string) => operatorLabels[operator] ?? operator

interface FactorDraft {
  factorCode: string
  label: string
  operator: string
  referenceValue: string
  weight: string
  enabled: boolean
  explanation: string
}

const emptyFactor = (): FactorDraft => ({
  factorCode: '',
  label: '',
  operator: 'EQ',
  referenceValue: '',
  weight: '',
  enabled: false,
  explanation: '',
})

function PriorityPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const fullScreenDialog = useMediaQuery('(max-width:599px)')
  const canManage = Boolean(user?.roles.some((role) => ['ROLE_ADMIN', 'ROLE_RH_TESOURARIA'].includes(role)))
  const [configurations, setConfigurations] = useState<PriorityConfiguration[]>([])
  const [ranking, setRanking] = useState<PriorityRanking | null>(null)
  const [familyNames, setFamilyNames] = useState<Record<number, string>>({})
  const [showDetails, setShowDetails] = useState(false)
  const [inspect, setInspect] = useState<FamilyPriorityResult | null>(null)
  const [dragId, setDragId] = useState<number | null>(null)
  const [moving, setMoving] = useState(false)
  const [history, setHistory] = useState<FamilyPriorityResult[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [formError, setFormError] = useState('')
  const [message, setMessage] = useState('')
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingConfiguration, setEditingConfiguration] = useState<PriorityConfiguration | null>(null)
  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [explanation, setExplanation] = useState('')
  const [factors, setFactors] = useState<FactorDraft[]>([emptyFactor()])
  const [recalculating, setRecalculating] = useState(false)

  const loadData = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [calculationResult, configurationResult, historyResult, familyResult] = await Promise.all([
        priorityService.getRanking(),
        canManage ? priorityService.listConfigurations() : Promise.resolve([]),
        canManage ? priorityService.getHistory() : Promise.resolve([]),
        familyService.search({ size: 500 }).catch(() => null),
      ])
      setRanking(calculationResult)
      setConfigurations(configurationResult)
      setHistory(historyResult)
      if (familyResult) {
        const names: Record<number, string> = {}
        for (const family of familyResult.content || []) names[family.id] = family.responsibleName
        setFamilyNames(names)
      }
    } catch (loadError) {
      setError(getApiErrorMessage(loadError, 'Não foi possível carregar as informações de prioridade.'))
    } finally {
      setLoading(false)
    }
  }, [canManage])

  useEffect(() => {
    void loadData()
  }, [loadData])

  const startNewConfiguration = (source?: PriorityConfiguration) => {
    setEditingConfiguration(source || null)
    setCode(source?.code || '')
    setName(source?.name || '')
    setExplanation(source?.explanation || '')
    setFactors(source
      ? source.factors.map((factor) => ({ ...factor, weight: String(factor.weight) }))
      : [emptyFactor()])
    setError('')
    setFormError('')
    setDialogOpen(true)
  }

  const updateFactor = (index: number, field: keyof FactorDraft, value: string | boolean) => {
    setFactors((current) => current.map((factor, currentIndex) =>
      currentIndex === index ? { ...factor, [field]: value } : factor
    ))
  }

  const submitConfiguration = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSaving(true)
    setError('')
    setFormError('')
    const request: PriorityConfigurationRequest = {
      code: code.trim(),
      name: name.trim(),
      explanation: explanation.trim(),
      factors: factors.map((factor): PriorityFactorConfiguration => ({
        ...factor,
        weight: Number(factor.weight),
      })),
    }
    try {
      if (editingConfiguration) {
        await priorityService.createVersion(editingConfiguration.id, request)
        setMessage('Nova versão criada inativa. Revise os fatores e ative-a explicitamente quando aprovada.')
      } else {
        await priorityService.createConfiguration(request)
        setMessage('Configuração criada inativa. Ela não será usada até ser ativada explicitamente.')
      }
      setDialogOpen(false)
      await loadData()
    } catch (saveError) {
      setFormError(getApiErrorMessage(saveError, 'Não foi possível salvar a configuração.'))
    } finally {
      setSaving(false)
    }
  }

  const activate = async (configuration: PriorityConfiguration) => {
    setError('')
    setMessage('')
    try {
      await priorityService.activateConfiguration(configuration.id)
      setMessage(`Versão ${configuration.code}-v${configuration.version} ativada. O resultado é técnico e não decide concessão de benefício.`)
      await loadData()
    } catch (activationError) {
      setError(getApiErrorMessage(activationError, 'Não foi possível ativar a configuração.'))
    }
  }

  const deactivate = async (configuration: PriorityConfiguration) => {
    setError('')
    setMessage('')
    try {
      await priorityService.deactivateConfiguration(configuration.id)
      setMessage('Configuração desativada. Resultados históricos permanecem preservados.')
      await loadData()
    } catch (deactivationError) {
      setError(getApiErrorMessage(deactivationError, 'Não foi possível desativar a configuração.'))
    }
  }

  const recalculateRanking = async () => {
    setRecalculating(true)
    setError('')
    setMessage('')
    try {
      const result = await priorityService.recalculateRanking()
      setRanking(result)
      setMessage('Cálculo registrado com a versão ativa. A análise e a decisão continuam humanas.')
      await loadData()
    } catch (calculationError) {
      setError(getApiErrorMessage(calculationError, 'Não foi possível calcular a prioridade.'))
    } finally {
      setRecalculating(false)
    }
  }

  const manualContribution = (result: FamilyPriorityResult) =>
    result.factors
      .filter((factor) => factor.factorCode === 'MANUAL_TEAM_ADJUSTMENT')
      .reduce((total, factor) => total + factor.contribution, 0)

  const moveFamily = async (familyId: number, targetIndex: number) => {
    if (!ranking || moving) return
    const others = ranking.families.filter((f) => f.familyId !== familyId)
    const currentIndex = ranking.families.findIndex((f) => f.familyId === familyId)
    if (currentIndex === targetIndex || currentIndex === targetIndex - 1) return
    const dragged = ranking.families.find((f) => f.familyId === familyId)
    if (!dragged || dragged.score == null) return

    const above = targetIndex > 0 ? others[targetIndex - 1] : null
    const below = targetIndex < others.length ? others[targetIndex] : null
    const aboveScore = above?.score ?? null
    const belowScore = below?.score ?? null
    if (aboveScore == null && belowScore == null) return

    const target = aboveScore != null && belowScore != null
      ? (aboveScore + belowScore) / 2
      : aboveScore != null ? aboveScore + 1 : (belowScore ?? 0) - 1
    const rawScore = dragged.score - manualContribution(dragged)
    const adjustment = Math.round((target - rawScore) * 100) / 100
    if (adjustment === manualContribution(dragged)) return

    setMoving(true)
    setError('')
    try {
      await priorityService.adjustPriority(familyId, adjustment,
        `Reposicionada manualmente na fila pela equipe (posição ${currentIndex + 1} → ${targetIndex + 1})`)
      await loadData()
      setMessage(`${familyNames[familyId] || 'Família'} reposicionada na fila. O ajuste ficou registrado na auditoria.`)
    } catch (moveError) {
      setError(getApiErrorMessage(moveError, 'Não foi possível reposicionar a família.'))
    } finally {
      setMoving(false)
      setDragId(null)
    }
  }

  const removeFactor = (index: number) => {
    setFactors((current) => current.filter((_, currentIndex) => currentIndex !== index))
  }

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default', pb: 5 }}>
      <Box sx={{ bgcolor: 'primary.main', color: 'white' }}>
        <Toolbar>
          <IconButton color="inherit" aria-label="Voltar ao painel" onClick={() => navigate('/modulos')}>
            <ArrowBackIcon />
          </IconButton>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>Prioridade configurável</Typography>
          <Chip label={canManage ? 'Equipe' : 'Consulta'} sx={{ color: 'white', bgcolor: 'rgba(255,255,255,0.18)' }} />
        </Toolbar>
      </Box>

      <Container maxWidth="lg" sx={{ mt: 3 }}>
        <Alert severity="warning" sx={{ mb: 2 }}>
          Esta ferramenta não define quem merece benefício e não altera o status das famílias. Toda análise e decisão permanecem humanas. Nenhuma regra social oficial está predefinida.
        </Alert>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        {message && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setMessage('')}>{message}</Alert>}

        {loading ? (
          <Box sx={{ py: 8, textAlign: 'center' }}><CircularProgress aria-label="Carregando prioridade" /></Box>
        ) : !ranking ? (
          <Alert severity="error">Não há resultado de prioridade disponível para exibir.</Alert>
        ) : ranking.status === 'PRIORIDADE_NAO_CONFIGURADA' ? (
          <Card>
            <CardContent>
              <Typography variant="h5" gutterBottom>Prioridade desativada</Typography>
              <Typography color="text.secondary">
                A prioridade ainda não foi configurada pelos responsáveis do projeto.
              </Typography>
              {canManage && (
                <Button sx={{ mt: 2 }} variant="contained" startIcon={<AddIcon />} onClick={() => startNewConfiguration()}>
                  Criar configuração inativa
                </Button>
              )}
            </CardContent>
          </Card>
        ) : ranking.status === 'CALCULO_PENDENTE' ? (
          <Card>
            <CardContent>
              <Typography variant="h5" gutterBottom>Configuração ativa sem cálculo registrado</Typography>
              <Typography color="text.secondary">
                Regra {ranking.calculationVersion}. Os resultados só serão calculados quando uma pessoa autorizada solicitar o recálculo.
              </Typography>
              {canManage && (
                <Button sx={{ mt: 2 }} variant="contained" onClick={() => void recalculateRanking()} disabled={recalculating}>
                  {recalculating ? 'Calculando...' : 'Calcular ranking técnico'}
                </Button>
              )}
            </CardContent>
          </Card>
        ) : ranking.status === 'RECALCULO_NECESSARIO' ? (
          <Card>
            <CardContent>
              <Typography variant="h5" gutterBottom>Atualização necessária</Typography>
              <Typography color="text.secondary">
                Há famílias novas ou alteradas desde o último cálculo. O ranking anterior não será apresentado como atual.
              </Typography>
              {canManage && (
                <Button sx={{ mt: 2 }} variant="contained" onClick={() => void recalculateRanking()} disabled={recalculating}>
                  {recalculating ? 'Calculando...' : 'Recalcular ranking técnico'}
                </Button>
              )}
            </CardContent>
          </Card>
        ) : (
          <>
            <Typography variant="h5" color="primary" gutterBottom>Prévia da fila de prioridade</Typography>
            <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={1} sx={{ mb: 2 }}>
              <Typography color="text.secondary">
                Regras {ranking.calculationVersion}. Quanto mais acima, maior a necessidade segundo os critérios — a decisão final continua sendo da equipe.
              </Typography>
              {canManage && ranking.families.length > 1 && (
                <Typography variant="body2" color="text.secondary">
                  Arraste uma família para cima ou para baixo se a equipe decidir — o ajuste fica registrado.
                </Typography>
              )}
            </Stack>
            {ranking.families.length === 0 ? (
              <Alert severity="info">Não há famílias para calcular nesta consulta.</Alert>
            ) : (
              <>
                <Card sx={{ mb: 2 }}>
                  <CardContent sx={{ '&:last-child': { pb: 2 } }}>
                    <Stack spacing={0.5} divider={<Divider flexItem />}>
                      {ranking.families.map((result, index) => {
                        const matched = result.factors
                          .filter((factor) => factor.matched && factor.contribution > 0)
                          .slice(0, 3)
                        const excluded = (result.score ?? 0) < 0
                        return (
                          <Stack key={result.familyId} direction={{ xs: 'column', sm: 'row' }}
                            alignItems={{ sm: 'center' }} gap={1}
                            draggable={canManage && !moving}
                            onDragStart={() => setDragId(result.familyId)}
                            onDragOver={(e) => canManage && e.preventDefault()}
                            onDrop={(e) => { e.preventDefault(); if (dragId != null) void moveFamily(dragId, index) }}
                            onDragEnd={() => setDragId(null)}
                            onClick={() => setInspect(result)}
                            sx={{
                              py: 0.75, cursor: 'pointer',
                              bgcolor: dragId === result.familyId ? 'action.selected' : 'transparent',
                              '&:hover': { bgcolor: 'action.hover' },
                            }}>
                            <Typography sx={{ minWidth: 44, fontWeight: 800, color: excluded ? 'error.main' : 'primary.main' }}>
                              #{result.position}
                            </Typography>
                            <Typography sx={{ flexGrow: 1, fontWeight: 600 }}>
                              {familyNames[result.familyId] || `Família #${result.familyId}`}
                            </Typography>
                            <Typography variant="body2" sx={{ minWidth: 86 }}>
                              {result.score?.toFixed(0)} pts
                            </Typography>
                            <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
                              {excluded
                                ? <Chip size="small" color="error" label="Avaliação da equipe" />
                                : matched.map((factor) => (
                                    <Chip key={factor.factorCode + factor.label} size="small" variant="outlined"
                                      label={factor.label} />
                                  ))}
                              {matched.length === 0 && !excluded && (
                                <Chip size="small" variant="outlined" label="Sem critério de prioridade" />
                              )}
                            </Stack>
                          </Stack>
                        )
                      })}
                    </Stack>
                  </CardContent>
                </Card>
                <Button variant="text" onClick={() => setShowDetails((v) => !v)} sx={{ mb: 2 }}>
                  {showDetails ? 'Ocultar explicação detalhada por família' : 'Ver explicação detalhada por família'}
                </Button>
              </>
            )}
            {showDetails && ranking.families.length > 0 && (
              <Grid container spacing={2}>
                {ranking.families.map((result) => (
                  <Grid item xs={12} md={6} key={result.familyId}>
                    <Card>
                      <CardContent>
                        <Stack direction="row" justifyContent="space-between" alignItems="center" gap={1}>
                          <Typography variant="h6">{familyNames[result.familyId] || `Família #${result.familyId}`}</Typography>
                          <Chip label={`Posição técnica ${result.position}`} color="primary" variant="outlined" />
                        </Stack>
                        <Typography variant="h4" sx={{ my: 1, fontWeight: 800 }}>
                          Pontuação: {result.score?.toFixed(2)}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          Regra {result.calculationVersion} · cálculo {result.calculatedAt ? new Date(result.calculatedAt).toLocaleString('pt-BR') : '-'}
                        </Typography>
                        <Divider sx={{ my: 1.5 }} />
                        {result.factors.map((factor) => (
                          <Box key={`${result.familyId}-${factor.factorCode}`} sx={{ mb: 1.5 }}>
                            <Typography fontWeight={700}>{factor.label}</Typography>
                            <Typography variant="body2">Valor cadastrado: {factor.inputValue}</Typography>
                            <Typography variant="body2">
                              Contribuição técnica: {factor.contribution.toFixed(2)} ({factor.matched ? 'regra atendida' : 'regra não atendida'})
                            </Typography>
                            <Typography variant="body2" color="text.secondary">{factor.explanation}</Typography>
                          </Box>
                        ))}
                        <Alert severity="info" sx={{ mt: 1 }}>
                          Resultado de apoio à revisão humana; não aprova nem garante benefício.
                        </Alert>
                      </CardContent>
                    </Card>
                  </Grid>
                ))}
              </Grid>
            )}
          </>
        )}

        {canManage && (
          <Box sx={{ mt: 4 }}>
            <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ xs: 'stretch', sm: 'center' }} gap={1} sx={{ mb: 2 }}>
              <Box>
                <Typography variant="h5" color="primary">Versões de configuração</Typography>
                <Typography variant="body2" color="text.secondary">Alterações criam versões novas e não apagam o histórico.</Typography>
              </Box>
              <Button variant="contained" startIcon={<AddIcon />} onClick={() => startNewConfiguration()}>
                Nova configuração
              </Button>
            </Stack>
            {configurations.length === 0 ? (
              <Alert severity="info">Nenhuma configuração criada. O motor permanece desativado.</Alert>
            ) : (
              <Grid container spacing={2}>
                {configurations.map((configuration) => (
                  <Grid item xs={12} md={6} key={configuration.id}>
                    <Card>
                      <CardContent>
                        <Stack direction="row" justifyContent="space-between" gap={1} alignItems="center">
                          <Typography variant="h6">{configuration.name}</Typography>
                          <Chip label={configuration.active ? 'ATIVA' : 'INATIVA'} color={configuration.active ? 'success' : 'default'} />
                        </Stack>
                        <Typography variant="body2">Versão {configuration.code}-v{configuration.version}</Typography>
                        <Typography variant="body2" color="text.secondary" sx={{ my: 1 }}>{configuration.explanation}</Typography>
                        <Typography variant="caption">Criada por {configuration.createdBy} · alterada por {configuration.updatedBy}</Typography>
                        <Divider sx={{ my: 1 }} />
                        {configuration.factors.map((factor) => (
                          <Typography variant="body2" key={`${configuration.id}-${factor.factorCode}`}>
                            {factor.label}: {operatorLabel(factor.operator)} {factor.referenceValue}; contribuição configurada {factor.weight} ({factor.enabled ? 'habilitado' : 'desabilitado'})
                          </Typography>
                        ))}
                        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ mt: 2 }}>
                          <Button fullWidth onClick={() => startNewConfiguration(configuration)}>Criar nova versão</Button>
                          {configuration.active ? (
                            <Button fullWidth color="warning" onClick={() => void deactivate(configuration)}>Desativar</Button>
                          ) : (
                            <Button fullWidth variant="contained" onClick={() => void activate(configuration)}>Ativar versão</Button>
                          )}
                        </Stack>
                      </CardContent>
                    </Card>
                  </Grid>
                ))}
              </Grid>
            )}
          </Box>
        )}
        {canManage && history.length > 0 && (
          <Box sx={{ mt: 4 }}>
            <Typography variant="h5" color="primary" gutterBottom>Histórico de cálculos</Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Resultados anteriores permanecem associados à versão utilizada, mesmo após desativação.
            </Typography>
            <Grid container spacing={2}>
              {history.map((result, index) => (
                <Grid item xs={12} md={6} key={`${result.familyId}-${result.calculationVersion}-${result.calculatedAt}-${index}`}>
                  <Card variant="outlined">
                    <CardContent>
                      <Typography fontWeight={700}>Família #{result.familyId} · {result.calculationVersion}</Typography>
                      <Typography>Pontuação técnica: {result.score?.toFixed(2)} · posição {result.position ?? 'não atribuída'}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        {result.calculatedAt ? new Date(result.calculatedAt).toLocaleString('pt-BR') : '-'}
                      </Typography>
                      {result.factors.map((factor) => (
                        <Box key={`${result.familyId}-${result.calculatedAt}-${factor.factorCode}`} sx={{ mt: 1 }}>
                          <Typography variant="body2">{factor.label}: {factor.inputValue} → {factor.contribution.toFixed(2)}</Typography>
                          <Typography variant="body2" color="text.secondary">{factor.explanation}</Typography>
                        </Box>
                      ))}
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>
          </Box>
        )}
      </Container>

      <Dialog open={inspect !== null} onClose={() => setInspect(null)} fullWidth maxWidth="sm">
        <DialogTitle>
          {inspect ? (familyNames[inspect.familyId] || `Família #${inspect.familyId}`) : ''}
        </DialogTitle>
        <DialogContent>
          {inspect && (
            <>
              <Typography variant="h5" fontWeight={800} sx={{ mb: 1 }}>
                #{inspect.position} · {inspect.score?.toFixed(0)} pontos
              </Typography>
              {(inspect.score ?? 0) < 0 && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  Marcada para avaliação da equipe — veja abaixo o critério que a colocou no fim da fila.
                </Alert>
              )}
              <Stack spacing={1.5}>
                {inspect.factors
                  .filter((factor) => factor.matched)
                  .sort((a, b) => Math.abs(b.contribution) - Math.abs(a.contribution))
                  .map((factor) => (
                    <Box key={factor.factorCode + factor.label}>
                      <Typography fontWeight={700}>
                        {factor.label} <Chip size="small" label={`${factor.contribution > 0 ? '+' : ''}${factor.contribution.toFixed(0)} pts`}
                          color={factor.contribution > 0 ? 'success' : 'error'} variant="outlined" />
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        {factor.explanation} (valor: {factor.inputValue})
                      </Typography>
                    </Box>
                  ))}
                {inspect.factors.every((f) => !f.matched) && (
                  <Alert severity="info">Nenhum critério de prioridade atendido.</Alert>
                )}
              </Stack>
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setInspect(null)}>Fechar</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={dialogOpen} onClose={() => !saving && setDialogOpen(false)} fullWidth maxWidth="md" fullScreen={fullScreenDialog}>
        <Box component="form" onSubmit={submitConfiguration}>
          <DialogTitle>{editingConfiguration ? 'Criar nova versão' : 'Nova configuração inativa'}</DialogTitle>
          <DialogContent>
            {formError && <Alert severity="error" sx={{ mb: 2 }}>{formError}</Alert>}
            <Alert severity="warning" sx={{ mb: 2 }}>
              Preencher estes campos não torna os fatores uma política oficial. Confirme critérios, pesos e explicações com as pessoas responsáveis antes de ativar.
            </Alert>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={4}>
                <TextField required fullWidth label="Código estável" value={code} onChange={(event) => setCode(event.target.value)} disabled={Boolean(editingConfiguration)} inputProps={{ maxLength: 80 }} />
              </Grid>
              <Grid item xs={12} sm={8}>
                <TextField required fullWidth label="Nome da configuração" value={name} onChange={(event) => setName(event.target.value)} inputProps={{ maxLength: 150 }} />
              </Grid>
              <Grid item xs={12}>
                <TextField required fullWidth multiline minRows={2} label="Explicação e finalidade" value={explanation} onChange={(event) => setExplanation(event.target.value)} inputProps={{ maxLength: 2000 }} />
              </Grid>
            </Grid>
            <Typography variant="h6" sx={{ mt: 3, mb: 1 }}>Fatores</Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Em cada fator, escolha o campo da família, a comparação (por exemplo, "menor ou igual a") e o valor de referência.
              A contribuição indica quanto esse fator pesa na pontuação de prioridade.
            </Typography>
            {factors.map((factor, index) => (
              <Card variant="outlined" key={index} sx={{ mb: 2, overflow: 'visible' }}>
                <CardContent>
                  <Grid container spacing={2} alignItems="center">
                    <Grid item xs={12} sm={6}>
                      <FormControl fullWidth required>
                        <InputLabel>Campo existente</InputLabel>
                        <Select
                          value={factor.factorCode}
                          label="Campo existente"
                          onChange={(event) => {
                            const selectedFactor = event.target.value
                            updateFactor(index, 'factorCode', selectedFactor)
                            if (selectedFactor === 'REQUEST_REASON' || selectedFactor === 'ORIGIN') {
                              updateFactor(index, 'operator', 'EQ')
                            }
                          }}
                        >
                          {factorOptions.map((option) => (
                            <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>
                          ))}
                        </Select>
                      </FormControl>
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <TextField required fullWidth label="Rótulo explicativo" value={factor.label} onChange={(event) => updateFactor(index, 'label', event.target.value)} />
                    </Grid>
                    <Grid item xs={12} sm={4}>
                      <FormControl fullWidth>
                        <InputLabel>Operador</InputLabel>
                        <Select
                          value={factor.operator}
                          label="Operador"
                          onChange={(event) => updateFactor(index, 'operator', event.target.value)}
                        >
                          {(factor.factorCode === 'REQUEST_REASON' || factor.factorCode === 'ORIGIN'
                            ? ['EQ', 'NE']
                            : ['EQ', 'NE', 'GT', 'GTE', 'LT', 'LTE']
                          ).map((operator) => <MenuItem key={operator} value={operator}>{operatorLabel(operator)}</MenuItem>)}
                        </Select>
                      </FormControl>
                    </Grid>
                    <Grid item xs={12} sm={4}>
                      <TextField required fullWidth label="Valor de referência" value={factor.referenceValue} onChange={(event) => updateFactor(index, 'referenceValue', event.target.value)} />
                    </Grid>
                    <Grid item xs={10} sm={3}>
                      <TextField required fullWidth type="number" label="Contribuição configurada" value={factor.weight} onChange={(event) => updateFactor(index, 'weight', event.target.value)} inputProps={{ step: '0.01' }} />
                    </Grid>
                    <Grid item xs={2} sm={1}>
                      <IconButton aria-label="Remover fator" color="error" onClick={() => removeFactor(index)} disabled={factors.length === 1}>
                        <DeleteIcon />
                      </IconButton>
                    </Grid>
                    <Grid item xs={12}>
                      <TextField required fullWidth multiline minRows={2} label="Explicação apresentada no resultado" value={factor.explanation} onChange={(event) => updateFactor(index, 'explanation', event.target.value)} inputProps={{ maxLength: 2000 }} />
                    </Grid>
                    <Grid item xs={12}>
                      <FormControl fullWidth>
                        <InputLabel>Estado do fator</InputLabel>
                        <Select value={factor.enabled ? 'enabled' : 'disabled'} label="Estado do fator" onChange={(event) => updateFactor(index, 'enabled', event.target.value === 'enabled')}>
                          <MenuItem value="disabled">Desabilitado</MenuItem>
                          <MenuItem value="enabled">Habilitado</MenuItem>
                        </Select>
                      </FormControl>
                    </Grid>
                  </Grid>
                </CardContent>
              </Card>
            ))}
            <Button startIcon={<AddIcon />} onClick={() => setFactors((current) => [...current, emptyFactor()])}>
              Adicionar fator
            </Button>
          </DialogContent>
          <DialogActions sx={{ p: 2, flexWrap: 'wrap', gap: 1 }}>
            <Button onClick={() => setDialogOpen(false)} disabled={saving}>Cancelar</Button>
            <Button type="submit" variant="contained" disabled={saving || factors.length === 0}>
              {saving ? 'Salvando...' : editingConfiguration ? 'Criar versão inativa' : 'Criar inativa'}
            </Button>
          </DialogActions>
        </Box>
      </Dialog>
    </Box>
  )
}

export default PriorityPage

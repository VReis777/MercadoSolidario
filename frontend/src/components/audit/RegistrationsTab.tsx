import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Alert, Box, Button, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle,
  FormControl, FormControlLabel, IconButton, InputLabel, MenuItem, Paper,
  Select, Stack, Switch, Table, TableBody, TableCell, TableContainer, TableHead, TablePagination, TableRow,
  TextField, Tooltip, Typography,
} from '@mui/material'
import UploadIcon from '@mui/icons-material/Upload'
import PriorityHighIcon from '@mui/icons-material/PriorityHigh'
import campaignService, { CampaignRegistration, Page } from '../../services/campaignService'
import priorityService, { FamilyPriorityResult } from '../../services/priorityService'
import { getApiErrorMessage } from '../../utils/apiError'
import RegistrationImportDialog from './RegistrationImportDialog'

export const familyStatusLabels: Record<string, string> = {
  CADASTRADA: 'Cadastrada',
  EM_ANALISE: 'Em análise',
  APROVADA: 'Aprovada',
  CONTEMPLADA: 'Contemplada',
  ATENDIDA: 'Atendida',
  INATIVA: 'Inativa',
  CANCELADA: 'Cancelada',
}

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

interface Props {
  campaignYear: number
  canImport?: boolean
  initialOnlyWithHistory?: boolean
  onChanged?: () => void
}

function RegistrationsTab({ campaignYear, canImport = false, initialOnlyWithHistory = false, onChanged }: Props) {
  const navigate = useNavigate()
  const [status, setStatus] = useState('')
  const [onlyWithHistory, setOnlyWithHistory] = useState(initialOnlyWithHistory)
  const [search, setSearch] = useState('')
  const [appliedSearch, setAppliedSearch] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(100)
  const [data, setData] = useState<Page<CampaignRegistration> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [importOpen, setImportOpen] = useState(false)
  const [reload, setReload] = useState(0)
  const [priorityByFamily, setPriorityByFamily] = useState<Map<number, FamilyPriorityResult>>(new Map())
  const [criteriaDialog, setCriteriaDialog] = useState<FamilyPriorityResult | null>(null)

  useEffect(() => {
    priorityService.getRanking()
      .then((ranking) => {
        if (ranking.status === 'CALCULADA') {
          setPriorityByFamily(new Map(ranking.families.map((f) => [f.familyId, f])))
        }
      })
      .catch(() => undefined)
  }, [reload])

  const sortedRows = useMemo(() => {
    if (!data) return []
    return [...data.content].sort((a, b) => {
      const pa = priorityByFamily.get(a.id)?.position ?? null
      const pb = priorityByFamily.get(b.id)?.position ?? null
      if (pa === null && pb === null) return 0
      if (pa === null) return 1
      if (pb === null) return -1
      return pa - pb
    })
  }, [data, priorityByFamily])

  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    campaignService.registrations({
      year: campaignYear, status: status || undefined, onlyWithHistory,
      search: appliedSearch || undefined, page, size,
    })
      .then((result) => { if (active) setData(result) })
      .catch((e) => { if (active) setError(getApiErrorMessage(e, 'Não foi possível carregar os cadastros.')) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [campaignYear, status, onlyWithHistory, appliedSearch, page, size, reload])

  return (
    <Stack spacing={2}>
      <Alert severity="info">
        Famílias cadastradas em {campaignYear}, conforme a data de criação do registro.
        Cadastro <strong>não</strong> significa aprovação nem contemplação: acompanhe a coluna Situação.
      </Alert>

      <Paper sx={{ p: 2 }}>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems={{ md: 'center' }}>
          <FormControl size="small" sx={{ minWidth: 170 }}>
            <InputLabel id="reg-status">Situação</InputLabel>
            <Select labelId="reg-status" label="Situação" value={status}
              onChange={(e) => { setStatus(e.target.value); setPage(0) }}>
              <MenuItem value="">Todas</MenuItem>
              {Object.entries(familyStatusLabels).map(([value, label]) => (
                <MenuItem key={value} value={value}>{label}</MenuItem>
              ))}
            </Select>
          </FormControl>
          <FormControlLabel
            control={<Switch checked={onlyWithHistory} onChange={(e) => { setOnlyWithHistory(e.target.checked); setPage(0) }} />}
            label="Só quem já foi contemplado antes"
          />
          <Box component="form" sx={{ display: 'flex', gap: 1, flexGrow: 1 }}
            onSubmit={(e) => { e.preventDefault(); setAppliedSearch(search.trim()); setPage(0) }}>
            <TextField size="small" fullWidth label="Buscar por nome ou CPF completo" value={search}
              onChange={(e) => setSearch(e.target.value)} />
            <Button type="submit" variant="outlined">Buscar</Button>
          </Box>
          {canImport && (
            <Button variant="outlined" startIcon={<UploadIcon />} onClick={() => setImportOpen(true)}>
              Importar cadastros
            </Button>
          )}
          <Button variant="contained" onClick={() => navigate('/familias')}>Gerenciar famílias</Button>
        </Stack>
      </Paper>

      {error && <Alert severity="error">{error}</Alert>}

      <Paper>
        {loading ? (
          <Box sx={{ textAlign: 'center', py: 6 }}><CircularProgress /></Box>
        ) : data && data.content.length === 0 ? (
          <Box sx={{ p: 3 }}>
            <Alert severity="info">Nenhuma família cadastrada em {campaignYear} com estes filtros.</Alert>
          </Box>
        ) : data ? (
          <>
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Responsável</TableCell>
                    <TableCell>CPF</TableCell>
                    <TableCell>Pessoas</TableCell>
                    <TableCell>Renda</TableCell>
                    <TableCell>Origem</TableCell>
                    <TableCell>Cadastro</TableCell>
                    <TableCell>Situação</TableCell>
                    <TableCell>Prioridade</TableCell>
                    <TableCell>Histórico</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {sortedRows.map((f) => {
                    const priority = priorityByFamily.get(f.id)
                    const hasHistory = f.historyMatch === 'CPF' || f.historyMatch === 'NOME'
                    return (
                    <TableRow key={f.id} hover
                      sx={hasHistory ? {
                        bgcolor: 'rgba(255, 196, 46, 0.18)',
                        '&:hover': { bgcolor: 'rgba(255, 196, 46, 0.3)' },
                        borderLeft: '4px solid #FFC42E',
                      } : undefined}>
                      <TableCell>
                        {f.responsibleName}
                        {f.referrerName && (
                          <Typography variant="caption" display="block" color="text.secondary">Indicação: {f.referrerName}</Typography>
                        )}
                      </TableCell>
                      <TableCell>{f.cpfMasked ?? '—'}</TableCell>
                      <TableCell>{f.householdSize ?? '—'}</TableCell>
                      <TableCell>{f.totalIncome != null ? currency.format(f.totalIncome) : '—'}</TableCell>
                      <TableCell>{f.origin ?? '—'}</TableCell>
                      <TableCell>{new Date(f.createdAt).toLocaleDateString('pt-BR')}</TableCell>
                      <TableCell>{f.status ? familyStatusLabels[f.status] ?? f.status : '—'}</TableCell>
                      <TableCell>
                        {priority?.position != null ? (
                          <Stack direction="row" spacing={0.5} alignItems="center">
                            <Chip size="small" color="secondary" label={`#${priority.position}`} />
                            {priority.score != null && Number(priority.score) <= -500 && (
                              <Tooltip title="Regra de exclusão atendida — avaliação da equipe obrigatória">
                                <Chip size="small" color="error" label="Revisão" />
                              </Tooltip>
                            )}
                            {priority.score != null && Number(priority.score) < 0 && Number(priority.score) > -500 && (
                              <Tooltip title="Acima da régua de renda — desce na lista e recebe marca de revisão">
                                <Chip size="small" color="warning" label="*" />
                              </Tooltip>
                            )}
                            <Tooltip title="Ver critérios atendidos">
                              <IconButton size="small" color="warning"
                                onClick={() => setCriteriaDialog(priority)}
                                aria-label={`Ver critérios de prioridade de ${f.responsibleName}`}>
                                <PriorityHighIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          </Stack>
                        ) : (
                          <Typography variant="caption" color="text.secondary">—</Typography>
                        )}
                      </TableCell>
                      <TableCell>
                        {f.historyMatch === 'CPF' && (
                          <Chip size="small" color="error" label={`Contemplada em ${f.historyYears.join(', ')} (CPF)`} />
                        )}
                        {f.historyMatch === 'NOME' && (
                          <Chip size="small" color="warning" variant="outlined"
                            label={`Possível: ${f.historyYears.join(', ')} (nome) — conferir`} />
                        )}
                        {f.historyMatch === 'NENHUM' && (
                          <Typography variant="caption" color="text.secondary">Sem registro anterior</Typography>
                        )}
                      </TableCell>
                    </TableRow>
                    )
                  })}
                </TableBody>
              </Table>
            </TableContainer>
            <TablePagination component="div" count={data.totalElements} page={page} rowsPerPage={size}
              rowsPerPageOptions={[20, 50, 100, 200]} labelRowsPerPage="Por página"
              labelDisplayedRows={({ from, to, count }) => `${from}–${to} de ${count}`}
              onPageChange={(_, p) => setPage(p)}
              onRowsPerPageChange={(e) => { setSize(Number(e.target.value)); setPage(0) }} />
          </>
        ) : null}
      </Paper>

      <RegistrationImportDialog open={importOpen} onClose={() => setImportOpen(false)}
        onImported={() => { setReload((r) => r + 1); onChanged?.() }} />

      <Dialog open={criteriaDialog !== null} onClose={() => setCriteriaDialog(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Critérios de prioridade atendidos</DialogTitle>
        <DialogContent dividers>
          {criteriaDialog && (
            <Stack spacing={1.5}>
              <Typography variant="body2" color="text.secondary">
                Pontuação total: <strong>{criteriaDialog.score}</strong> — posição #{criteriaDialog.position}
              </Typography>
              {criteriaDialog.factors.filter((factor) => factor.matched).length === 0 ? (
                <Alert severity="info">Nenhum critério atendido no momento.</Alert>
              ) : (
                criteriaDialog.factors.filter((factor) => factor.matched).map((factor) => (
                  <Paper key={factor.label} variant="outlined" sx={{ p: 1.5 }}>
                    <Stack direction="row" justifyContent="space-between" alignItems="center">
                      <Typography fontWeight={700} variant="body2">{factor.label}</Typography>
                      <Chip size="small" color={Number(factor.contribution) < 0 ? 'error' : 'secondary'}
                        label={`${Number(factor.contribution) > 0 ? '+' : ''}${factor.contribution}`} />
                    </Stack>
                    <Typography variant="caption" color="text.secondary">
                      {factor.explanation} (valor da família: {factor.inputValue})
                    </Typography>
                  </Paper>
                ))
              )}
            </Stack>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCriteriaDialog(null)}>Fechar</Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}

export default RegistrationsTab

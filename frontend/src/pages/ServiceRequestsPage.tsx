import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardActions,
  CardContent,
  Chip,
  CircularProgress,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Toolbar,
  Typography,
  useMediaQuery,
} from '@mui/material'
import { useTheme } from '@mui/material/styles'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import RefreshIcon from '@mui/icons-material/Refresh'
import VisibilityIcon from '@mui/icons-material/Visibility'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import managedServiceRequestService, {
  ManagedServiceRequest,
  ManagedServiceRequestDetails,
  ServiceRequestHistoryEntry,
  ServiceRequestStatus,
} from '../services/managedServiceRequestService'
import { getApiErrorMessage } from '../utils/apiError'
import ThemeToggle from '../components/ThemeToggle'

const statuses: ServiceRequestStatus[] = [
  'RECEBIDA',
  'EM_ANALISE',
  'AGUARDANDO_CONTATO',
  'ATENDIDA',
  'ENCAMINHADA',
  'CANCELADA',
]

const statusLabels: Record<ServiceRequestStatus, string> = {
  RECEBIDA: 'Recebida',
  EM_ANALISE: 'Em análise',
  AGUARDANDO_CONTATO: 'Aguardando contato',
  ATENDIDA: 'Atendida',
  ENCAMINHADA: 'Encaminhada',
  CANCELADA: 'Cancelada',
}

const assistanceLabels: Record<string, string> = {
  FOOD: 'Alimentação',
  HYGIENE: 'Higiene',
  OTHER: 'Outra necessidade',
}

interface SearchFilters {
  protocol: string
  status: ServiceRequestStatus | ''
  from: string
  to: string
}

const emptyFilters: SearchFilters = { protocol: '', status: '', from: '', to: '' }
const pageSize = 10

function formatDate(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? 'Data indisponível' : date.toLocaleString('pt-BR')
}

function ServiceRequestsPage() {
  const navigate = useNavigate()
  const theme = useTheme()
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'))
  const { user, hasAnyRole } = useAuth()
  const isAdmin = hasAnyRole(['ROLE_ADMIN'])
  const canForward = hasAnyRole(['ROLE_RH_TESOURARIA'])
  const isDeacon = hasAnyRole(['ROLE_DIACONO'])
  const canAssign = isAdmin || canForward
  const canReadSensitiveDetails = isAdmin || canForward
  const [filters, setFilters] = useState<SearchFilters>(emptyFilters)
  const [appliedFilters, setAppliedFilters] = useState<SearchFilters>(emptyFilters)
  const [requests, setRequests] = useState<ManagedServiceRequest[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [selected, setSelected] = useState<ManagedServiceRequestDetails | null>(null)
  const [detailTarget, setDetailTarget] = useState<ManagedServiceRequest | null>(null)
  const [history, setHistory] = useState<ServiceRequestHistoryEntry[]>([])
  const [detailsLoading, setDetailsLoading] = useState(false)
  const [detailsError, setDetailsError] = useState('')
  const [actionError, setActionError] = useState('')
  const [actionMessage, setActionMessage] = useState('')
  const [saving, setSaving] = useState(false)
  const [nextStatus, setNextStatus] = useState<ServiceRequestStatus | ''>('')
  const [statusObservation, setStatusObservation] = useState('')
  const [confirmStatusOpen, setConfirmStatusOpen] = useState(false)
  const [note, setNote] = useState('')
  const [username, setUsername] = useState('')

  const queryParams = useMemo(() => ({
    page,
    size: pageSize,
    ...(appliedFilters.status ? { status: appliedFilters.status } : {}),
    ...(appliedFilters.from ? { fromDate: appliedFilters.from } : {}),
    ...(appliedFilters.to ? { toDate: appliedFilters.to } : {}),
    ...(appliedFilters.protocol.trim() ? { protocol: appliedFilters.protocol.trim() } : {}),
  }), [page, appliedFilters])

  const loadRequests = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const result = await managedServiceRequestService.search(queryParams)
      setRequests(result.content)
      setTotalPages(result.totalPages)
      setTotalElements(result.totalElements)
    } catch (loadError) {
      setError(getApiErrorMessage(loadError, 'Não foi possível carregar as solicitações.'))
      setRequests([])
    } finally {
      setLoading(false)
    }
  }, [queryParams])

  useEffect(() => {
    void loadRequests()
  }, [loadRequests, refreshKey])

  const reloadSelected = useCallback(async (requestId: number) => {
    const [details, entries] = await Promise.all([
      managedServiceRequestService.getById(requestId),
      managedServiceRequestService.getHistory(requestId),
    ])
    setSelected(details)
    setHistory(entries)
  }, [])

  const openDetails = async (request: ManagedServiceRequest) => {
    setDetailsLoading(true)
    setDetailsError('')
    setActionError('')
    setActionMessage('')
    setSelected(null)
    setDetailTarget(request)
    try {
      await reloadSelected(request.id)
    } catch (detailError) {
      setDetailsError(getApiErrorMessage(detailError, 'Não foi possível carregar os detalhes da solicitação.'))
    } finally {
      setDetailsLoading(false)
    }
  }

  const applyFilters = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (filters.from && filters.to && filters.from > filters.to) {
      setError('A data inicial não pode ser posterior à data final.')
      return
    }
    setError('')
    setPage(0)
    setAppliedFilters({ ...filters })
  }

  const refresh = () => setRefreshKey((current) => current + 1)

  const allowedTransitions = (status: ServiceRequestStatus): ServiceRequestStatus[] => {
    if (isAdmin) {
      const transitions: Record<ServiceRequestStatus, ServiceRequestStatus[]> = {
        RECEBIDA: ['EM_ANALISE'],
        EM_ANALISE: ['AGUARDANDO_CONTATO', 'ENCAMINHADA', 'CANCELADA'],
        AGUARDANDO_CONTATO: ['ATENDIDA', 'ENCAMINHADA', 'CANCELADA'],
        ATENDIDA: [],
        ENCAMINHADA: ['ATENDIDA', 'CANCELADA'],
        CANCELADA: [],
      }
      return transitions[status]
    }
    if (canForward && status === 'EM_ANALISE') return ['ENCAMINHADA']
    if (isDeacon && status === 'RECEBIDA') return ['EM_ANALISE']
    return []
  }

  const updateStatus = async () => {
    if (!selected || !nextStatus) return
    setSaving(true)
    setActionError('')
    setActionMessage('')
    try {
      await managedServiceRequestService.updateStatus(selected.id, {
        newStatus: nextStatus,
        ...(statusObservation.trim() ? { observation: statusObservation.trim() } : {}),
      })
      setConfirmStatusOpen(false)
      setStatusObservation('')
      setNextStatus('')
      setActionMessage('Status atualizado. Isso não representa aprovação automática de benefício.')
      await Promise.all([reloadSelected(selected.id), loadRequests()])
    } catch (statusError) {
      setActionError(getApiErrorMessage(statusError, 'Não foi possível atualizar o status.'))
    } finally {
      setSaving(false)
    }
  }

  const addNote = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!selected || !note.trim()) return
    setSaving(true)
    setActionError('')
    setActionMessage('')
    try {
      await managedServiceRequestService.addNote(selected.id, { observation: note.trim() })
      setNote('')
      setActionMessage('Nota interna registrada.')
      await reloadSelected(selected.id)
    } catch (noteError) {
      setActionError(getApiErrorMessage(noteError, 'Não foi possível registrar a nota.'))
    } finally {
      setSaving(false)
    }
  }

  const assignRequest = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!selected || !username.trim()) return
    setSaving(true)
    setActionError('')
    setActionMessage('')
    try {
      await managedServiceRequestService.assign(selected.id, { username: username.trim() })
      setUsername('')
      setActionMessage('Responsável atualizado.')
      await Promise.all([reloadSelected(selected.id), loadRequests()])
    } catch (assignError) {
      setActionError(getApiErrorMessage(assignError, 'Não foi possível atribuir a solicitação.'))
    } finally {
      setSaving(false)
    }
  }

  const renderStatus = (status: ServiceRequestStatus) => (
    <Chip size="small" label={statusLabels[status] ?? status} color={
      status === 'CANCELADA' ? 'error'
        : status === 'ATENDIDA' ? 'success'
          : status === 'ENCAMINHADA' ? 'info'
            : status === 'EM_ANALISE' || status === 'AGUARDANDO_CONTATO' ? 'warning'
              : 'default'
    } />
  )

  const renderDetailsContent = () => {
    if (detailsLoading) {
      return <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}><CircularProgress /></Box>
    }
    if (detailsError) {
      return <Alert severity="error" action={
        <Button color="inherit" size="small" onClick={() => detailTarget && void openDetails(detailTarget)}>Tentar novamente</Button>
      }>{detailsError}</Alert>
    }
    if (!selected) return null

    const transitions = allowedTransitions(selected.status)
    return (
      <Stack spacing={2} sx={{ minWidth: 0 }}>
        <Alert severity="info">
          Esta solicitação registra um pedido de assistência e não significa que algum benefício foi aprovado.
        </Alert>
        {actionError && <Alert severity="error">{actionError}</Alert>}
        {actionMessage && <Alert severity="success">{actionMessage}</Alert>}

        <Paper variant="outlined" sx={{ p: 2, minWidth: 0 }}>
          <Stack spacing={1}>
            <Typography variant="h6">Solicitação #{selected.id}</Typography>
            <Box>{renderStatus(selected.status)}</Box>
            {canReadSensitiveDetails && <Typography><strong>Nome:</strong> {selected.fullName}</Typography>}
            <Typography><strong>Contato:</strong> {selected.contact}</Typography>
            {canReadSensitiveDetails && <Typography>
              <strong>Preferência de contato:</strong> {selected.contactPreference === 'EMAIL' ? 'E-mail' : 'Telefone'}
            </Typography>}
            {selected.location && <Typography><strong>Localidade:</strong> {selected.location}</Typography>}
            {selected.householdSize != null && (
              <Typography><strong>Tamanho do domicílio:</strong> {selected.householdSize}</Typography>
            )}
            <Typography><strong>Tipo de assistência:</strong> {assistanceLabels[selected.assistanceType] ?? selected.assistanceType}</Typography>
            {selected.welcomePreference === 'VISIT' && (
              <Typography>
                <strong>Acolhimento:</strong> presencial em{' '}
                {selected.welcomeDate
                  ? new Date(selected.welcomeDate + 'T12:00:00').toLocaleDateString('pt-BR')
                  : 'data a confirmar'}
                {selected.welcomeTime && ` por volta das ${selected.welcomeTime}`}
              </Typography>
            )}
            {selected.welcomePreference === 'PHONE' && (
              <Typography><strong>Acolhimento:</strong> prefere contato por telefone</Typography>
            )}
            <Typography><strong>Criada:</strong> {formatDate(selected.createdAt)}</Typography>
            <Typography><strong>Atualizada:</strong> {formatDate(selected.updatedAt)}</Typography>
            <Typography><strong>Responsável:</strong> {selected.assignedUsername || 'Não atribuído'}</Typography>
            {canReadSensitiveDetails && selected.message && (
              <Typography sx={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>
                <strong>Mensagem:</strong> {selected.message}
              </Typography>
            )}
          </Stack>
        </Paper>

        {transitions.length > 0 && (
          <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="subtitle1" fontWeight={700} gutterBottom>Atualizar status</Typography>
            {!isAdmin && <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              Seu perfil pode apenas encaminhar esta solicitação.
            </Typography>}
            <Stack spacing={1}>
              <FormControl fullWidth size="small">
                <InputLabel id="next-request-status-label">Novo status</InputLabel>
                <Select
                  labelId="next-request-status-label"
                  label="Novo status"
                  value={nextStatus}
                  onChange={(event) => setNextStatus(event.target.value as ServiceRequestStatus | '')}
                >
                  {transitions.map((status) => <MenuItem key={status} value={status}>{statusLabels[status]}</MenuItem>)}
                </Select>
              </FormControl>
              {isAdmin && <TextField
                label="Observação da alteração (opcional)"
                value={statusObservation}
                onChange={(event) => setStatusObservation(event.target.value)}
                multiline
                minRows={2}
                fullWidth
                inputProps={{ maxLength: 1000 }}
              />}
              <Button
                variant="contained"
                disabled={!nextStatus || saving}
                onClick={() => setConfirmStatusOpen(true)}
              >
                Alterar status
              </Button>
            </Stack>
          </Paper>
        )}

        {canAssign && (
          <>
            <Paper component="form" variant="outlined" onSubmit={(event) => void assignRequest(event)} sx={{ p: 2 }}>
              <Typography variant="subtitle1" fontWeight={700} gutterBottom>Atribuir responsável</Typography>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                <TextField
                  label="Nome de usuário"
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                  fullWidth
                  required
                  inputProps={{ maxLength: 100 }}
                />
                <Button type="submit" variant="outlined" disabled={!username.trim() || saving}>Atribuir</Button>
              </Stack>
            </Paper>
          </>
        )}
        {isAdmin && (
          <Paper component="form" variant="outlined" onSubmit={(event) => void addNote(event)} sx={{ p: 2 }}>
            <Typography variant="subtitle1" fontWeight={700} gutterBottom>Nota interna</Typography>
            <Stack spacing={1}>
              <TextField
                label="Observação"
                value={note}
                onChange={(event) => setNote(event.target.value)}
                multiline
                minRows={2}
                fullWidth
                required
                inputProps={{ maxLength: 2000 }}
              />
              <Button type="submit" variant="outlined" disabled={!note.trim() || saving}>Registrar nota</Button>
            </Stack>
          </Paper>
        )}

        <Paper variant="outlined" sx={{ p: 2 }}>
          <Typography variant="subtitle1" fontWeight={700} gutterBottom>Histórico</Typography>
          {history.length === 0
            ? <Typography color="text.secondary">Nenhum evento de histórico registrado.</Typography>
            : <Stack spacing={1}>
              {history.map((entry) => (
                <Box key={entry.id} sx={{ borderLeft: '2px solid', borderColor: 'divider', pl: 1.5 }}>
                  <Typography variant="body2">
                    {entry.newStatus
                      ? `${entry.previousStatus ? `${statusLabels[entry.previousStatus]} → ` : ''}${statusLabels[entry.newStatus]}`
                      : 'Atualização registrada'}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {entry.changedBy} · {formatDate(entry.changedAt)}
                  </Typography>
                  {canReadSensitiveDetails && entry.observation && (
                    <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>
                      {entry.observation}
                    </Typography>
                  )}
                </Box>
              ))}
            </Stack>}
        </Paper>
      </Stack>
    )
  }

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default', overflowX: 'hidden' }}>
      <Toolbar sx={{ bgcolor: 'primary.main', color: 'white' }}>
        <Button color="inherit" startIcon={<ArrowBackIcon />} onClick={() => navigate('/modulos')} sx={{ mr: 2 }}>
          Dashboard
        </Button>
        <Typography variant="h6" component="h1" sx={{ flexGrow: 1 }}>Solicitações de assistência</Typography>
        <Typography variant="body2" sx={{ display: { xs: 'none', sm: 'block' } }}>{user?.fullName}</Typography>
        <ThemeToggle />
      </Toolbar>
      <Container maxWidth="xl" sx={{ py: 3, px: { xs: 1.5, sm: 3 } }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ sm: 'center' }} sx={{ mb: 2 }}>
          <Box sx={{ flexGrow: 1 }}>
            <Typography variant="h4" component="h2" fontWeight={700}>Fila de solicitações</Typography>
            <Typography color="text.secondary">Consulte e acompanhe os pedidos recebidos. Nenhum pedido implica aprovação automática.</Typography>
          </Box>
          <Button variant="outlined" startIcon={<RefreshIcon />} disabled={loading} onClick={refresh}>Atualizar</Button>
        </Stack>

        <Paper component="form" onSubmit={applyFilters} sx={{ p: 2, mb: 2 }}>
          <Grid container spacing={1.5} alignItems="center">
            <Grid item xs={12} sm={6} md={3}>
              <TextField
                label="Protocolo"
                value={filters.protocol}
                onChange={(event) => setFilters((current) => ({ ...current, protocol: event.target.value }))}
                fullWidth
                size="small"
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <FormControl fullWidth size="small">
                <InputLabel id="request-filter-status-label">Status</InputLabel>
                <Select
                  labelId="request-filter-status-label"
                  label="Status"
                  value={filters.status}
                  onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value as ServiceRequestStatus | '' }))}
                >
                  <MenuItem value="">Todos</MenuItem>
                  {statuses.map((status) => <MenuItem key={status} value={status}>{statusLabels[status]}</MenuItem>)}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={6} sm={4} md={2}>
              <TextField
                label="De"
                type="date"
                value={filters.from}
                onChange={(event) => setFilters((current) => ({ ...current, from: event.target.value }))}
                InputLabelProps={{ shrink: true }}
                fullWidth
                size="small"
              />
            </Grid>
            <Grid item xs={6} sm={4} md={2}>
              <TextField
                label="Até"
                type="date"
                value={filters.to}
                onChange={(event) => setFilters((current) => ({ ...current, to: event.target.value }))}
                InputLabelProps={{ shrink: true }}
                fullWidth
                size="small"
              />
            </Grid>
            <Grid item xs={12} sm={4} md={2}>
              <Button type="submit" variant="contained" fullWidth>Buscar</Button>
            </Grid>
          </Grid>
        </Paper>

        {error && <Alert severity="error" sx={{ mb: 2 }} action={
          <Button color="inherit" size="small" onClick={refresh}>Tentar novamente</Button>
        }>{error}</Alert>}
        {loading && <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}><CircularProgress /></Box>}
        {!loading && !error && requests.length === 0 && (
          <Paper sx={{ p: 4, textAlign: 'center' }}>
            <Typography variant="h6">Nenhuma solicitação encontrada</Typography>
            <Typography color="text.secondary">Altere os filtros ou tente novamente mais tarde.</Typography>
          </Paper>
        )}

        {!loading && !error && requests.length > 0 && (
          <>
            {isMobile ? (
              <Stack spacing={1.5}>
                {requests.map((request) => (
                  <Card key={request.id} sx={{ minWidth: 0 }}>
                    <CardContent sx={{ overflowWrap: 'anywhere' }}>
                      <Stack spacing={1}>
                        <Typography variant="subtitle1" fontWeight={700}>Solicitação #{request.id}</Typography>
                        {renderStatus(request.status)}
                        <Typography><strong>Data:</strong> {formatDate(request.createdAt)}</Typography>
                        {request.location && <Typography><strong>Localidade:</strong> {request.location}</Typography>}
                        <Typography><strong>Contato:</strong> {request.contact}</Typography>
                        <Typography><strong>Tipo:</strong> {assistanceLabels[request.assistanceType] ?? request.assistanceType}</Typography>
                      </Stack>
                    </CardContent>
                    <CardActions>
                      <Button startIcon={<VisibilityIcon />} onClick={() => void openDetails(request)}>Ver detalhes</Button>
                    </CardActions>
                  </Card>
                ))}
              </Stack>
            ) : (
              <TableContainer component={Paper}>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Solicitação</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Recebida</TableCell>
                      <TableCell>Localidade</TableCell>
                      <TableCell>Contato</TableCell>
                      <TableCell>Tipo</TableCell>
                      <TableCell align="right">Ação</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {requests.map((request) => (
                      <TableRow key={request.id} hover>
                        <TableCell>#{request.id}</TableCell>
                        <TableCell>{renderStatus(request.status)}</TableCell>
                        <TableCell>{formatDate(request.createdAt)}</TableCell>
                        <TableCell>{request.location || '—'}</TableCell>
                        <TableCell>{request.contact}</TableCell>
                        <TableCell>{assistanceLabels[request.assistanceType] ?? request.assistanceType}</TableCell>
                        <TableCell align="right">
                          <Button size="small" startIcon={<VisibilityIcon />} onClick={() => void openDetails(request)}>
                            Detalhes
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
            <Stack direction="row" spacing={2} alignItems="center" justifyContent="space-between" sx={{ mt: 2 }}>
              <Typography variant="body2" color="text.secondary">
                {totalElements} solicitação(ões) · Página {page + 1} de {Math.max(totalPages, 1)}
              </Typography>
              <Stack direction="row" spacing={1}>
                <Button variant="outlined" disabled={page <= 0 || loading} onClick={() => setPage((current) => current - 1)}>Anterior</Button>
                <Button variant="outlined" disabled={page + 1 >= totalPages || loading} onClick={() => setPage((current) => current + 1)}>Próxima</Button>
              </Stack>
            </Stack>
          </>
        )}
      </Container>

      <Dialog
        open={detailsLoading || Boolean(selected) || Boolean(detailsError)}
        onClose={() => { setSelected(null); setDetailTarget(null); setDetailsError('') }}
        fullScreen={isMobile}
        fullWidth
        maxWidth="md"
        PaperProps={{ sx: { maxHeight: { sm: 'calc(100% - 64px)' }, overflowX: 'hidden' } }}
      >
        <DialogTitle sx={{ overflowWrap: 'anywhere' }}>
          {selected
            ? `Solicitação #${selected.id}`
            : detailTarget
              ? `Solicitação #${detailTarget.id}`
              : 'Detalhes da solicitação'}
        </DialogTitle>
        <DialogContent dividers sx={{ minWidth: 0 }}>{renderDetailsContent()}</DialogContent>
        <DialogActions>
          <Button onClick={() => { setSelected(null); setDetailTarget(null); setDetailsError('') }}>Fechar</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={confirmStatusOpen} onClose={() => !saving && setConfirmStatusOpen(false)}>
        <DialogTitle>Confirmar alteração de status</DialogTitle>
        <DialogContent>
          <Typography>
            Alterar o status para <strong>{nextStatus ? statusLabels[nextStatus] : ''}</strong>?
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            Esta mudança registra o andamento da solicitação; não aprova automaticamente um benefício.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmStatusOpen(false)} disabled={saving}>Cancelar</Button>
          <Button variant="contained" onClick={() => void updateStatus()} disabled={saving}>
            {saving ? <CircularProgress size={20} /> : 'Confirmar'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default ServiceRequestsPage

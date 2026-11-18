import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Container,
  Grid,
  IconButton,
  MenuItem,
  Stack,
  TextField,
  Toolbar,
  Typography,
} from '@mui/material'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import DirectionsRunIcon from '@mui/icons-material/DirectionsRun'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import runService, { AdminRunRegistration } from '../services/runService'
import { getApiErrorMessage } from '../utils/apiError'
import ThemeToggle from '../components/ThemeToggle'

const STATUS_LABELS: Record<string, string> = {
  PENDENTE: 'Pendente',
  PAGA: 'Paga',
  CANCELADA: 'Cancelada',
}

const STATUS_COLORS: Record<string, 'warning' | 'success' | 'default'> = {
  PENDENTE: 'warning',
  PAGA: 'success',
  CANCELADA: 'default',
}

function fmtBRL(value: number): string {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

function RunRegistrationsPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAdmin = Boolean(user?.roles.includes('ROLE_ADMIN'))
  const [registrations, setRegistrations] = useState<AdminRunRegistration[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [filterModality, setFilterModality] = useState('')
  const [filterStatus, setFilterStatus] = useState('')
  const [filterText, setFilterText] = useState('')
  const [acting, setActing] = useState<number | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setRegistrations(await runService.listAdmin())
    } catch (loadError) {
      setError(getApiErrorMessage(loadError, 'Não foi possível carregar as inscrições.'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { void load() }, [load])

  const filtered = useMemo(() => registrations.filter((r) => {
    if (filterModality && r.modality !== filterModality) return false
    if (filterStatus && r.status !== filterStatus) return false
    if (filterText) {
      const q = filterText.toLowerCase()
      const hay = `${r.fullName} ${r.email ?? ''} ${r.phone ?? ''}`.toLowerCase()
      if (!hay.includes(q)) return false
    }
    return true
  }), [registrations, filterModality, filterStatus, filterText])

  const totals = useMemo(() => {
    const paid = registrations.filter((r) => r.status === 'PAGA')
    const pending = registrations.filter((r) => r.status === 'PENDENTE')
    const raised = paid.reduce((sum, r) => sum + (r.totalCharged ?? r.amount), 0)
    const by3km = paid.filter((r) => r.modality === '3KM').length
    const by7km = paid.filter((r) => r.modality === '7KM').length
    return { paid: paid.length, pending: pending.length, raised, by3km, by7km }
  }, [registrations])

  const confirm = async (id: number) => {
    setActing(id)
    setError('')
    try {
      await runService.confirmPayment(id)
      setMessage('Pagamento confirmado — inscrição paga e somada na arrecadação.')
      await load()
    } catch (confirmError) {
      setError(getApiErrorMessage(confirmError, 'Não foi possível confirmar.'))
    } finally {
      setActing(null)
    }
  }

  const cancel = async (id: number) => {
    setActing(id)
    setError('')
    try {
      await runService.cancel(id)
      setMessage('Inscrição cancelada — a vaga voltou a ficar disponível.')
      await load()
    } catch (cancelError) {
      setError(getApiErrorMessage(cancelError, 'Não foi possível cancelar.'))
    } finally {
      setActing(null)
    }
  }

  const paymentLink = async (id: number) => {
    setActing(id)
    setError('')
    try {
      const url = await runService.paymentLink(id)
      try {
        await navigator.clipboard.writeText(url)
        setMessage('Link de pagamento gerado, copiado e enviado por e-mail ao inscrito — cole no WhatsApp se preferir.')
      } catch {
        setMessage(`Link de pagamento: ${url}`)
      }
    } catch (linkError) {
      setError(getApiErrorMessage(linkError, 'Não foi possível gerar o link.'))
    } finally {
      setActing(null)
    }
  }

  const sendRetryEmail = async (id: number) => {
    setActing(id)
    setError('')
    try {
      await runService.sendRetryEmail(id)
      setMessage('E-mail de retentativa enviado.')
    } catch (emailError) {
      setError(getApiErrorMessage(emailError, 'Não foi possível enviar o e-mail.'))
    } finally {
      setActing(null)
    }
  }

  const remove = async (id: number, name: string) => {
    if (!window.confirm(`Excluir a inscrição de ${name}? Isso não pode ser desfeito.`)) return
    setActing(id)
    setError('')
    try {
      await runService.remove(id)
      setMessage('Inscrição excluída.')
      await load()
    } catch (removeError) {
      setError(getApiErrorMessage(removeError, 'Não foi possível excluir.'))
    } finally {
      setActing(null)
    }
  }

  const copyPendingContacts = async () => {
    const pending = registrations.filter((r) => r.status === 'PENDENTE')
    if (pending.length === 0) {
      setError('Não há inscrições pendentes para contatar.')
      return
    }
    const text = pending.map((r) =>
      `${r.fullName} — ${r.phone ?? r.email ?? 'sem contato'}${r.email && r.phone ? ` (${r.email})` : ''} — inscrição #${r.id} (${r.modality === '3KM' ? '3 km caminhada' : '7 km corrida'})`
    ).join('\n')
    try {
      await navigator.clipboard.writeText(text)
      setMessage(`${pending.length} contato(s) de inscrições pendentes copiado(s) — cole no WhatsApp para chamar a equipe.`)
    } catch {
      setError('Não foi possível copiar — selecione e copie manualmente.')
    }
  }


  const whatsappLink = (r: AdminRunRegistration): string | null => {
    const digits = (r.phone ?? '').replace(/\D/g, '')
    if (!digits || !r.checkinCode) return null
    const number = digits.startsWith('55') ? digits : `55${digits}`
    const firstName = r.fullName.trim().split(' ')[0]
    const verifyUrl = `${window.location.origin}/run/verificar/${r.checkinCode}`
    const msg = `Olá, ${firstName}! Sua inscrição no Mercado Solidário Run foi CONFIRMADA. ` +
      `Seu QR code de retirada do kit (camisa + medalha) está aqui: ${verifyUrl} ` +
      `— apresente na retirada dia 13/12. Obrigado por correr com a gente!`
    return `https://wa.me/${number}?text=${encodeURIComponent(msg)}`
  }
  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default', pb: 5 }}>
      <Box sx={{ bgcolor: 'primary.main', color: 'white' }}>
        <Toolbar>
          <IconButton color="inherit" onClick={() => navigate('/modulos')} aria-label="Voltar ao painel">
            <ArrowBackIcon />
          </IconButton>
          <DirectionsRunIcon sx={{ mr: 1 }} />
          <Typography variant="h6" sx={{ flexGrow: 1 }}>Inscritos — Run</Typography>
          <ThemeToggle />
        </Toolbar>
      </Box>
      <Container maxWidth="xl" sx={{ mt: 3 }}>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        {message && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setMessage('')}>{message}</Alert>}
        {loading ? (
          <Box sx={{ py: 8, textAlign: 'center' }}><CircularProgress /></Box>
        ) : (
          <>
            <Grid container spacing={2} sx={{ mb: 3 }}>
              <Grid item xs={6} sm={3}>
                <Card><CardContent>
                  <Typography color="text.secondary">Pagas</Typography>
                  <Typography variant="h4" fontWeight={800} color="success.main">{totals.paid}</Typography>
                </CardContent></Card>
              </Grid>
              <Grid item xs={6} sm={3}>
                <Card><CardContent>
                  <Typography color="text.secondary">Aguardando pagamento</Typography>
                  <Typography variant="h4" fontWeight={800} color="warning.main">{totals.pending}</Typography>
                </CardContent></Card>
              </Grid>
              <Grid item xs={6} sm={3}>
                <Card><CardContent>
                  <Typography color="text.secondary">3KM / 7KM pagos</Typography>
                  <Typography variant="h4" fontWeight={800}>{totals.by3km} / {totals.by7km}</Typography>
                </CardContent></Card>
              </Grid>
              <Grid item xs={6} sm={3}>
                <Card><CardContent>
                  <Typography color="text.secondary">Arrecadado</Typography>
                  <Typography variant="h4" fontWeight={800} color="primary.main">{fmtBRL(totals.raised)}</Typography>
                </CardContent></Card>
              </Grid>
            </Grid>

            <Stack direction={{ xs: 'column', sm: 'row' }} gap={1} sx={{ mb: 2 }}>
              <TextField size="small" label="Buscar nome, e-mail ou telefone" value={filterText}
                onChange={(e) => setFilterText(e.target.value)} sx={{ minWidth: 240 }} />
              <TextField size="small" select label="Modalidade" value={filterModality}
                onChange={(e) => setFilterModality(e.target.value)} sx={{ minWidth: 170 }}>
                <MenuItem value="">Todas</MenuItem>
                <MenuItem value="3KM">3 km — caminhada</MenuItem>
                <MenuItem value="7KM">7 km — corrida</MenuItem>
              </TextField>
              <TextField size="small" select label="Status" value={filterStatus}
                onChange={(e) => setFilterStatus(e.target.value)} sx={{ minWidth: 160 }}>
                <MenuItem value="">Todos</MenuItem>
                <MenuItem value="PENDENTE">Pendente</MenuItem>
                <MenuItem value="PAGA">Paga</MenuItem>
                <MenuItem value="CANCELADA">Cancelada</MenuItem>
              </TextField>
              <Button variant="outlined" onClick={() => void copyPendingContacts()}>Copiar contatos pendentes</Button>
              <Button onClick={() => void load()}>Atualizar</Button>
            </Stack>

            {filtered.length === 0 ? (
              <Alert severity="info">Nenhuma inscrição encontrada.</Alert>
            ) : (
              <Grid container spacing={1}>
                {filtered.map((r) => (
                  <Grid item xs={12} md={6} key={r.id}>
                    <Card variant="outlined">
                      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                        <Stack direction="row" justifyContent="space-between" alignItems="center" gap={1}>
                          <Typography fontWeight={700}>#{r.id} · {r.fullName}</Typography>
                          <Chip size="small" color={STATUS_COLORS[r.status] ?? 'default'}
                            label={STATUS_LABELS[r.status] ?? r.status} />
                        </Stack>
                        <Typography variant="body2">
                          {r.modality === '3KM' ? '3 km — caminhada' : '7 km — corrida'} · {fmtBRL(r.totalCharged ?? r.amount)}
                          {r.extraDonation ? ` (${fmtBRL(r.amount)} + ${fmtBRL(r.extraDonation)} extra)` : ''}
                          {' · '}{r.paymentMethod}
                          {r.shirtSize ? ` · camisa ${r.shirtSize}` : ''}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          {r.email || '—'}{r.phone ? ` · ${r.phone}` : ''}
                          {r.birthDate ? ` · nasc. ${new Date(r.birthDate + 'T12:00:00').toLocaleDateString('pt-BR')}` : ''}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {new Date(r.createdAt).toLocaleString('pt-BR')}
                          {r.paidAt ? ` · pago em ${new Date(r.paidAt).toLocaleString('pt-BR')}` : ''}
                        </Typography>
                        {r.kitDeliveredAt && (
                          <Typography variant="caption" color="success.main" fontWeight={700}>
                            Kit retirado em {new Date(r.kitDeliveredAt).toLocaleString('pt-BR')}
                            {r.kitDeliveredBy ? ` — entregue por ${r.kitDeliveredBy}` : ''}
                          </Typography>
                        )}
                        {isAdmin && (r.status === 'PENDENTE' || r.status === 'CANCELADA') && (
                          <Stack direction="row" gap={1} sx={{ mt: 1 }}>
                            {r.status === 'PENDENTE' && (
                              <>
                                <Button size="small" variant="contained" color="success"
                                  disabled={acting === r.id} onClick={() => void confirm(r.id)}>
                                  Confirmar pagamento
                                </Button>
                                <Button size="small" variant="outlined" color="error"
                                  disabled={acting === r.id} onClick={() => void cancel(r.id)}>
                                  Cancelar
                                </Button>
                                <Button size="small" variant="outlined" color="info"
                                  disabled={acting === r.id} onClick={() => void sendRetryEmail(r.id)}>
                                  E-mail retentativa
                                </Button>
                                <Button size="small" variant="outlined" color="secondary"
                                  disabled={acting === r.id} onClick={() => void paymentLink(r.id)}>
                                  Link de pagamento
                                </Button>
                              </>
                            )}
                            <Button size="small" variant="text" color="error"
                              disabled={acting === r.id} onClick={() => void remove(r.id, r.fullName)}>
                              Excluir
                            </Button>
                          </Stack>
                        )}
                        {r.status === 'PAGA' && whatsappLink(r) && (
                          <Button size="small" variant="outlined" color="success" sx={{ mt: 1 }}
                            component="a" href={whatsappLink(r) ?? '#'} target="_blank" rel="noreferrer">
                            Enviar QR no WhatsApp
                          </Button>
                        )}
                      </CardContent>
                    </Card>
                  </Grid>
                ))}
              </Grid>
            )}
          </>
        )}
      </Container>
    </Box>
  )
}

export default RunRegistrationsPage

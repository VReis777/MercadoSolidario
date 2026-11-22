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
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
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
  const [editTarget, setEditTarget] = useState<AdminRunRegistration | null>(null)
  const [editForm, setEditForm] = useState({ fullName: '', email: '', phone: '', shirtSize: '', modality: '7KM' })
  const [savingEdit, setSavingEdit] = useState(false)
  const [importOpen, setImportOpen] = useState(false)
  const [importText, setImportText] = useState('')
  const [donationsText, setDonationsText] = useState('')
  const [importing, setImporting] = useState(false)
  const [importResult, setImportResult] = useState<{ created: number; skipped: number; errors: string[] } | null>(null)
  const [mpOpen, setMpOpen] = useState(false)
  const [mpPayments, setMpPayments] = useState<{
    paymentId: string | null; externalReference: string | null; amount: number | null
    paymentMethod: string | null; payerEmail: string | null; payerName: string | null
    status: string | null; dateCreated: string | null
  }[]>([])
  const [mpLoading, setMpLoading] = useState(false)
  const [bulkBusy, setBulkBusy] = useState(false)

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
    const raised = paid.reduce((sum, r) => sum + (r.netAmount ?? r.totalCharged ?? r.amount), 0)
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

  const syncPayment = async (id: number) => {
    setActing(id)
    setError('')
    try {
      const reg = await runService.syncPayment(id)
      setMessage(reg.status === 'PAGA'
        ? `Pagamento confirmado como ${reg.paymentMethod === 'PIX' ? 'PIX' : 'cartão'} — QR enviado ao inscrito.`
        : 'Nenhum pagamento aprovado encontrado no Mercado Pago para esta inscrição.')
      await load()
    } catch (syncError) {
      setError(getApiErrorMessage(syncError, 'Não foi possível verificar o pagamento.'))
    } finally {
      setActing(null)
    }
  }

  const estornarLegado = async () => {
    if (!window.confirm(
      'Estornar TODAS as inscrições pagas que vieram da importação? ' +
      'Elas voltam para PENDENTE e as doações automáticas de R$100 são removidas. ' +
      'Depois use "Sincronizar pendentes" para confirmar pelo Mercado Pago real.')) return
    setBulkBusy(true)
    setError('')
    try {
      const res = await runService.estornarLegado()
      setMessage(`${res.reverted} inscrição(ões) estornada(s) — agora é só sincronizar com o Mercado Pago.`)
      await load()
    } catch (estornoError) {
      setError(getApiErrorMessage(estornoError, 'Não foi possível estornar as importadas.'))
    } finally {
      setBulkBusy(false)
    }
  }

  const syncAllPending = async () => {
    const pending = registrations.filter((r) => r.status === 'PENDENTE')
    if (pending.length === 0) {
      setError('Não há inscrições pendentes para sincronizar.')
      return
    }
    setBulkBusy(true)
    setError('')
    let confirmed = 0
    let errors = 0
    for (const r of pending) {
      try {
        const reg = await runService.syncPayment(r.id)
        if (reg.status === 'PAGA') confirmed++
      } catch {
        errors++
      }
    }
    setMessage(
      `Sincronização concluída: ${confirmed} pagamento(s) confirmado(s) no Mercado Pago, ` +
      `${pending.length - confirmed - errors} sem pagamento aprovado, ${errors} erro(s).`)
    await load()
    setBulkBusy(false)
  }

  const openMpPayments = async () => {
    setMpOpen(true)
    setMpLoading(true)
    try {
      setMpPayments(await runService.mpPayments())
    } catch {
      setMpPayments([])
    } finally {
      setMpLoading(false)
    }
  }

  const parseImport = () => {
    const registrations = importText.split('\n')
      .map((l) => l.trim())
      .filter((l) => l && !l.startsWith('#'))
      .map((line) => {
        const p = line.split(';')
        const num = (v?: string) => (v && v.trim() ? Number(v.trim()) : null)
        const str = (v?: string) => (v && v.trim() ? v.trim() : null)
        const dt = (v?: string) => {
          const s = str(v)
          if (!s) return null
          const iso = s.includes('T') ? s : s.replace(' ', 'T')
          return /\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?/.test(iso)
            ? (iso.length === 16 ? `${iso}:00` : iso)
            : null
        }
        return {
          originalId: num(p[0]),
          fullName: (p[1] ?? '').trim(),
          email: str(p[2]),
          phone: str(p[3]),
          birthDate: str(p[4]),
          modality: (p[5] ?? '').trim().toUpperCase(),
          shirtSize: str(p[6]),
          status: (p[7] ?? '').trim().toUpperCase() || 'PENDENTE',
          paymentMethod: str(p[8]),
          createdAt: dt(p[9]),
        }
      })
      .filter((r) => r.fullName)
    const donations = donationsText.split('\n')
      .map((l) => l.trim())
      .filter((l) => l && !l.startsWith('#'))
      .map((line) => {
        const p = line.split(';')
        return {
          amount: Number((p[0] ?? '').trim().replace(',', '.')),
          donorName: p[1]?.trim() || null,
          donorContact: p[2]?.trim() || null,
          observation: p.slice(3).join(';').trim() || null,
        }
      })
      .filter((d) => Number.isFinite(d.amount) && d.amount > 0)
    return { registrations, donations }
  }

  const submitImport = async () => {
    const { registrations, donations } = parseImport()
    if (registrations.length === 0 && donations.length === 0) {
      setError('Nada para importar — preencha as linhas no formato indicado.')
      return
    }
    setImporting(true)
    setError('')
    setImportResult(null)
    try {
      const result = await runService.importRegistrations({ registrations, donations })
      setImportResult(result)
      if (result.errors.length === 0) {
        setMessage(`Importação concluída: ${result.created} registro(s) criado(s), ${result.skipped} ignorado(s).`)
      }
      await load()
    } catch (importError) {
      setError(getApiErrorMessage(importError, 'Falha na importação.'))
    } finally {
      setImporting(false)
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

  const resendQr = async (id: number) => {
    setActing(id)
    setError('')
    try {
      await runService.resendQrEmail(id)
      setMessage('E-mail com QR reenviado ao inscrito.')
    } catch (qrError) {
      setError(getApiErrorMessage(qrError, 'Não foi possível reenviar o e-mail do QR.'))
    } finally {
      setActing(null)
    }
  }

  const revertPayment = async (id: number, name: string) => {
    if (!window.confirm(
      `Estornar o pagamento de ${name}? A inscrição volta para PENDENTE e os R$ da arrecadação são removidos. ` +
      `Use só quando o pagamento na verdade pertence a outra inscrição.`)) return
    setActing(id)
    setError('')
    try {
      await runService.revertPayment(id)
      setMessage('Pagamento estornado — inscrição voltou para pendente.')
      await load()
    } catch (revertError) {
      setError(getApiErrorMessage(revertError, 'Não foi possível estornar.'))
    } finally {
      setActing(null)
    }
  }

  const claimPayment = async (id: number) => {
    const raw = window.prompt(
      'ID da inscrição que gerou o link/pagamento usado (ex.: 32). ' +
      'A inscrição de origem precisa estar estornada ou cancelada.')
    if (!raw) return
    const fromId = Number(raw.replace(/\D/g, ''))
    if (!fromId) {
      setError('Informe um número de inscrição válido.')
      return
    }
    setActing(id)
    setError('')
    try {
      const reg = await runService.claimPayment(id, fromId)
      setMessage(`Pagamento da inscrição #${fromId} vinculado — ${reg.fullName} confirmada como ${reg.paymentMethod === 'PIX' ? 'PIX' : 'cartão'}.`)
      await load()
    } catch (claimError) {
      setError(getApiErrorMessage(claimError, 'Não foi possível vincular o pagamento.'))
    } finally {
      setActing(null)
    }
  }

  const linkMpPayment = async (id: number) => {
    const raw = window.prompt(
      'Cole o ID do pagamento aprovado no Mercado Pago (está na lista "Pagamentos no MP", no fim de cada linha). ' +
      'Use quando o pagamento antigo não foi encontrado automaticamente.')
    if (!raw) return
    const paymentId = raw.replace(/\D/g, '')
    if (!paymentId) {
      setError('Informe o ID numérico do pagamento.')
      return
    }
    setActing(id)
    setError('')
    try {
      const reg = await runService.linkPayment(id, paymentId)
      setMessage(`Pagamento ${paymentId} vinculado — ${reg.fullName} confirmada como ${reg.paymentMethod === 'PIX' ? 'PIX' : 'cartão'}.`)
      await load()
    } catch (linkMpError) {
      setError(getApiErrorMessage(linkMpError, 'Não foi possível vincular o pagamento.'))
    } finally {
      setActing(null)
    }
  }

  const openEdit = (r: AdminRunRegistration) => {
    setEditTarget(r)
    setEditForm({
      fullName: r.fullName,
      email: r.email ?? '',
      phone: r.phone ?? '',
      shirtSize: r.shirtSize ?? '',
      modality: r.modality,
    })
  }

  const saveEdit = async () => {
    if (!editTarget) return
    setSavingEdit(true)
    setError('')
    try {
      await runService.update(editTarget.id, {
        fullName: editForm.fullName,
        email: editForm.email || undefined,
        phone: editForm.phone || undefined,
        shirtSize: editForm.shirtSize || undefined,
        modality: editForm.modality || undefined,
      })
      setMessage('Inscrição atualizada.')
      setEditTarget(null)
      await load()
    } catch (editError) {
      setError(getApiErrorMessage(editError, 'Não foi possível salvar.'))
    } finally {
      setSavingEdit(false)
    }
  }

  const recadastrar = async (id: number, name: string) => {
    if (!window.confirm(`Criar uma nova inscrição PENDENTE com os dados de ${name}? A inscrição atual não é alterada.`)) return
    setActing(id)
    setError('')
    try {
      const reg = await runService.recadastrar(id)
      setMessage(`Nova inscrição #${reg.id} criada como pendente com os dados de ${name}.`)
      await load()
    } catch (reError) {
      setError(getApiErrorMessage(reError, 'Não foi possível recadastrar.'))
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
                  <Typography color="text.secondary">Arrecadado (líquido)</Typography>
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
              {isAdmin && (
                <Button variant="outlined" color="secondary" onClick={() => setImportOpen(true)}>
                  Importar inscrições
                </Button>
              )}
              {isAdmin && (
                <Button variant="outlined" color="info" onClick={() => void openMpPayments()}>
                  Pagamentos no MP
                </Button>
              )}
              {isAdmin && (
                <Button variant="outlined" color="warning" disabled={bulkBusy}
                  onClick={() => void estornarLegado()}>
                  {bulkBusy ? 'Processando…' : 'Estornar importados'}
                </Button>
              )}
              {isAdmin && (
                <Button variant="contained" color="success" disabled={bulkBusy}
                  onClick={() => void syncAllPending()}>
                  {bulkBusy ? 'Sincronizando…' : 'Sincronizar pendentes'}
                </Button>
              )}
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
                          {r.status === 'PAGA' && r.netAmount != null && r.netAmount !== (r.totalCharged ?? r.amount)
                            ? ` · líq. ${fmtBRL(r.netAmount)}` : ''}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          {r.email || '—'}{r.phone ? ` · ${r.phone}` : ''}
                          {r.birthDate ? ` · nasc. ${new Date(r.birthDate + 'T12:00:00').toLocaleDateString('pt-BR')}` : ''}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {new Date(r.createdAt).toLocaleString('pt-BR')}
                          {r.paidAt ? ` · pago em ${new Date(r.paidAt).toLocaleString('pt-BR')}` : ''}
                          {r.paymentRef ? ` · ref. MP: ${r.paymentRef}` : ''}
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
                                <Button size="small" variant="outlined" color="primary"
                                  disabled={acting === r.id} onClick={() => void syncPayment(r.id)}>
                                  Verificar no MP
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
                                <Button size="small" variant="outlined" color="warning"
                                  disabled={acting === r.id} onClick={() => void claimPayment(r.id)}>
                                  Vincular pagto. MP
                                </Button>
                                <Button size="small" variant="outlined" color="success"
                                  disabled={acting === r.id} onClick={() => void linkMpPayment(r.id)}>
                                  Colar ID do pagto.
                                </Button>
                              </>
                            )}
                            <Button size="small" variant="text" color="error"
                              disabled={acting === r.id} onClick={() => void remove(r.id, r.fullName)}>
                              Excluir
                            </Button>
                            <Button size="small" variant="text" color="primary"
                              disabled={acting === r.id} onClick={() => void recadastrar(r.id, r.fullName)}>
                              Recadastrar
                            </Button>
                          </Stack>
                        )}
                        {isAdmin && (
                          <Button size="small" variant="text" color="inherit"
                            onClick={() => openEdit(r)} sx={{ mt: 0.5, alignSelf: 'flex-start' }}>
                            Editar dados
                          </Button>
                        )}
                        {r.status === 'PAGA' && (
                          <Stack direction="row" gap={1} sx={{ mt: 1 }}>
                            {whatsappLink(r) && (
                              <Button size="small" variant="outlined" color="success"
                                component="a" href={whatsappLink(r) ?? '#'} target="_blank" rel="noreferrer">
                                Enviar QR no WhatsApp
                              </Button>
                            )}
                            {isAdmin && (
                              <Button size="small" variant="outlined" color="info"
                                disabled={acting === r.id} onClick={() => void resendQr(r.id)}>
                                Reenviar QR por e-mail
                              </Button>
                            )}
                            {isAdmin && (
                              <Button size="small" variant="outlined" color="warning"
                                disabled={acting === r.id}
                                onClick={() => void revertPayment(r.id, r.fullName)}>
                                Estornar pagamento
                              </Button>
                            )}
                          </Stack>
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

      <Dialog open={editTarget !== null} onClose={() => setEditTarget(null)} maxWidth="xs" fullWidth>
        <DialogTitle>Editar inscrição #{editTarget?.id}</DialogTitle>
        <DialogContent dividers>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField required fullWidth label="Nome completo" value={editForm.fullName}
              onChange={(e) => setEditForm({ ...editForm, fullName: e.target.value })} />
            <TextField fullWidth label="E-mail" type="email" value={editForm.email}
              onChange={(e) => setEditForm({ ...editForm, email: e.target.value })} />
            <TextField fullWidth label="Telefone/WhatsApp" value={editForm.phone}
              onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })} />
            <TextField fullWidth select label="Modalidade" value={editForm.modality}
              onChange={(e) => setEditForm({ ...editForm, modality: e.target.value })}>
              <MenuItem value="3KM">3 km — caminhada</MenuItem>
              <MenuItem value="7KM">7 km — corrida</MenuItem>
            </TextField>
            <TextField fullWidth select label="Tamanho da camisa" value={editForm.shirtSize}
              onChange={(e) => setEditForm({ ...editForm, shirtSize: e.target.value })}>
              <MenuItem value="">Não informado</MenuItem>
              {['P', 'M', 'G', 'GG', 'XG'].map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
            </TextField>
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setEditTarget(null)} disabled={savingEdit}>Cancelar</Button>
          <Button onClick={() => void saveEdit()} variant="contained" disabled={savingEdit || !editForm.fullName.trim()}>
            {savingEdit ? 'Salvando…' : 'Salvar'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={importOpen} onClose={() => setImportOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Importar inscrições e doações</DialogTitle>
        <DialogContent dividers>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Alert severity="info">
              <strong>Inscrições</strong> — uma por linha, campos separados por <code>;</code>:<br />
              <code>id;nome;email;telefone;nascimento;modalidade;camisa;status;metodo;criado_em</code><br />
              Ex.: <code>32;Daniela de S Domingos;email@x.com;;1983-05-26;3KM;G;PAGA;PIX;</code><br />
              id vazio = automático · status: PENDENTE ou PAGA · metodo: PIX ou CARTAO · campos podem ficar vazios.<br />
              <strong>Dica:</strong> para recuperar inscrições antigas, importe como PENDENTE e depois use
              "Sincronizar com MP" em cada uma — o Mercado Pago confirma método, valor e quem realmente pagou.
            </Alert>
            <TextField multiline minRows={8} fullWidth label="Inscrições" value={importText}
              onChange={(e) => setImportText(e.target.value)}
              placeholder={'32;Daniela de S Domingos;ddpaulino83@gmail.com;;1983-05-26;3KM;G;PAGA;PIX;'} />
            <Alert severity="info">
              <strong>Doações avulsas</strong> — uma por linha: <code>valor;nome;contato;observação</code><br />
              Ex.: <code>2500,00;Brechó;;;Doação do brechó</code>
            </Alert>
            <TextField multiline minRows={3} fullWidth label="Doações" value={donationsText}
              onChange={(e) => setDonationsText(e.target.value)}
              placeholder={'2500,00;Brechó;;;Doação do brechó\n5,00;Doação pessoal;;;'} />
            {importResult && (
              <Alert severity={importResult.errors.length ? 'warning' : 'success'}>
                {importResult.created} criado(s), {importResult.skipped} ignorado(s).
                {importResult.errors.length > 0 && (
                  <Box component="ul" sx={{ m: 0, pl: 2 }}>
                    {importResult.errors.map((e, i) => <li key={i}>{e}</li>)}
                  </Box>
                )}
              </Alert>
            )}
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setImportOpen(false)} disabled={importing}>Fechar</Button>
          <Button onClick={() => void submitImport()} variant="contained" disabled={importing}>
            {importing ? 'Importando…' : 'Importar'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={mpOpen} onClose={() => setMpOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Pagamentos aprovados no Mercado Pago</DialogTitle>
        <DialogContent dividers>
          {mpLoading ? (
            <Stack alignItems="center" sx={{ py: 4 }}><CircularProgress /></Stack>
          ) : mpPayments.length === 0 ? (
            <Alert severity="info">Nenhum pagamento aprovado encontrado (ou gateway indisponível).</Alert>
          ) : (
            <Stack spacing={0.5} sx={{ mt: 1 }}>
              {mpPayments.map((p, i) => (
                <Typography key={i} variant="body2">
                  <strong>ref. {p.externalReference ?? '—'}</strong>
                  {' · '}{p.dateCreated ? new Date(p.dateCreated).toLocaleString('pt-BR') : '—'}
                  {' · '}{p.amount != null ? fmtBRL(p.amount) : '—'}
                  {' · '}{p.paymentMethod ?? '—'}
                  {' · '}{p.payerName ?? p.payerEmail ?? '—'}
                  {' · id '}{p.paymentId ?? '—'}
                </Typography>
              ))}
            </Stack>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setMpOpen(false)}>Fechar</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default RunRegistrationsPage

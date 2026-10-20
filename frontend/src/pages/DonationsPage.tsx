import { FormEvent, useCallback, useEffect, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Checkbox,
  Chip,
  CircularProgress,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
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
import VolunteerActivismIcon from '@mui/icons-material/VolunteerActivism'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import donationService, { Donation, DonationStatus, PixConfiguration } from '../services/donationService'
import cashService, { CashSummary } from '../services/cashService'
import stockService, { Product } from '../services/stockService'
import { getApiErrorMessage } from '../utils/apiError'
import { newRequestReference } from '../utils/idempotency'
import ThemeToggle from '../components/ThemeToggle'

const statusLabel: Record<DonationStatus, string> = {
  CRIADA: 'Criada',
  AGUARDANDO_PAGAMENTO: 'Aguardando pagamento',
  PAGA: 'Paga',
  CANCELADA: 'Cancelada',
  EXPIRADA: 'Expirada',
  CONFIRMADA_MANUALMENTE: 'Confirmada manualmente',
}

type ConfirmationAction = 'confirm' | 'cancel' | 'expire'

function DonationsPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const isMobile = useMediaQuery('(max-width:599px)')
  const isAdmin = Boolean(user?.roles.includes('ROLE_ADMIN'))
  const [donations, setDonations] = useState<Donation[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [configuration, setConfiguration] = useState<PixConfiguration | null>(null)
  const [filter, setFilter] = useState<DonationStatus | 'TODAS'>('TODAS')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [action, setAction] = useState<{ donation: Donation; type: ConfirmationAction } | null>(null)
  const [productId, setProductId] = useState('')
  const [quantity, setQuantity] = useState('1')
  const [donorName, setDonorName] = useState('')
  const [anonymous, setAnonymous] = useState(false)
  const [observation, setObservation] = useState('')
  const [requestReference, setRequestReference] = useState(() => newRequestReference())
  const [pixKey, setPixKey] = useState('')
  const [merchantName, setMerchantName] = useState('')
  const [merchantCity, setMerchantCity] = useState('')
  const [clearPixKey, setClearPixKey] = useState(false)
  const [cash, setCash] = useState<CashSummary | null>(null)
  const [cashOpen, setCashOpen] = useState(false)
  const [cashType, setCashType] = useState<'ENTRADA' | 'SAIDA'>('SAIDA')
  const [cashAmount, setCashAmount] = useState('')
  const [cashDescription, setCashDescription] = useState('')
  const [cashFile, setCashFile] = useState<File | null>(null)

  const loadData = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [donationResult, productResult] = await Promise.all([
        donationService.list(),
        stockService.listProducts(),
      ])
      setDonations(donationResult)
      setProducts(productResult)
      setCash(await cashService.summary().catch(() => null))
      if (isAdmin) {
        const pix = await donationService.getPixConfiguration()
        setConfiguration(pix)
        setMerchantName(pix.merchantName)
        setMerchantCity(pix.merchantCity)
      }
    } catch (loadError) {
      setError(getApiErrorMessage(loadError, 'Não foi possível carregar as doações.'))
    } finally {
      setLoading(false)
    }
  }, [isAdmin])

  useEffect(() => {
    void loadData()
  }, [loadData])

  const createProductDonation = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      await donationService.createProductDonation({
        productId: Number(productId),
        quantity: Number(quantity),
        donorName: anonymous ? undefined : donorName.trim() || undefined,
        anonymous,
        observation: observation.trim() || undefined,
        externalReference: requestReference,
      })
      setMessage('Doação de produto registrada e entrada de estoque lançada no mesmo processo.')
      setProductId('')
      setQuantity('1')
      setDonorName('')
      setAnonymous(false)
      setObservation('')
      setRequestReference(newRequestReference())
      await loadData()
    } catch (saveError) {
      setError(getApiErrorMessage(saveError, 'Não foi possível registrar a doação de produto.'))
    } finally {
      setSaving(false)
    }
  }

  const savePixConfiguration = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      const updated = await donationService.updatePixConfiguration({
        pixKey: pixKey.trim() || undefined,
        merchantName: merchantName.trim() || undefined,
        merchantCity: merchantCity.trim() || undefined,
        clearPixKey,
      })
      setConfiguration(updated)
      setPixKey('')
      setClearPixKey(false)
      setMessage(updated.configured ? 'Configuração PIX salva. A chave completa não é exibida novamente.' : 'Configuração salva; PIX não está habilitado.')
    } catch (saveError) {
      setError(getApiErrorMessage(saveError, 'Não foi possível salvar a configuração PIX.'))
    } finally {
      setSaving(false)
    }
  }

  const performAction = async () => {
    if (!action) return
    setSaving(true)
    setError('')
    try {
      if (action.type === 'confirm') await donationService.confirmManually(action.donation.id)
      if (action.type === 'cancel') await donationService.cancel(action.donation.id)
      if (action.type === 'expire') await donationService.expire(action.donation.id)
      const actionLabel = action.type === 'confirm' ? 'confirmação manual registrada' : action.type === 'cancel' ? 'doação cancelada' : 'doação expirada'
      setMessage(`${actionLabel}. O código PIX, por si só, não é prova de pagamento.`)
      setAction(null)
      await loadData()
    } catch (actionError) {
      setError(getApiErrorMessage(actionError, 'Não foi possível atualizar o estado da doação.'))
      setAction(null)
    } finally {
      setSaving(false)
    }
  }

  const saveCashMovement = async () => {
    const value = Number(cashAmount.replace(',', '.'))
    if (!cashAmount.trim() || Number.isNaN(value) || value <= 0) {
      setError('Informe um valor maior que zero.')
      return
    }
    if (!cashDescription.trim()) {
      setError('Descreva a movimentação.')
      return
    }
    setSaving(true)
    setError('')
    try {
      await cashService.create(cashType, value, cashDescription.trim(), cashFile || undefined)
      setMessage(cashType === 'SAIDA'
        ? 'Saída registrada e descontada do total. Se foi compra de itens da lista, lembre de cadastrar as quantidades no Estoque.'
        : 'Entrada registrada e somada ao total em caixa.')
      setCashOpen(false)
      setCashAmount('')
      setCashDescription('')
      setCashFile(null)
      await loadData()
    } catch (saveError) {
      setError(getApiErrorMessage(saveError, 'Não foi possível registrar a movimentação.'))
    } finally {
      setSaving(false)
    }
  }

  const downloadCashAttachment = async (id: number) => {
    try {
      const { blob, filename } = await cashService.downloadAttachment(id)
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename
      link.click()
      URL.revokeObjectURL(url)
    } catch (downloadError) {
      setError(getApiErrorMessage(downloadError, 'Não foi possível baixar o anexo.'))
    }
  }

  const filtered = donations.filter((donation) => filter === 'TODAS' || donation.status === filter)
  const selectedProduct = products.find((product) => product.id === Number(productId))

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default', pb: 5, overflowX: 'hidden' }}>
      <Box sx={{ bgcolor: 'primary.main', color: 'white' }}>
        <Toolbar>
          <IconButton color="inherit" onClick={() => navigate('/modulos')} aria-label="Voltar ao painel"><ArrowBackIcon /></IconButton>
          <VolunteerActivismIcon sx={{ mr: 1 }} />
          <Typography variant="h6" sx={{ flexGrow: 1 }}>Doações</Typography>
          <ThemeToggle />
        </Toolbar>
      </Box>
      <Container maxWidth="xl" sx={{ mt: 3 }}>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        {message && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setMessage('')}>{message}</Alert>}

        {isAdmin && (
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h5" color="primary" gutterBottom>Configuração PIX</Typography>
              {configuration && (
                <Alert severity={configuration.configured ? 'success' : 'warning'} sx={{ mb: 2 }}>
                  {configuration.configured
                    ? `PIX configurado — chave ${configuration.maskedPixKey}. Nunca compartilhe a chave completa em logs ou telas públicas.`
                    : 'PIX_NAO_CONFIGURADO — informe a chave oficial e os dados do recebedor para habilitar o código PIX.'}
                </Alert>
              )}
              <Box component="form" onSubmit={savePixConfiguration}>
                <Grid container spacing={2}>
                  <Grid item xs={12} md={4}>
                    <TextField fullWidth type="password" label="Chave PIX oficial" value={pixKey} onChange={(event) => setPixKey(event.target.value)} helperText="Em branco mantém a chave atual." inputProps={{ maxLength: 200 }} />
                  </Grid>
                  <Grid item xs={12} md={3}>
                    <TextField required fullWidth label="Nome do recebedor" value={merchantName} onChange={(event) => setMerchantName(event.target.value)} inputProps={{ maxLength: 25 }} />
                  </Grid>
                  <Grid item xs={12} md={3}>
                    <TextField required fullWidth label="Cidade do recebedor" value={merchantCity} onChange={(event) => setMerchantCity(event.target.value)} inputProps={{ maxLength: 15 }} />
                  </Grid>
                  <Grid item xs={12} md={2}>
                    <Stack sx={{ height: '100%' }} justifyContent="center">
                      <Box sx={{ display: 'flex', alignItems: 'center' }}>
                        <Checkbox checked={clearPixKey} onChange={(event) => setClearPixKey(event.target.checked)} />
                        <Typography variant="body2">Remover chave</Typography>
                      </Box>
                      <Button type="submit" variant="contained" disabled={saving}>Salvar configuração</Button>
                    </Stack>
                  </Grid>
                </Grid>
              </Box>
              <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 1 }}>
                Alterações são auditadas sem registrar a chave. A chave persistida fica restrita à configuração administrativa.
              </Typography>
            </CardContent>
          </Card>
        )}

        {cash && (
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} gap={2}>
                <Box>
                  <Typography variant="h5" color="primary" gutterBottom>Caixa do projeto</Typography>
                  <Typography variant="h4" fontWeight={700}>R$ {cash.total.toFixed(2)}</Typography>
                  <Typography variant="caption" color="text.secondary">
                    Doações confirmadas R$ {cash.donationsTotal.toFixed(2)} · outras movimentações R$ {cash.movementsBalance.toFixed(2)}
                  </Typography>
                </Box>
                <Button variant="contained" onClick={() => setCashOpen(true)}>Registrar entrada ou saída</Button>
              </Stack>
              {cash.movements.length > 0 && (
                <Stack spacing={1} sx={{ mt: 2 }}>
                  <Typography variant="subtitle2" color="text.secondary">Transparência — movimentações</Typography>
                  {cash.movements.map((m) => (
                    <Stack key={m.id} direction={{ xs: 'column', sm: 'row' }} alignItems={{ sm: 'center' }} gap={1}
                      sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1, p: 1 }}>
                      <Chip size="small" label={m.type === 'ENTRADA' ? 'Entrada' : 'Saída'}
                        color={m.type === 'ENTRADA' ? 'success' : 'error'} />
                      <Typography fontWeight={700}>{m.type === 'SAIDA' ? '-' : '+'} R$ {m.amount.toFixed(2)}</Typography>
                      <Typography variant="body2" sx={{ flexGrow: 1 }}>{m.description}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        {m.createdBy} · {new Date(m.createdAt).toLocaleString('pt-BR')}
                      </Typography>
                      {m.hasAttachment && (
                        <Button size="small" onClick={() => void downloadCashAttachment(m.id)}>Baixar comprovante</Button>
                      )}
                    </Stack>
                  ))}
                </Stack>
              )}
            </CardContent>
          </Card>
        )}

        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Typography variant="h5" color="primary" gutterBottom>Registrar doação de produto</Typography>
            {products.every((product) => !product.active) && (
              <Alert severity="info" sx={{ mb: 2 }}>Cadastre e ative um produto no estoque antes de registrar uma doação de produto.</Alert>
            )}
            <Box component="form" onSubmit={createProductDonation}>
              <Grid container spacing={2} alignItems="center">
                <Grid item xs={12} sm={6} md={3}>
                  <FormControl fullWidth required>
                    <InputLabel>Produto</InputLabel>
                    <Select value={productId} label="Produto" onChange={(event) => setProductId(event.target.value)}>
                      {products.filter((product) => product.active).map((product) => (
                        <MenuItem key={product.id} value={product.id}>{product.name} — {product.currentQuantity} {product.unit}</MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>
                <Grid item xs={6} sm={3} md={2}>
                  <TextField required fullWidth type="number" label={`Quantidade${selectedProduct ? ` (${selectedProduct.unit})` : ''}`} value={quantity} onChange={(event) => setQuantity(event.target.value)} inputProps={{ min: 1, step: 1 }} />
                </Grid>
                <Grid item xs={6} sm={3} md={3}>
                  <TextField fullWidth label="Doador (opcional)" value={donorName} disabled={anonymous} onChange={(event) => setDonorName(event.target.value)} inputProps={{ maxLength: 255 }} />
                </Grid>
                <Grid item xs={12} md={4}>
                  <TextField fullWidth label="Observação" value={observation} onChange={(event) => setObservation(event.target.value)} inputProps={{ maxLength: 2000 }} />
                </Grid>
                <Grid item xs={12}>
                  <Stack direction={{ xs: 'column', sm: 'row' }} alignItems={{ xs: 'stretch', sm: 'center' }} spacing={1}>
                    <Box sx={{ display: 'flex', alignItems: 'center', mr: 'auto' }}>
                      <Checkbox checked={anonymous} onChange={(event) => setAnonymous(event.target.checked)} />
                      <Typography>Doação anônima</Typography>
                    </Box>
                    <Button type="submit" variant="contained" disabled={saving || products.every((product) => !product.active)}>
                      {saving ? 'Registrando...' : 'Registrar doação e entrada no estoque'}
                    </Button>
                  </Stack>
                </Grid>
              </Grid>
            </Box>
          </CardContent>
        </Card>

        <Stack direction={{ xs: 'column', sm: 'row' }} alignItems={{ sm: 'center' }} justifyContent="space-between" gap={2} sx={{ mb: 2 }}>
          <Typography variant="h5" color="primary">Histórico de doações</Typography>
          <FormControl sx={{ minWidth: { xs: '100%', sm: 260 } }}>
            <InputLabel>Filtrar por status</InputLabel>
            <Select value={filter} label="Filtrar por status" onChange={(event) => setFilter(event.target.value as DonationStatus | 'TODAS')}>
              <MenuItem value="TODAS">Todas</MenuItem>
              {Object.entries(statusLabel).map(([value, label]) => <MenuItem value={value} key={value}>{label}</MenuItem>)}
            </Select>
          </FormControl>
        </Stack>

        {loading ? (
          <Box sx={{ textAlign: 'center', py: 6 }}><CircularProgress /></Box>
        ) : filtered.length === 0 ? (
          <Alert severity="info">Nenhuma doação encontrada para este filtro.</Alert>
        ) : (
          <Grid container spacing={2}>
            {filtered.map((donation) => (
              <Grid item xs={12} md={6} xl={4} key={donation.id}>
                <Card>
                  <CardContent>
                    <Stack direction="row" justifyContent="space-between" gap={1}>
                      <Typography variant="h6">Doação #{donation.id}</Typography>
                      <Chip size="small" label={statusLabel[donation.status]} color={donation.status === 'AGUARDANDO_PAGAMENTO' ? 'warning' : donation.status === 'CONFIRMADA_MANUALMENTE' ? 'success' : 'default'} />
                    </Stack>
                    <Typography color="text.secondary">{donation.type === 'FINANCEIRA' ? 'Financeira' : 'Produto'} · {new Date(donation.createdAt).toLocaleString('pt-BR')}</Typography>
                    {donation.type === 'FINANCEIRA'
                      ? <Typography sx={{ mt: 1, fontWeight: 700 }}>R$ {donation.amount?.toFixed(2)}</Typography>
                      : <Typography sx={{ mt: 1, fontWeight: 700 }}>{donation.quantity} {donation.unit} de {donation.productName}</Typography>}
                    <Typography variant="body2">Doador: {donation.anonymous ? 'Anônimo' : donation.donorName || 'Não informado'}</Typography>
                    {donation.observation && <Typography variant="body2" color="text.secondary">Observação: {donation.observation}</Typography>}
                    {donation.confirmedAt && (
                      <Typography variant="caption" display="block" color="text.secondary">
                        {donation.status === 'CONFIRMADA_MANUALMENTE' ? 'Confirmação manual' : 'Atualizado'} por {donation.confirmedBy || '—'} · {new Date(donation.confirmedAt).toLocaleString('pt-BR')}
                      </Typography>
                    )}
                    {isAdmin && donation.status === 'AGUARDANDO_PAGAMENTO' && (
                      <Stack direction="row" flexWrap="wrap" gap={1} sx={{ mt: 2 }}>
                        <Button size="small" variant="contained" onClick={() => setAction({ donation, type: 'confirm' })}>Confirmar manualmente</Button>
                        <Button size="small" color="warning" onClick={() => setAction({ donation, type: 'cancel' })}>Cancelar</Button>
                        <Button size="small" onClick={() => setAction({ donation, type: 'expire' })}>Expirar</Button>
                      </Stack>
                    )}
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        )}
      </Container>

      <Dialog open={cashOpen} onClose={() => !saving && setCashOpen(false)} fullWidth maxWidth="sm" fullScreen={isMobile}>
        <DialogTitle>Registrar movimentação de caixa</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <FormControl fullWidth>
              <InputLabel>Tipo</InputLabel>
              <Select value={cashType} label="Tipo" onChange={(e) => setCashType(e.target.value as 'ENTRADA' | 'SAIDA')}>
                <MenuItem value="ENTRADA">Entrada (entrou dinheiro)</MenuItem>
                <MenuItem value="SAIDA">Saída (gasto do projeto)</MenuItem>
              </Select>
            </FormControl>
            <TextField required fullWidth type="number" label="Valor (R$)" value={cashAmount}
              onChange={(e) => setCashAmount(e.target.value)} inputProps={{ min: 0.01, step: '0.01' }} />
            <TextField required fullWidth multiline minRows={2} label="Descrição" value={cashDescription}
              onChange={(e) => setCashDescription(e.target.value)}
              helperText="Ex.: compra de itens da cesta, gasolina da coleta, doação em dinheiro"
              inputProps={{ maxLength: 1000 }} />
            <Button component="label" variant="outlined">
              {cashFile ? `Anexo: ${cashFile.name}` : 'Anexar comprovante (PDF, JPG ou PNG — até 5 MB)'}
              <input hidden type="file" accept="application/pdf,image/jpeg,image/png"
                onChange={(e) => setCashFile(e.target.files?.[0] || null)} />
            </Button>
            {cashType === 'SAIDA' && (
              <Alert severity="info">
                Se a saída for compra de itens da lista, lembre de cadastrar as quantidades no Estoque
                para o controle bater. O comprovante anexado vai por e-mail ao endereço do projeto
                e fica disponível no quadro de transparência.
              </Alert>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCashOpen(false)} disabled={saving}>Cancelar</Button>
          <Button variant="contained" onClick={() => void saveCashMovement()} disabled={saving}>
            {saving ? 'Registrando...' : 'Registrar'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={Boolean(action)} onClose={() => !saving && setAction(null)} fullWidth maxWidth="xs" fullScreen={isMobile}>
        <DialogTitle>
          {action?.type === 'confirm' ? 'Confirmar manualmente?' : action?.type === 'cancel' ? 'Cancelar doação?' : 'Expirar doação?'}
        </DialogTitle>
        <DialogContent>
          <Typography>
            {action?.type === 'confirm'
              ? 'Confirme somente após verificar o recebimento por um canal externo. Esta ação registra confirmação manual, não confirmação bancária automática.'
              : 'A alteração será auditada e não poderá ser desfeita por esta tela.'}
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAction(null)} disabled={saving}>Voltar</Button>
          <Button variant="contained" onClick={() => void performAction()} disabled={saving}>Confirmar ação</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default DonationsPage

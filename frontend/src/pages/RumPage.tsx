import { useEffect, useState } from 'react'
import {
  Alert,
  AppBar,
  Box,
  Button,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  Paper,
  Stack,
  TextField,
  Toolbar,
  Typography,
} from '@mui/material'
import DirectionsRunIcon from '@mui/icons-material/DirectionsRun'
import DirectionsWalkIcon from '@mui/icons-material/DirectionsWalk'
import EmojiEventsIcon from '@mui/icons-material/EmojiEvents'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import { useNavigate } from 'react-router-dom'
import runService, { RunInfo, RunRegistration, RunStatus } from '../services/runService'
import { getApiErrorMessage } from '../utils/apiError'

const SHIRT_SIZES = ['P', 'M', 'G', 'GG', 'XG']

function fmtBRL(value: number): string {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

function RumPage() {
  const navigate = useNavigate()
  const [info, setInfo] = useState<RunInfo | null>(null)
  const [open, setOpen] = useState<string | null>(null) // modalidade escolhida
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [shirtSize, setShirtSize] = useState('')
  const [extraDonation, setExtraDonation] = useState('')
  const [method, setMethod] = useState<'PIX' | 'CARTAO'>('PIX')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')
  const [registration, setRegistration] = useState<RunRegistration | null>(null)
  const [status, setStatus] = useState<RunStatus | null>(null)

  useEffect(() => {
    runService.info().then(setInfo).catch(() => {})
  }, [])

  // Depois de inscrito, verifica o status periodicamente até confirmar.
  useEffect(() => {
    if (!registration || status?.status === 'PAGA') return
    const timer = setInterval(async () => {
      try {
        setStatus(await runService.status(registration.id))
      } catch { /* tenta de novo no próximo ciclo */ }
    }, 5000)
    return () => clearInterval(timer)
  }, [registration, status?.status])

  const openDialog = (modality: string) => {
    setOpen(modality)
    setError('')
    setRegistration(null)
    setStatus(null)
    setName(''); setEmail(''); setPhone(''); setShirtSize(''); setExtraDonation('')
    setMethod('PIX')
  }

  const submit = async () => {
    if (!open) return
    if (!name.trim()) { setError('Informe seu nome completo.'); return }
    if (!email.trim() && !phone.trim()) { setError('Informe e-mail ou telefone para contato.'); return }
    setSending(true)
    setError('')
    try {
      const extra = Number(extraDonation.replace(',', '.'))
      const created = await runService.register({
        fullName: name.trim(),
        email: email.trim() || undefined,
        phone: phone.trim() || undefined,
        modality: open,
        shirtSize: shirtSize || undefined,
        paymentMethod: method,
        ...(Number.isFinite(extra) && extra > 0 ? { extraDonation: extra } : {}),
      })
      setRegistration(created)
      if (created.checkoutUrl) {
        window.location.href = created.checkoutUrl
      }
    } catch (submitError) {
      setError(getApiErrorMessage(submitError, 'Não foi possível registrar a inscrição.'))
    } finally {
      setSending(false)
    }
  }

  const copyPix = () => {
    if (!registration?.pixCode) return
    if (navigator.clipboard) {
      navigator.clipboard.writeText(registration.pixCode).catch(() => fallbackCopy(registration.pixCode!))
    } else {
      fallbackCopy(registration.pixCode)
    }
  }

  // HTTP fora de localhost não tem clipboard API — fallback com textarea
  const fallbackCopy = (text: string) => {
    const area = document.createElement('textarea')
    area.value = text
    area.style.position = 'fixed'
    area.style.opacity = '0'
    document.body.appendChild(area)
    area.select()
    try { document.execCommand('copy') } catch { /* usuário copia manualmente */ }
    document.body.removeChild(area)
  }

  const paid = status?.status === 'PAGA'

  return (
    <Box component="main" sx={{ bgcolor: '#FBF9F1', minHeight: '100vh' }}>
      <AppBar position="sticky" elevation={1} sx={{ bgcolor: '#1B2A6B' }}>
        <Toolbar sx={{ gap: 2 }}>
          <Box
            component="img"
            src="/logo-mercado-solidario.png"
            alt="Logo do Mercado Solidário Petrópolis"
            sx={{ height: { xs: 44, md: 56 }, width: 'auto', cursor: 'pointer' }}
            onClick={() => navigate('/')}
          />
          <Typography sx={{ color: '#FFC42E', fontWeight: 800, letterSpacing: 1 }}>
            MERCADO SOLIDÁRIO RUN
          </Typography>
          <Box sx={{ flexGrow: 1 }} />
        </Toolbar>
      </AppBar>

      {/* Cartaz oficial */}
      <Box sx={{ bgcolor: '#1B2A6B', pt: 3, pb: { xs: 3, md: 4 }, textAlign: 'center' }}>
        <Box
          component="img"
          src="/rum-principal-horizontal.jpg"
          alt="Cartaz oficial Mercado Solidário Run — 13 de dezembro — 3KM caminhada e 6KM corrida"
          sx={{
            display: { xs: 'none', md: 'block' },
            mx: 'auto', maxWidth: 980, width: '100%', borderRadius: 3,
            boxShadow: '0 18px 50px rgba(0,0,0,.45)',
          }}
        />
        <Box
          component="img"
          src="/rum-principal-vertical.jpg"
          alt="Cartaz oficial Mercado Solidário Run — 13 de dezembro — 3KM caminhada e 6KM corrida"
          sx={{
            display: { xs: 'block', md: 'none' },
            mx: 'auto', maxWidth: 420, width: '92%', borderRadius: 3,
            boxShadow: '0 18px 50px rgba(0,0,0,.45)',
          }}
        />
      </Box>

      <Box
        sx={{
          bgcolor: '#1B2A6B', color: '#FFFFFF', pb: { xs: 7, md: 9 },
          textAlign: 'center', px: 2,
        }}
      >
        <Typography
          variant="h4" component="h1"
          sx={{ color: '#FFC42E', fontWeight: 800, letterSpacing: 1, textTransform: 'uppercase' }}
        >
          Corra por um propósito maior
        </Typography>
        <Typography sx={{ color: '#EAF0FF', mt: 1, fontSize: '1.1rem' }}>
          <strong>13 de dezembro</strong> · Kit do atleta: camisa + medalha ·{' '}
          <strong>{info ? fmtBRL(info.amount) : 'R$ 100,00'}</strong> por inscrição
        </Typography>
        <Typography sx={{ color: '#EAF0FF', mt: 0.5 }}>
          100% da arrecadação revertida para o Mercado Solidário.
        </Typography>
      </Box>

      <Container maxWidth="md" sx={{ py: { xs: 6, md: 8 } }}>
        <Typography variant="h4" component="h2" sx={{ color: '#1B2A6B', fontWeight: 800, mb: 4, textAlign: 'center' }}>
          Escolha seu percurso
        </Typography>
        <Grid container spacing={3} justifyContent="center">
          {info?.modalities.map((modality) => (
            <Grid item xs={12} sm={6} key={modality.modality}>
              <Paper
                elevation={0}
                sx={{
                  p: 4, textAlign: 'center', height: '100%',
                  border: '1px solid #E3E8EF',
                  borderTop: modality.modality === '3KM' ? '4px solid #FFC42E' : '4px solid #19B8D2',
                  borderRadius: 3, bgcolor: '#FFFFFF',
                }}
              >
                {modality.modality === '3KM'
                  ? <DirectionsWalkIcon sx={{ fontSize: 52, color: '#FFC42E', mb: 1 }} />
                  : <DirectionsRunIcon sx={{ fontSize: 52, color: '#19B8D2', mb: 1 }} />}
                <Typography variant="h5" sx={{ fontWeight: 800, color: '#1B2A6B' }}>
                  {modality.label}
                </Typography>
                <Typography variant="body2" sx={{ color: '#44506B', mt: 1 }}>
                  {modality.remaining > 0
                    ? `${modality.remaining} de ${modality.slots} vagas disponíveis`
                    : 'Vagas esgotadas'}
                </Typography>
                <Button
                  variant="contained"
                  disabled={modality.remaining <= 0}
                  onClick={() => openDialog(modality.modality)}
                  sx={{
                    mt: 3, fontWeight: 700,
                    bgcolor: '#1B2A6B', '&:hover': { bgcolor: '#2E4FA3' },
                  }}
                >
                  Inscrever-se — {fmtBRL(info.amount)}
                </Button>
              </Paper>
            </Grid>
          )) ?? (
            <Typography sx={{ color: '#44506B' }}>Carregando modalidades…</Typography>
          )}
        </Grid>

        {/* Kit do Atleta — imagem oficial chega do Pastor e entra em /rum-kit.jpg */}
        <Paper
          elevation={0}
          sx={{ mt: 6, p: 4, textAlign: 'center', borderRadius: 3, bgcolor: '#FFFFFF', border: '1px solid #E3E8EF' }}
        >
          <Typography variant="h5" sx={{ fontWeight: 800, color: '#1B2A6B', mb: 1 }}>
            Kit do Atleta
          </Typography>
          <Typography sx={{ color: '#44506B', mb: 3 }}>
            Todo inscrito recebe <strong>camisa oficial + medalha</strong>,
            em qualquer modalidade.
          </Typography>
          <Box
            component="img"
            src="/rum-kit-horizontal.jpg"
            alt="Kit do atleta Mercado Solidário Run — camisa oficial e medalha"
            sx={{
              display: { xs: 'none', md: 'block' },
              mx: 'auto', maxWidth: 720, width: '100%', borderRadius: 2,
              boxShadow: '0 12px 34px rgba(27,42,107,.18)',
            }}
          />
          <Box
            component="img"
            src="/rum-kit-vertical.jpg"
            alt="Kit do atleta Mercado Solidário Run — camisa oficial e medalha"
            sx={{
              display: { xs: 'block', md: 'none' },
              mx: 'auto', maxWidth: 380, width: '92%', borderRadius: 2,
              boxShadow: '0 12px 34px rgba(27,42,107,.18)',
            }}
          />
        </Paper>

        <Paper
          elevation={0}
          sx={{ mt: 6, p: 4, textAlign: 'center', borderRadius: 3, bgcolor: '#FFF6D6', border: '2px solid #FFC42E' }}
        >
          <EmojiEventsIcon sx={{ color: '#19B8D2', mr: 1, verticalAlign: 'middle' }} />
          <Typography variant="h6" component="span" sx={{ fontWeight: 800, color: '#1B2A6B' }}>
            Seus passos alimentam sonhos e transformam realidades.
          </Typography>
          <Typography sx={{ color: '#44506B', mt: 1 }}>
            Toda a arrecadação do Mercado Solidário Run ajuda a alcançar a meta de
            500 famílias assistidas em 2026.
          </Typography>
        </Paper>
      </Container>

      <Box component="footer" sx={{ bgcolor: '#1B2A6B', color: '#EAF0FF', py: 3, textAlign: 'center' }}>
        <Typography variant="body2">Mercado Solidário Petrópolis — 2026</Typography>
      </Box>

      {/* Diálogo de inscrição */}
      <Dialog open={open !== null} onClose={() => !sending && setOpen(null)} maxWidth="xs" fullWidth>
        <DialogTitle sx={{ fontWeight: 800, color: '#1B2A6B' }}>
          Inscrição {open === '3KM' ? '3 km — caminhada' : '6 km — corrida'}
        </DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
          {registration && paid ? (
            <>
              <Alert severity="success" icon={<CheckCircleIcon />}>
                <strong>Inscrição confirmada!</strong> Pagamento aprovado —
                inscrição nº {registration.id}. Nos vemos no dia 13 de dezembro!
              </Alert>
              {status?.checkinCode && (
                <Button
                  variant="contained"
                  onClick={() => navigate(`/rum/verificar/${status.checkinCode}`)}
                  sx={{ bgcolor: '#1B2A6B', '&:hover': { bgcolor: '#2E4FA3' }, fontWeight: 700 }}
                >
                  Ver meu QR de retirada do kit
                </Button>
              )}
            </>
          ) : registration ? (
            <>
              <Alert severity="info">
                Inscrição criada! Pague para confirmar sua vaga.
              </Alert>
              {registration.pixCode ? (
                <>
                  <Typography variant="body2" sx={{ color: '#44506B' }}>
                    PIX de <strong>{fmtBRL(registration.totalCharged)}</strong> — aponte a
                    câmera no QR ou copie o código e pague no app do seu banco.
                  </Typography>
                  {registration.pixQrUri && (
                    <Box
                      component="img"
                      src={registration.pixQrUri}
                      alt={`QR Code PIX de ${fmtBRL(registration.totalCharged)}`}
                      sx={{ mx: 'auto', width: 200, height: 200, borderRadius: 2, border: '1px solid #E3E8EF' }}
                    />
                  )}
                  <TextField
                    fullWidth
                    multiline
                    rows={3}
                    label="Código PIX (segure para selecionar e copiar)"
                    value={registration.pixCode}
                    InputProps={{ readOnly: true }}
                    inputProps={{ style: { fontSize: 12 } }}
                    onFocus={(e) => e.target.select()}
                  />
                  <Button startIcon={<ContentCopyIcon />} onClick={copyPix} variant="outlined" fullWidth>
                    Copiar código PIX
                  </Button>
                  <Typography variant="caption" sx={{ color: '#44506B' }}>
                    Após pagar, envie o comprovante — a equipe confirma e sua vaga fica garantida.
                  </Typography>
                </>
              ) : (
                <Typography variant="body2" sx={{ color: '#44506B' }}>
                  Total: {fmtBRL(registration.totalCharged)}. A confirmação é manual —
                  pague pela chave PIX oficial do projeto e aguarde a equipe confirmar.
                </Typography>
              )}
            </>
          ) : (
            <>
              {info && (
                <Typography variant="body2" sx={{ color: '#44506B' }}>
                  Inscrição: <strong>{fmtBRL(info.amount)}</strong> — inclui camisa e medalha.
                </Typography>
              )}
              <TextField required fullWidth label="Nome completo" value={name}
                onChange={(e) => setName(e.target.value)} />
              <TextField fullWidth label="E-mail" type="email" value={email}
                onChange={(e) => setEmail(e.target.value)} />
              <TextField fullWidth label="Telefone/WhatsApp" value={phone}
                onChange={(e) => setPhone(e.target.value)} />
              <TextField
                fullWidth select label="Tamanho da camisa" value={shirtSize}
                onChange={(e) => setShirtSize(e.target.value)}
                SelectProps={{ native: true }}
              >
                <option value="">Escolha…</option>
                {SHIRT_SIZES.map((size) => <option key={size} value={size}>{size}</option>)}
              </TextField>
              <TextField
                fullWidth type="number" label="Doação extra (opcional, em R$)"
                value={extraDonation}
                onChange={(e) => setExtraDonation(e.target.value)}
                inputProps={{ min: 0, step: 1 }}
                helperText="Quer ajudar além da inscrição? Some um valor aqui — 100% vai para as famílias."
              />
              <Stack direction="row" spacing={1}>
                <Button
                  variant={method === 'PIX' ? 'contained' : 'outlined'}
                  onClick={() => setMethod('PIX')} fullWidth
                >
                  PIX
                </Button>
                <Button
                  variant={method === 'CARTAO' ? 'contained' : 'outlined'}
                  onClick={() => setMethod('CARTAO')} fullWidth
                  disabled={!info?.cardEnabled}
                >
                  Cartão{!info?.cardEnabled ? ' (em breve)' : ''}
                </Button>
              </Stack>
              {error && <Alert severity="error">{error}</Alert>}
            </>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          {registration ? (
            <Button onClick={() => setOpen(null)} variant="contained">Fechar</Button>
          ) : (
            <>
              <Button onClick={() => setOpen(null)} disabled={sending}>Cancelar</Button>
              <Button onClick={() => void submit()} variant="contained" disabled={sending}>
                {sending ? 'Registrando…' : 'Confirmar inscrição'}
              </Button>
            </>
          )}
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default RumPage

import { useEffect, useRef, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Container,
  IconButton,
  Stack,
  TextField,
  Toolbar,
  Typography,
} from '@mui/material'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import QrCodeScannerIcon from '@mui/icons-material/QrCodeScanner'
import { Html5Qrcode } from 'html5-qrcode'
import { useNavigate } from 'react-router-dom'
import runService, { RunVerify } from '../services/runService'
import { getApiErrorMessage } from '../utils/apiError'
import ThemeToggle from '../components/ThemeToggle'

// Validação do QR do inscrito pela equipe logada: mostra nome, modalidade e
// tamanho da camisa; registra a retirada do kit com o usuário logado.
function RunKitScanPage() {
  const navigate = useNavigate()
  const [code, setCode] = useState('')
  const [verify, setVerify] = useState<RunVerify | null>(null)
  const [loading, setLoading] = useState(false)
  const [delivering, setDelivering] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [scanning, setScanning] = useState(false)
  const [cameraError, setCameraError] = useState(false)
  const scannerRef = useRef<Html5Qrcode | null>(null)

  const stopScanner = () => {
    scannerRef.current?.stop().catch(() => undefined)
    scannerRef.current = null
    setScanning(false)
  }

  const startScanner = async () => {
    setError('')
    setVerify(null)
    setMessage('')
    setCameraError(false)
    setScanning(true)
    try {
      const scanner = new Html5Qrcode('qr-reader')
      scannerRef.current = scanner
      await scanner.start(
        { facingMode: 'environment' },
        { fps: 10, qrbox: { width: 220, height: 220 } },
        (decoded) => {
          stopScanner()
          setCode(decoded)
          void check(decoded)
        },
        () => undefined
      )
    } catch {
      setScanning(false)
      setCameraError(true)
    }
  }

  useEffect(() => () => { scannerRef.current?.stop().catch(() => undefined) }, [])

  const check = async (raw?: string) => {
    const value = (raw ?? code).trim()
    if (!value) return
    setLoading(true)
    setError('')
    setMessage('')
    try {
      // Aceita tanto o código puro quanto a URL completa do QR.
      const match = value.match(/verificar\/([A-Za-z0-9-]+)/)
      const clean = match ? match[1] : value
      setVerify(await runService.verify(clean))
    } catch (loadError) {
      setVerify(null)
      setError(getApiErrorMessage(loadError, 'Código de inscrição inválido.'))
    } finally {
      setLoading(false)
    }
  }

  const deliver = async () => {
    if (!verify) return
    setDelivering(true)
    setError('')
    try {
      const match = code.match(/verificar\/([A-Za-z0-9-]+)/)
      const clean = match ? match[1] : code
      const updated = await runService.kitDelivered(clean)
      setVerify(updated)
      setMessage('Retirada do kit registrada em seu nome.')
    } catch (deliverError) {
      setError(getApiErrorMessage(deliverError, 'Não foi possível registrar a retirada.'))
    } finally {
      setDelivering(false)
    }
  }

  const paid = verify?.status === 'PAGA'
  const delivered = Boolean(verify?.kitDeliveredAt)

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default', pb: 5 }}>
      <Box sx={{ bgcolor: 'primary.main', color: 'white' }}>
        <Toolbar>
          <IconButton color="inherit" onClick={() => navigate('/modulos')} aria-label="Voltar ao painel">
            <ArrowBackIcon />
          </IconButton>
          <QrCodeScannerIcon sx={{ mr: 1 }} />
          <Typography variant="h6" sx={{ flexGrow: 1 }}>Verificar Inscritos — Rum</Typography>
          <ThemeToggle />
        </Toolbar>
      </Box>
      <Container maxWidth="sm" sx={{ mt: 3 }}>
        <Card sx={{ mb: 2 }}>
          <CardContent>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Aponte a câmera para o QR do inscrito ou cole/digite o código/link abaixo.
            </Typography>
            {cameraError && (
              <Card
                variant="outlined"
                sx={{ mb: 2, borderColor: 'warning.main', cursor: 'pointer', '&:hover': { bgcolor: 'action.hover' } }}
                onClick={() => void startScanner()}
              >
                <CardContent>
                  <Typography fontWeight={700} color="warning.dark" gutterBottom>
                    Câmera bloqueada ou sem permissão
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Para liberar, toque no cadeado (ou ícone) ao lado do endereço do site
                    na barra do navegador → <strong>Permissões</strong> → ative
                    <strong> Câmera</strong>. No Chrome também dá em
                    Configurações do site → Câmera → Permitir.
                  </Typography>
                  <Typography variant="body2" color="primary" fontWeight={700} sx={{ mt: 1 }}>
                    Toque aqui para tentar novamente
                  </Typography>
                </CardContent>
              </Card>
            )}
            {!scanning ? (
              <Button
                variant="contained" color="secondary" fullWidth
                startIcon={<QrCodeScannerIcon />}
                onClick={() => void startScanner()}
                sx={{ mb: 2 }}
              >
                Abrir leitor de QR (câmera)
              </Button>
            ) : (
              <>
                <Box id="qr-reader" sx={{ mb: 1, borderRadius: 1, overflow: 'hidden' }} />
                <Button variant="outlined" fullWidth onClick={stopScanner} sx={{ mb: 2 }}>
                  Fechar câmera
                </Button>
              </>
            )}
            <Stack direction="row" gap={1}>
              <TextField
                fullWidth
                size="small"
                label="Código ou link do QR"
                value={code}
                onChange={(e) => setCode(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && void check()}
                placeholder="Ex.: LEG-1 ou .../rum/verificar/abc123"
              />
              <Button variant="contained" onClick={() => void check()} disabled={loading || !code.trim()}>
                {loading ? '...' : 'Verificar'}
              </Button>
            </Stack>
          </CardContent>
        </Card>

        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        {message && <Alert severity="success" sx={{ mb: 2 }}>{message}</Alert>}

        {loading && <Box sx={{ textAlign: 'center', py: 4 }}><CircularProgress /></Box>}

        {verify && (
          <Card variant="outlined">
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center" gap={1}>
                <Typography variant="h6" fontWeight={800}>{verify.fullName}</Typography>
                <Chip
                  size="small"
                  color={paid ? 'success' : delivered ? 'info' : 'warning'}
                  label={paid ? (delivered ? 'Kit retirado' : 'Paga') : 'Não paga'}
                />
              </Stack>
              <Typography sx={{ mt: 1 }}>
                {verify.modality === '3KM' ? '3 km — caminhada' : '6 km — corrida'}
              </Typography>
              <Typography fontWeight={700}>
                Camisa: {verify.shirtSize || 'não informado'}
              </Typography>
              {delivered && (
                <Alert severity="info" sx={{ mt: 2 }}>
                  Kit já retirado em {new Date(verify.kitDeliveredAt!).toLocaleString('pt-BR')}
                  {verify.kitDeliveredBy ? ` — entregue por ${verify.kitDeliveredBy}` : ''}
                </Alert>
              )}
              {paid && !delivered && (
                <Button
                  variant="contained" color="success" fullWidth sx={{ mt: 2, fontWeight: 700 }}
                  onClick={() => void deliver()}
                  disabled={delivering}
                >
                  {delivering ? 'Registrando…' : 'Confirmar retirada do kit'}
                </Button>
              )}
              {!paid && (
                <Alert severity="warning" sx={{ mt: 2 }}>
                  Inscrição não paga — kit não pode ser liberado.
                </Alert>
              )}
            </CardContent>
          </Card>
        )}
      </Container>
    </Box>
  )
}

export default RunKitScanPage

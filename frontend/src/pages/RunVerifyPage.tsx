import { useEffect, useState } from 'react'
import {
  Alert,
  AppBar,
  Box,
  Button,
  CircularProgress,
  Container,
  Paper,
  Stack,
  Toolbar,
  Typography,
} from '@mui/material'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import CancelIcon from '@mui/icons-material/Cancel'
import { useNavigate, useParams } from 'react-router-dom'
import runService, { RunVerify } from '../services/runService'
import { useAuth } from '../auth/AuthContext'
import { getApiErrorMessage } from '../utils/apiError'

// Página pública de verificação do QR — o inscrito mostra o QR,
// a equipe escaneia e confirma a retirada do kit.
function RunVerifyPage() {
  const { code } = useParams<{ code: string }>()
  const navigate = useNavigate()
  const { user, isAuthenticated } = useAuth()
  const [verify, setVerify] = useState<RunVerify | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [delivering, setDelivering] = useState(false)
  const [message, setMessage] = useState('')

  const canDeliver = isAuthenticated
    && Boolean(user?.roles.some((role) => ['ROLE_ADMIN', 'ROLE_RH_TESOURARIA', 'ROLE_DIACONO'].includes(role)))

  const load = async () => {
    if (!code) return
    setLoading(true)
    try {
      setVerify(await runService.verify(code))
    } catch (loadError) {
      setError(getApiErrorMessage(loadError, 'Código de inscrição inválido.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void load() }, [code])

  const deliver = async () => {
    if (!code) return
    setDelivering(true)
    setError('')
    try {
      const updated = await runService.kitDelivered(code)
      setVerify(updated)
      setMessage('Retirada do kit registrada.')
    } catch (deliverError) {
      setError(getApiErrorMessage(deliverError, 'Não foi possível registrar a retirada.'))
    } finally {
      setDelivering(false)
    }
  }

  const paid = verify?.status === 'PAGA'
  const delivered = Boolean(verify?.kitDeliveredAt)

  return (
    <Box component="main" sx={{ bgcolor: '#FBF9F1', minHeight: '100vh' }}>
      <AppBar position="sticky" elevation={1} sx={{ bgcolor: '#1B2A6B' }}>
        <Toolbar sx={{ gap: 2 }}>
          <Box
            component="img"
            src="/logo-mercado-solidario.png"
            alt="Logo do Mercado Solidário Petrópolis"
            sx={{ height: { xs: 44, md: 56 }, width: 'auto', cursor: 'pointer' }}
            onClick={() => navigate('/rum')}
          />
          <Typography sx={{ color: '#FFC42E', fontWeight: 800, letterSpacing: 1 }}>
            VERIFICAÇÃO DE INSCRIÇÃO
          </Typography>
        </Toolbar>
      </AppBar>

      <Container maxWidth="xs" sx={{ py: 6 }}>
        {loading ? (
          <Box sx={{ textAlign: 'center', py: 8 }}><CircularProgress /></Box>
        ) : error && !verify ? (
          <Alert severity="error">{error}</Alert>
        ) : verify ? (
          <Stack spacing={3}>
            <Paper
              elevation={0}
              sx={{
                p: 4, textAlign: 'center', borderRadius: 3, bgcolor: '#FFFFFF',
                border: paid ? '3px solid #2E9E5B' : '3px solid #C9CDD6',
              }}
            >
              {paid ? (
                <CheckCircleIcon sx={{ fontSize: 64, color: '#2E9E5B' }} />
              ) : (
                <CancelIcon sx={{ fontSize: 64, color: '#9AA3B5' }} />
              )}
              <Typography variant="h5" sx={{ fontWeight: 800, color: '#1B2A6B', mt: 1 }}>
                {paid ? 'INSCRITO CONFIRMADO' : 'NÃO CONFIRMADO'}
              </Typography>
              <Typography variant="h6" sx={{ color: '#1B2A6B', mt: 2 }}>
                {verify.fullName}
              </Typography>
              <Typography sx={{ color: '#44506B', fontWeight: 700 }}>
                {verify.modality === '3KM' ? '3 km — caminhada' : '6 km — corrida'}
              </Typography>
              <Typography sx={{ color: '#44506B' }}>
                Camisa: <strong>{verify.shirtSize || 'não informado'}</strong>
              </Typography>
              {delivered && (
                <Alert severity="info" sx={{ mt: 2 }}>
                  Kit já retirado em {new Date(verify.kitDeliveredAt!).toLocaleString('pt-BR')}
                  {verify.kitDeliveredBy ? ` — entregue por ${verify.kitDeliveredBy}` : ''}
                </Alert>
              )}
            </Paper>

            {paid && (
              <Paper elevation={0} sx={{ p: 3, textAlign: 'center', borderRadius: 3, bgcolor: '#FFFFFF', border: '1px solid #E3E8EF' }}>
                <Typography variant="body2" sx={{ color: '#44506B', mb: 2 }}>
                  QR de verificação — apresente na retirada do kit
                </Typography>
                <Box
                  component="img"
                  src={verify.qrUri}
                  alt="QR de verificação da inscrição"
                  sx={{ width: 220, height: 220 }}
                />
              </Paper>
            )}

            {message && <Alert severity="success">{message}</Alert>}
            {error && <Alert severity="error">{error}</Alert>}

            {canDeliver && paid && !delivered && (
              <Button
                variant="contained" size="large"
                onClick={() => void deliver()}
                disabled={delivering}
                sx={{ bgcolor: '#2E9E5B', '&:hover': { bgcolor: '#237d47' }, fontWeight: 700 }}
              >
                {delivering ? 'Registrando…' : 'Confirmar retirada do kit'}
              </Button>
            )}
            {canDeliver && paid && delivered && (
              <Typography variant="body2" sx={{ color: '#44506B', textAlign: 'center' }}>
                A retirada deste kit já foi registrada.
              </Typography>
            )}
          </Stack>
        ) : null}
      </Container>
    </Box>
  )
}

export default RunVerifyPage

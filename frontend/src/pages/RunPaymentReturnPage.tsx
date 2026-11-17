import { useEffect, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  AppBar, Box, Button, CircularProgress, Container, Paper, Toolbar, Typography,
} from '@mui/material'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import runService from '../services/runService'

// Tela de retorno do checkout do Mercado Pago após pagar a inscrição.
function RunPaymentReturnPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const [searchParams] = useSearchParams()
  const key = searchParams.get('key')
  const [qrUri, setQrUri] = useState<string | null>(null)
  const [checking, setChecking] = useState(false)

  useEffect(() => {
    if (!id || !key) return
    const registrationId = Number(id)
    if (!Number.isFinite(registrationId)) return
    let attempts = 0
    let cancelled = false
    const load = async () => {
      try {
        const status = await runService.status(registrationId, key)
        if (cancelled) return
        if (status.status === 'PAGA' && status.checkinCode) {
          const verify = await runService.verify(status.checkinCode)
          if (!cancelled) setQrUri(verify.qrUri)
          return
        }
      } catch {
        // inscrição ainda não confirmada ou link incompleto — tenta de novo
      }
      attempts += 1
      if (attempts < 6 && !cancelled) {
        setTimeout(load, 8000)
      } else {
        setChecking(false)
      }
    }
    setChecking(true)
    load()
    return () => { cancelled = true }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, key])

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

      <Container maxWidth="xs" sx={{ py: 8 }}>
        <Paper
          elevation={0}
          sx={{
            p: { xs: 4, md: 5 }, textAlign: 'center', borderRadius: 3,
            bgcolor: '#FFFFFF', border: '1px solid #E3E8EF',
          }}
        >
          <CheckCircleIcon sx={{ fontSize: 64, color: '#2E9E5B', mb: 2 }} />
          <Typography variant="h5" sx={{ fontWeight: 800, color: '#1B2A6B', mb: 2 }}>
            Pagamento recebido! Obrigado por correr com a gente.
          </Typography>
          <Typography sx={{ color: '#44506B', mb: 3 }}>
            Sua inscrição no Mercado Solidário Run está sendo confirmada.
            Assim que o pagamento for validado, você recebe um e-mail com o
            <strong> QR code de retirada do kit</strong> (camisa + medalha) em anexo.
          </Typography>

          {qrUri ? (
            <Box sx={{ mb: 3 }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 700, color: '#1B2A6B', mb: 1 }}>
                Seu QR code de retirada:
              </Typography>
              <Box
                component="img"
                src={qrUri}
                alt="QR code de retirada do kit"
                sx={{ width: 220, height: 220, borderRadius: 2, border: '1px solid #E3E8EF' }}
              />
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                Este mesmo QR code será enviado por e-mail — guarde também o e-mail
                para apresentar na retirada do kit.
              </Typography>
            </Box>
          ) : checking ? (
            <Box sx={{ mb: 3, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1 }}>
              <CircularProgress size={18} />
              <Typography variant="body2" color="text.secondary">
                Confirmando seu pagamento…
              </Typography>
            </Box>
          ) : (
            <Typography sx={{ color: '#44506B', mb: 3 }}>
              Se o pagamento foi por PIX e ainda estiver aguardando confirmação,
              a equipe do projeto valida e libera seu kit normalmente —
              é só apresentar o comprovante na retirada.
            </Typography>
          )}

          <Button
            variant="contained"
            onClick={() => navigate('/run')}
            sx={{ fontWeight: 700, bgcolor: '#1B2A6B', '&:hover': { bgcolor: '#2E4FA3' } }}
          >
            Voltar para a corrida
          </Button>
        </Paper>
      </Container>
    </Box>
  )
}

export default RunPaymentReturnPage

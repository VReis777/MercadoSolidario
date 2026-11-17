import { useNavigate } from 'react-router-dom'
import {
  AppBar, Box, Button, Container, Paper, Toolbar, Typography,
} from '@mui/material'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'

// Tela de retorno do checkout do Mercado Pago após pagar a inscrição.
function RunPaymentReturnPage() {
  const navigate = useNavigate()

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
          <Typography sx={{ color: '#44506B', mb: 1 }}>
            Sua inscrição no Mercado Solidário Run está sendo confirmada.
          </Typography>
          <Typography sx={{ color: '#44506B', mb: 3 }}>
            Assim que o pagamento for validado, você recebe um e-mail com o
            <strong> QR code de retirada do kit</strong> (camisa + medalha) em anexo.
            Apresente esse QR no dia da corrida para retirar seu kit.
          </Typography>
          <Typography sx={{ color: '#44506B', mb: 3 }}>
            Se o pagamento foi por PIX e ainda estiver aguardando confirmação,
            a equipe do projeto valida e libera seu kit normalmente —
            é só apresentar o comprovante na retirada.
          </Typography>
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

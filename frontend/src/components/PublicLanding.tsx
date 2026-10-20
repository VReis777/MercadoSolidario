import { useState } from 'react'
import {
  Box,
  Button,
  Card,
  CardContent,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  Typography,
} from '@mui/material'
import FavoriteBorderIcon from '@mui/icons-material/FavoriteBorder'
import BusinessIcon from '@mui/icons-material/Business'
import HomeIcon from '@mui/icons-material/Home'

interface PublicLandingProps {
  onPrimaryAction?: (action: 'donate' | 'company' | 'benefit') => void
  onBenefitRequest?: () => void
  extraActions?: React.ReactNode
}

function PublicLanding({ onPrimaryAction, onBenefitRequest, extraActions }: PublicLandingProps) {
  const [info, setInfo] = useState<{ title: string; message: string } | null>(null)

  const handleAction = (action: 'donate' | 'company' | 'benefit') => {
    if (action === 'donate' && onPrimaryAction) {
      onPrimaryAction(action)
      return
    }
    if (action === 'company' && onPrimaryAction) {
      onPrimaryAction(action)
      return
    }
    if (action === 'benefit' && (onBenefitRequest || onPrimaryAction)) {
      ;(onBenefitRequest ?? onPrimaryAction)?.(action)
      return
    }
    const messages = {
      donate: {
        title: 'Doações',
        message: 'A função de doação ainda será disponibilizada. Nenhum pagamento foi iniciado.',
      },
      company: {
        title: 'Empresas parceiras',
        message:
          'Empresas que queiram doar ou patrocinar podem falar conosco pelo Instagram @mercadosolidariopetropolis ou pela página "Doe aqui". Nenhum cadastro é exigido.',
      },
      benefit: {
        title: 'Quero ser beneficiado',
        message: 'O pedido público de benefício ainda será disponibilizado. Nenhum cadastro foi realizado.',
      },
    }
    setInfo(messages[action])
  }

  return (
    <Box sx={{ bgcolor: 'background.default', overflowX: 'hidden' }}>
      <Box
        sx={{
          position: 'relative',
          overflow: 'hidden',
          color: 'white',
          py: { xs: 8, md: 12 },
          px: 2,
          textAlign: 'center',
          '&::before': {
            content: '""',
            position: 'absolute',
            inset: 0,
            backgroundImage: 'url(/carrinho.png)',
            backgroundSize: 'cover',
            backgroundPosition: 'center',
            filter: 'blur(6px) brightness(0.55)',
            transform: 'scale(1.08)',
          },
        }}
      >
        <Container maxWidth="md" sx={{ position: 'relative' }}>
          <Typography variant="overline" sx={{ letterSpacing: 3, fontWeight: 800 }}>
            MEVAM PETRÓPOLIS · 2026
          </Typography>
          <Box sx={{ mt: 1, mb: 1 }}>
            <Typography variant="h3" component="h1"
              sx={{ fontWeight: 900, lineHeight: 1.05, fontSize: { xs: '2.4rem', sm: '3.2rem', md: '4rem' } }}>
              Mercado
            </Typography>
            <Typography component="span"
              sx={{
                display: 'inline-block',
                fontWeight: 900,
                fontSize: { xs: '2rem', sm: '2.8rem', md: '3.4rem' },
                color: '#0B2B4B',
                bgcolor: '#3EC6C0',
                px: 2,
                borderRadius: 1,
                boxShadow: '0 3px 0 #FFC42E',
              }}>
              Solidário
            </Typography>
          </Box>
          <Typography variant="h6" sx={{ fontWeight: 500, mt: 2, mb: 3 }}>
            Natal com dignidade para todas as famílias
          </Typography>
          <Typography sx={{ maxWidth: 640, mx: 'auto', mb: 4, opacity: 0.95 }}>
            Um espaço de cuidado para que nenhuma família fique invisível neste Natal.
            Aqui, a solidariedade aparece com organização, respeito e transparência.
          </Typography>
          <Box sx={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'center', gap: 1.5 }}>
            <Button
              variant="contained"
              color="secondary"
              size="large"
              startIcon={<FavoriteBorderIcon />}
              onClick={() => handleAction('donate')}
              sx={{ minWidth: { xs: '100%', sm: 200 } }}
            >
              DOE AQUI
            </Button>
            <Button
              variant="outlined"
              size="large"
              startIcon={<BusinessIcon />}
              onClick={() => handleAction('company')}
              sx={{
                minWidth: { xs: '100%', sm: 200 },
                color: 'white',
                borderColor: 'white',
                '&:hover': { borderColor: 'white', bgcolor: 'rgba(255,255,255,0.08)' },
              }}
            >
              SOU EMPRESA
            </Button>
            <Button
              variant="text"
              size="large"
              startIcon={<HomeIcon />}
              onClick={() => handleAction('benefit')}
              sx={{ minWidth: { xs: '100%', sm: 220 }, color: 'white' }}
            >
              QUERO SER BENEFICIADO
            </Button>
          </Box>
          {extraActions && <Box sx={{ mt: 3 }}>{extraActions}</Box>}
        </Container>
      </Box>

      <Container maxWidth="lg" sx={{ py: { xs: 6, md: 8 } }}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={4}>
            <Card sx={{ height: '100%' }}>
              <CardContent>
                <Typography variant="h6" gutterBottom color="primary">
                  O objetivo
                </Typography>
                <Typography color="text.secondary">
                  Organizar o apoio a famílias em situação de vulnerabilidade, com cadastro
                  responsável, acompanhamento humano e distribuição digna de cestas e cuidados.
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={4}>
            <Card sx={{ height: '100%' }}>
              <CardContent>
                <Typography variant="h6" gutterBottom color="primary">
                  Como ajudar
                </Typography>
                <Typography color="text.secondary">
                  Pessoas e empresas poderão contribuir com doações e parcerias. Estas funções
                  ainda estão em construção e não solicitam chave PIX, telefone ou WhatsApp.
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={4}>
            <Card sx={{ height: '100%' }}>
              <CardContent>
                <Typography variant="h6" gutterBottom color="primary">
                  Transparência
                </Typography>
                <Typography color="text.secondary">
                  O projeto registra famílias, status e operações administrativas para que o
                  cuidado seja rastreável. Esta página não apresenta indicadores reais.
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Container>

      <Box component="footer" sx={{ bgcolor: '#183B56', color: 'white', py: 4, px: 2 }}>
        <Container maxWidth="lg">
          <Typography fontWeight={800}>MERCADO SOLIDÁRIO</Typography>
          <Typography variant="body2" sx={{ mt: 1, opacity: 0.9 }}>
            Iniciativa comunitária em desenvolvimento.
          </Typography>
          <Typography variant="body2" sx={{ mt: 1, opacity: 0.9 }}>
            PIX para doações: <strong>msolidario@hotmail.com</strong> ·{' '}
            <a
              href="https://www.instagram.com/mercadosolidariopetropolis"
              target="_blank"
              rel="noopener noreferrer"
              style={{ color: 'inherit' }}
            >
              Instagram: @mercadosolidariopetropolis
            </a>
          </Typography>
        </Container>
      </Box>

      <Dialog open={Boolean(info)} onClose={() => setInfo(null)} fullWidth maxWidth="sm">
        <DialogTitle>{info?.title}</DialogTitle>
        <DialogContent>
          <Typography>{info?.message}</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setInfo(null)} variant="contained">
            Entendi
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default PublicLanding

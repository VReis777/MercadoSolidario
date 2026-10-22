import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  AppBar, Box, Button, Container, Divider, Drawer, Grid, IconButton, Link,
  List, ListItemButton, ListItemText, Paper, Toolbar, Typography,
} from '@mui/material'
import MenuIcon from '@mui/icons-material/Menu'
import FavoriteIcon from '@mui/icons-material/Favorite'
import InstagramIcon from '@mui/icons-material/Instagram'

const navItems = [
  { label: 'Início', target: 'inicio' },
  { label: 'Nossa História', target: 'historia' },
  { label: 'Nosso Impacto', target: 'impacto' },
  { label: '2026', target: 'desafio' },
]

const impactCards = [
  {
    value: '2020',
    title: 'Ano de início',
    text: 'O ano em que o Mercado Solidário nasceu em Petrópolis.',
  },
  {
    value: '5 edições',
    title: 'Uma trajetória construída',
    text: 'Uma história de crescimento ano a ano, com solidariedade.',
  },
  {
    value: '240 famílias',
    title: 'Somente em 2025',
    text: 'Famílias assistidas na maior edição realizada até então.',
  },
]

function scrollToSection(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function LandingPage() {
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)

  const goTo = (target: string) => {
    setMenuOpen(false)
    scrollToSection(target)
  }

  return (
    <Box component="main" sx={{ bgcolor: '#FBF9F1', minHeight: '100vh' }}>
      {/* Cabeçalho */}
      <AppBar position="sticky" elevation={1} sx={{ bgcolor: '#1B2A6B' }}>
        <Toolbar sx={{ gap: 2 }}>
          <Box
            component="img"
            src="/logo-mercado-solidario.png"
            alt="Logo do Mercado Solidário Petrópolis"
            sx={{ height: { xs: 44, md: 56 }, width: 'auto', cursor: 'pointer' }}
            onClick={() => goTo('inicio')}
          />
          <Box sx={{ flexGrow: 1 }} />
          <Box sx={{ display: { xs: 'none', md: 'flex' }, gap: 1 }}>
            {navItems.map((item) => (
              <Button
                key={item.target}
                color="inherit"
                onClick={() => goTo(item.target)}
                sx={{ fontWeight: 600, textTransform: 'none', fontSize: '1rem' }}
              >
                {item.label}
              </Button>
            ))}
          </Box>
          <IconButton
            color="inherit"
            aria-label="Abrir menu de navegação"
            sx={{ display: { xs: 'flex', md: 'none' } }}
            onClick={() => setMenuOpen(true)}
          >
            <MenuIcon />
          </IconButton>
        </Toolbar>
      </AppBar>
      <Drawer
        anchor="right"
        open={menuOpen}
        onClose={() => setMenuOpen(false)}
        PaperProps={{ sx: { width: 240 } }}
      >
        <List>
          {navItems.map((item) => (
            <ListItemButton key={item.target} onClick={() => goTo(item.target)}>
              <ListItemText primary={item.label} />
            </ListItemButton>
          ))}
        </List>
      </Drawer>

      {/* Hero */}
      <Box
        id="inicio"
        component="section"
        sx={{
          background: 'linear-gradient(135deg, #1B2A6B 0%, #2E4FA3 55%, #19B8D2 130%)',
          color: 'white',
          py: { xs: 8, md: 12 },
          textAlign: 'center',
        }}
      >
        <Container maxWidth="md">
          <Box
            sx={{
              display: 'flex',
              flexDirection: { xs: 'column', sm: 'row' },
              alignItems: 'center',
              justifyContent: 'center',
              gap: { xs: 3, sm: 5 },
              mb: 3,
            }}
          >
            <Box
              component="img"
              src="/logo-mercado-solidario.png"
              alt="Mercado Solidário — MEVAM Petrópolis 2026"
              sx={{ height: { xs: 110, md: 150 }, width: 'auto' }}
            />
            <Box
              component="img"
              src="/logo-terefrutas.png"
              alt="Grupo Terê Frutas"
              sx={{
                height: { xs: 80, md: 110 },
                width: 'auto',
                bgcolor: '#FFFFFF',
                px: 2,
                py: 1,
                borderRadius: 2,
              }}
            />
          </Box>
          <Typography
            variant="h6"
            component="p"
            sx={{
              letterSpacing: 4,
              fontWeight: 800,
              color: '#FFC42E',
              textTransform: 'uppercase',
              fontSize: { xs: '1.1rem', md: '1.5rem' },
            }}
          >
            Mercado Solidário Petrópolis
          </Typography>
          <Typography
            variant="h3"
            component="h1"
            sx={{ fontWeight: 800, my: 2, fontSize: { xs: '2rem', md: '3rem' } }}
          >
            Transformando generosidade em dignidade.
          </Typography>
          <Typography variant="h6" sx={{ fontWeight: 400, opacity: 0.95, mb: 4 }}>
            Uma corrente de solidariedade que leva acolhimento, cuidado e esperança
            às famílias de Petrópolis.
          </Typography>
          <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
            <Button
              variant="contained"
              size="large"
              onClick={() => scrollToSection('historia')}
              sx={{
                bgcolor: '#FFC42E',
                color: '#1B2A6B',
                fontWeight: 700,
                px: 4,
                '&:hover': { bgcolor: '#ffd35c' },
              }}
            >
              Conheça nossa história
            </Button>
            <Paper
              elevation={0}
              sx={{
                mt: 1,
                px: 3,
                py: 1,
                bgcolor: 'rgba(255,255,255,0.15)',
                color: 'white',
                borderRadius: 5,
                display: 'inline-flex',
                alignItems: 'center',
                gap: 1,
              }}
            >
              <FavoriteIcon fontSize="small" sx={{ color: '#FFC42E' }} />
              <Typography fontWeight={600}>
                Nosso desafio: alcançar 500 famílias.
              </Typography>
            </Paper>
          </Box>
        </Container>
      </Box>

      {/* Nossa História */}
      <Box id="historia" component="section" sx={{ py: { xs: 7, md: 10 } }}>
        <Container maxWidth="md">
          <Typography variant="h4" component="h2" sx={{ color: '#1B2A6B', fontWeight: 800, mb: 4, textAlign: 'center' }}>
            Nossa História
          </Typography>
          <Typography paragraph sx={{ fontSize: '1.1rem', lineHeight: 1.9, color: '#44506B' }}>
            O Mercado Solidário Petrópolis é uma iniciativa social que nasceu em{' '}
            <Box component="strong" sx={{ color: '#1B2A6B' }}>2020</Box>, com o propósito
            de levar dignidade, cuidado e esperança a famílias que enfrentam situações de
            vulnerabilidade.
          </Typography>
          <Typography paragraph sx={{ fontSize: '1.1rem', lineHeight: 1.9, color: '#44506B' }}>
            Ao longo de sua história, o projeto já realizou{' '}
            <Box component="strong" sx={{ color: '#1B2A6B' }}>5 edições</Box>, crescendo a
            cada ano por meio da mobilização de voluntários, parceiros, empresas e pessoas
            que acreditam no poder da generosidade.
          </Typography>
          <Typography paragraph sx={{ fontSize: '1.1rem', lineHeight: 1.9, color: '#44506B' }}>
            Somente em 2025, <Box component="strong" sx={{ color: '#1B2A6B' }}>240 famílias</Box>{' '}
            foram assistidas, na maior edição realizada até então. Ao longo de todas as
            edições, centenas de famílias de Petrópolis já foram alcançadas pelo projeto.
          </Typography>
          <Typography paragraph sx={{ fontSize: '1.1rem', lineHeight: 1.9, color: '#44506B' }}>
            Mais do que entregar alimentos, o Mercado Solidário busca proporcionar uma
            experiência de acolhimento e dignidade, permitindo que cada família seja
            recebida com respeito e cuidado.
          </Typography>
          <Typography paragraph sx={{ fontSize: '1.1rem', lineHeight: 1.9, color: '#44506B' }}>
            Agora, em 2026, estamos diante do nosso maior desafio: alcançar{' '}
            <Box component="strong" sx={{ color: '#1B2A6B' }}>500 famílias</Box>, mobilizando
            toda a cidade em uma grande corrente de solidariedade.
          </Typography>
        </Container>
      </Box>

      {/* Nosso Impacto */}
      <Box id="impacto" component="section" sx={{ py: { xs: 7, md: 10 }, bgcolor: '#FFFFFF' }}>
        <Container maxWidth="lg">
          <Typography variant="h4" component="h2" sx={{ color: '#1B2A6B', fontWeight: 800, mb: 5, textAlign: 'center' }}>
            Nosso Impacto
          </Typography>
          <Grid container spacing={4} justifyContent="center">
            {impactCards.map((card) => (
              <Grid item xs={12} sm={6} md={4} key={card.value}>
                <Paper
                  elevation={0}
                  sx={{
                    p: 4,
                    height: '100%',
                    textAlign: 'center',
                    bgcolor: '#FFFFFF',
                    border: '1px solid #E3E8EF',
                    borderRadius: 3,
                    borderTop: '4px solid #19B8D2',
                    boxShadow: '0 4px 18px rgba(27,42,107,0.10)',
                  }}
                >
                  <Typography variant="h3" sx={{ color: '#19B8D2', fontWeight: 800, mb: 1 }}>
                    {card.value}
                  </Typography>
                  <Typography variant="subtitle1" sx={{ fontWeight: 700, color: '#1B2A6B', mb: 1 }}>
                    {card.title}
                  </Typography>
                  <Typography variant="body2" sx={{ color: '#44506B' }}>
                    {card.text}
                  </Typography>
                </Paper>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      {/* Desafio 2026 */}
      <Box
        id="desafio"
        component="section"
        sx={{
          py: { xs: 8, md: 12 },
          background: 'linear-gradient(135deg, #1B2A6B 0%, #2E4FA3 60%, #19B8D2 140%)',
          color: 'white',
          textAlign: 'center',
        }}
      >
        <Container maxWidth="md">
          <Typography variant="h4" component="h2" sx={{ fontWeight: 800, mb: 2 }}>
            Em 2026, queremos chegar ainda mais longe.
          </Typography>
          <Typography
            sx={{
              fontSize: { xs: '5rem', md: '8rem' },
              fontWeight: 900,
              lineHeight: 1,
              color: '#FFC42E',
            }}
          >
            500
          </Typography>
          <Typography variant="h5" sx={{ fontWeight: 700, mb: 3 }}>
            famílias de Petrópolis
          </Typography>
          <Typography sx={{ fontSize: '1.1rem', lineHeight: 1.8, opacity: 0.95, mb: 3, maxWidth: 620, mx: 'auto' }}>
            Nosso maior desafio até agora é alcançar 500 famílias de Petrópolis.
            Para tornar esse objetivo realidade, precisamos da mobilização de
            voluntários, parceiros, empresas e de toda a comunidade.
          </Typography>
          <Typography variant="h6" sx={{ fontStyle: 'italic', color: '#FFC42E' }}>
            "Cada gesto de solidariedade pode transformar uma história."
          </Typography>
        </Container>
      </Box>

      {/* Parceiros */}
      <Box component="section" sx={{ py: { xs: 7, md: 10 } }}>
        <Container maxWidth="md" sx={{ textAlign: 'center' }}>
          <Typography variant="h4" component="h2" sx={{ color: '#1B2A6B', fontWeight: 800, mb: 2 }}>
            A solidariedade cresce quando caminhamos juntos.
          </Typography>
          <Typography sx={{ fontSize: '1.05rem', color: '#44506B', mb: 5, maxWidth: 600, mx: 'auto' }}>
            A união entre iniciativas sociais, parceiros e comunidade fortalece nossa
            capacidade de levar dignidade a mais famílias.
          </Typography>
          <Box
            sx={{
              display: 'flex',
              flexDirection: { xs: 'column', sm: 'row' },
              alignItems: 'center',
              justifyContent: 'center',
              gap: { xs: 5, sm: 8 },
            }}
          >
            <Box>
              <Box
                component="img"
                src="/logo-mercado-solidario.png"
                alt="Logo do Mercado Solidário Petrópolis"
                sx={{ height: { xs: 90, md: 120 }, width: 'auto' }}
              />
              <Typography variant="caption" display="block" sx={{ mt: 1, color: '#44506B', fontWeight: 600 }}>
                Mercado Solidário Petrópolis
              </Typography>
            </Box>
            <Box>
              <Box
                component="img"
                src="/logo-terefrutas.png"
                alt="Logo do Grupo Terê Frutas"
                sx={{ height: { xs: 70, md: 90 }, width: 'auto', bgcolor: '#FFFFFF', p: 1, borderRadius: 2 }}
              />
              <Typography variant="caption" display="block" sx={{ mt: 1, color: '#44506B', fontWeight: 600 }}>
                Parceira — Grupo Terê Frutas
              </Typography>
            </Box>
          </Box>
        </Container>
      </Box>

      {/* Rodapé */}
      <Box component="footer" sx={{ bgcolor: '#1B2A6B', color: 'white', py: 5 }}>
        <Container maxWidth="md" sx={{ textAlign: 'center' }}>
          <Typography variant="h6" sx={{ fontWeight: 800 }}>
            Mercado Solidário Petrópolis
          </Typography>
          <Typography variant="body2" sx={{ opacity: 0.85, mb: 2 }}>
            Transformando generosidade em dignidade.
          </Typography>
          <Box sx={{ display: 'flex', justifyContent: 'center', flexWrap: 'wrap', gap: 2.5, mb: 2 }}>
            <Link component="button" color="inherit" underline="hover" onClick={() => goTo('inicio')}>
              Voltar ao início
            </Link>
            <Link component="button" color="inherit" underline="hover" onClick={() => navigate('/doacao')}>
              DOE AQUI
            </Link>
            <Link component="button" color="inherit" underline="hover" onClick={() => navigate('/sou-empresa')}>
              Sou empresa
            </Link>
            <Link component="button" color="inherit" underline="hover" onClick={() => navigate('/entrar')}>
              Acesso administrativo
            </Link>
            <Link
              color="inherit"
              underline="hover"
              href="https://www.instagram.com/mercadosolidariopetropolis"
              target="_blank"
              rel="noopener noreferrer"
              sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5 }}
            >
              <InstagramIcon fontSize="small" /> Instagram
            </Link>
          </Box>
          <Divider sx={{ borderColor: 'rgba(255,255,255,0.2)', my: 2 }} />
          <Typography variant="caption" sx={{ opacity: 0.7 }}>
            Mercado Solidário Petrópolis — 2026
          </Typography>
        </Container>
      </Box>
    </Box>
  )
}

export default LandingPage

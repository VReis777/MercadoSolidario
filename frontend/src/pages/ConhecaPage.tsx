import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box, Button, Container, Grid, IconButton, Paper, Toolbar, Typography,
} from '@mui/material'
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft'
import ChevronRightIcon from '@mui/icons-material/ChevronRight'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import PlayCircleOutlineIcon from '@mui/icons-material/PlayCircleOutline'

const GALLERY_COUNT = 22

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

function ConhecaPage() {
  const navigate = useNavigate()
  const [galleryIndex, setGalleryIndex] = useState(0)

  return (
    <Box component="main" sx={{ bgcolor: '#FBF9F1', minHeight: '100vh' }}>
      {/* Cabeçalho simples */}
      <Toolbar sx={{ bgcolor: '#1B2A6B', color: 'white', gap: 2 }}>
        <Box
          component="img"
          src="/logo-mercado-solidario.png"
          alt="Logo do Mercado Solidário Petrópolis"
          sx={{ height: { xs: 40, md: 50 }, width: 'auto', cursor: 'pointer' }}
          onClick={() => navigate('/')}
        />
        <Box sx={{ flexGrow: 1 }} />
        <Button
          color="inherit"
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate('/')}
          sx={{ fontWeight: 600, textTransform: 'none' }}
        >
          Voltar ao início
        </Button>
      </Toolbar>

      {/* Hero */}
      <Box
        component="section"
        sx={{
          background: 'linear-gradient(135deg, #1B2A6B 0%, #2E4FA3 55%, #19B8D2 130%)',
          color: 'white',
          py: { xs: 7, md: 10 },
          textAlign: 'center',
        }}
      >
        <Container maxWidth="md">
          <Typography
            variant="h6"
            component="p"
            sx={{
              letterSpacing: 4,
              fontWeight: 800,
              color: '#FFC42E',
              textTransform: 'uppercase',
              fontSize: { xs: '1.1rem', md: '1.4rem' },
            }}
          >
            Mercado Solidário Petrópolis
          </Typography>
          <Typography
            variant="h3"
            component="h1"
            sx={{ fontWeight: 800, my: 2, fontSize: { xs: '2rem', md: '3rem' } }}
          >
            Conheça um pouco do Mercado Solidário
          </Typography>
          <Typography variant="h6" sx={{ fontWeight: 400, opacity: 0.95, mb: 4 }}>
            Um pouco do ano passado, um pouco de solidariedade.
          </Typography>
          <Button
            variant="contained"
            size="large"
            onClick={() => scrollToSection('video')}
            startIcon={<PlayCircleOutlineIcon />}
            sx={{
              bgcolor: '#FFC42E',
              color: '#1B2A6B',
              fontWeight: 700,
              px: 4,
              '&:hover': { bgcolor: '#ffd35c' },
            }}
          >
            Assista o vídeo
          </Button>
        </Container>
      </Box>

      {/* Vídeo */}
      <Box id="video" component="section" sx={{ py: { xs: 7, md: 9 } }}>
        <Container maxWidth="md" sx={{ textAlign: 'center' }}>
          <Typography variant="h4" component="h2" sx={{ color: '#1B2A6B', fontWeight: 800, mb: 2 }}>
            O Mercado Solidário em ação
          </Typography>
          <Typography sx={{ fontSize: '1.05rem', color: '#44506B', mb: 4, maxWidth: 640, mx: 'auto' }}>
            Um vídeo para mostrar o que essa corrente de solidariedade faz
            por dentro — e por que cada família importa.
          </Typography>
          <Box
            component="video"
            controls
            preload="metadata"
            playsInline
            poster="/historia-video-poster.jpg"
            src="/historia-video.mp4"
            sx={{
              width: '100%',
              maxHeight: { xs: 320, md: 560 },
              borderRadius: 3,
              boxShadow: '0 8px 24px rgba(27,42,107,0.18)',
              bgcolor: '#000',
            }}
          />
        </Container>
      </Box>

      {/* Galeria */}
      <Box component="section" sx={{ py: { xs: 7, md: 9 }, bgcolor: '#FFFFFF' }}>
        <Container maxWidth="md" sx={{ textAlign: 'center' }}>
          <Typography variant="h4" component="h2" sx={{ color: '#1B2A6B', fontWeight: 800, mb: 2 }}>
            Momentos de solidariedade
          </Typography>
          <Typography sx={{ fontSize: '1.05rem', color: '#44506B', mb: 4, maxWidth: 640, mx: 'auto' }}>
            Cada entrega é uma família recebendo dignidade — veja como foi.
          </Typography>

          <Box sx={{ position: 'relative', mb: 2 }}>
            <Box
              component="img"
              src={`/galeria/foto-${String(galleryIndex + 1).padStart(2, '0')}.jpg`}
              alt="Registro de ação do Mercado Solidário"
              sx={{
                width: '100%',
                maxHeight: { xs: 320, md: 440 },
                objectFit: 'cover',
                borderRadius: 3,
                boxShadow: '0 8px 24px rgba(27,42,107,0.18)',
              }}
            />
            <IconButton
              onClick={() => setGalleryIndex((galleryIndex + GALLERY_COUNT - 1) % GALLERY_COUNT)}
              aria-label="Foto anterior"
              sx={{
                position: 'absolute', left: 8, top: '50%', transform: 'translateY(-50%)',
                bgcolor: 'rgba(255,255,255,0.25)', color: '#7B9CFF',
                backdropFilter: 'blur(2px)',
                '&:hover': { bgcolor: 'rgba(255,255,255,0.45)' },
              }}
            >
              <ChevronLeftIcon fontSize="large" />
            </IconButton>
            <IconButton
              onClick={() => setGalleryIndex((galleryIndex + 1) % GALLERY_COUNT)}
              aria-label="Próxima foto"
              sx={{
                position: 'absolute', right: 8, top: '50%', transform: 'translateY(-50%)',
                bgcolor: 'rgba(255,255,255,0.25)', color: '#7B9CFF',
                backdropFilter: 'blur(2px)',
                '&:hover': { bgcolor: 'rgba(255,255,255,0.45)' },
              }}
            >
              <ChevronRightIcon fontSize="large" />
            </IconButton>
          </Box>

          <Box
            sx={{
              display: 'flex',
              gap: 1,
              overflowX: 'auto',
              pb: 1,
              justifyContent: { md: 'center' },
              '&::-webkit-scrollbar': { height: 6 },
              '&::-webkit-scrollbar-thumb': { bgcolor: '#C9CDD6', borderRadius: 3 },
            }}
          >
            {Array.from({ length: GALLERY_COUNT }, (_, i) => {
              const src = `/galeria/foto-${String(i + 1).padStart(2, '0')}.jpg`
              const active = i === galleryIndex
              return (
                <Box
                  key={src}
                  component="img"
                  src={src}
                  alt={`Miniatura ${i + 1}`}
                  loading="lazy"
                  onClick={() => setGalleryIndex(i)}
                  sx={{
                    width: 64,
                    height: 64,
                    objectFit: 'cover',
                    flexShrink: 0,
                    borderRadius: 1.5,
                    cursor: 'pointer',
                    border: active ? '3px solid #FFC42E' : '2px solid transparent',
                    opacity: active ? 1 : 0.6,
                    transition: 'opacity 0.2s',
                    '&:hover': { opacity: 1 },
                  }}
                />
              )
            })}
          </Box>
        </Container>
      </Box>

      {/* História */}
      <Box id="historia" component="section" sx={{ py: { xs: 7, md: 10 } }}>
        <Container maxWidth="md">
          <Typography variant="h4" component="h2" sx={{ color: '#1B2A6B', fontWeight: 800, mb: 4, textAlign: 'center' }}>
            Um pouco da nossa história
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
        </Container>
      </Box>

      {/* Impacto */}
      <Box component="section" sx={{ py: { xs: 7, md: 10 }, bgcolor: '#FFFFFF' }}>
        <Container maxWidth="lg">
          <Typography variant="h4" component="h2" sx={{ color: '#1B2A6B', fontWeight: 800, mb: 5, textAlign: 'center' }}>
            Nosso impacto
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

      {/* Meta 2026 */}
      <Box
        id="meta2026"
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
            A meta de 2026
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
          <Button
            variant="contained"
            size="large"
            onClick={() => navigate('/doacao')}
            sx={{
              mt: 4,
              bgcolor: '#FFC42E',
              color: '#1B2A6B',
              fontWeight: 700,
              px: 5,
              '&:hover': { bgcolor: '#ffd35c' },
            }}
          >
            Doe e ajude a chegar lá
          </Button>
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
          <Button color="inherit" onClick={() => navigate('/')} sx={{ textTransform: 'none', fontWeight: 600 }}>
            ← Voltar ao início
          </Button>
          <Typography variant="caption" display="block" sx={{ opacity: 0.7, mt: 2 }}>
            Mercado Solidário Petrópolis — 2026
          </Typography>
        </Container>
      </Box>
    </Box>
  )
}

export default ConhecaPage

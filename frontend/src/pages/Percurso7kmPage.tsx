import { Box, Button, Container, Paper, Typography } from '@mui/material'
import DirectionsRunIcon from '@mui/icons-material/DirectionsRun'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import { useNavigate } from 'react-router-dom'

const TRECHOS = [
  {
    km: '1,33 km',
    titulo: 'Trecho 1',
    texto: 'Saímos da MEVAM pela Pedro Elmer e seguimos pela Quissamã, passando ao lado do Clube Palmeira, até a entrada da Gregório Cruzick.',
  },
  {
    km: '2,31 km',
    titulo: 'Trecho 2',
    texto: 'Da Gregório Cruzick, seguimos pela José Timóteo Caldara, junto à Ponte de Ferro, e pela João de Farias até a Bernardo Proença.',
  },
  {
    km: '1,32 km',
    titulo: 'Trecho 3',
    texto: 'Da Bernardo Proença, acessamos a Hyvio Naliato e chegamos à Bernardo Vasconcelos, na altura da Dual Elétrica.',
  },
  {
    km: '1,60 km',
    titulo: 'Trecho 4',
    texto: 'Seguimos pela Estrada de Cascatinha, passamos pela Guilherme Daumas Nunes e pela Machado Fagundes e descemos a Francisco Framback para cruzar a linha de chegada na MEVAM.',
  },
]

function Percurso7kmPage() {
  const navigate = useNavigate()

  return (
    <Box component="main" sx={{ bgcolor: '#FBF9F1', minHeight: '100vh' }}>
      <Box sx={{ bgcolor: '#1B2A6B', pt: 3, pb: { xs: 4, md: 6 }, textAlign: 'center' }}>
        <Box
          component="img"
          src="/logo-mercado-solidario.png"
          alt="Logo do Mercado Solidário Petrópolis"
          sx={{ height: { xs: 48, md: 64 }, width: 'auto', cursor: 'pointer', mb: 2 }}
          onClick={() => navigate('/')}
        />
        <Typography
          variant="h3" component="h1"
          sx={{ color: '#FFC42E', fontWeight: 800, letterSpacing: 1 }}
        >
          🏃‍♂️ TRAJETO LIBERADO!
        </Typography>
        <Typography sx={{ color: '#EAF0FF', mt: 1.5, fontSize: '1.15rem', maxWidth: 720, mx: 'auto', px: 2 }}>
          Já saiu o percurso do Mercado Solidário Run! São aproximadamente{' '}
          <strong>6,56 km</strong> pelas ruas de Petrópolis, com largada e chegada na
          MEVAM Petrópolis, na Rua Bernardo Vasconcelos, 604.
        </Typography>
      </Box>

      <Container maxWidth="md" sx={{ py: { xs: 4, md: 6 } }}>
        <Paper
          elevation={0}
          sx={{ p: { xs: 2, md: 4 }, border: '1px solid #E3E8EF', borderRadius: 3, bgcolor: '#FFFFFF' }}
        >
          <Box
            component="img"
            src="/percurso-7km.jpg"
            alt="Mapa oficial do percurso da corrida de 7 km — Mercado Solidário Run"
            sx={{ width: '100%', borderRadius: 2, mb: 4, border: '1px solid #E3E8EF' }}
          />

          {TRECHOS.map((trecho, index) => (
            <Box
              key={trecho.titulo}
              sx={{
                display: 'flex', gap: 2, mb: 3,
                alignItems: 'flex-start',
              }}
            >
              <Box
                sx={{
                  minWidth: 64, height: 64, borderRadius: '50%', flexShrink: 0,
                  bgcolor: index === 0 ? '#FFC42E' : index === TRECHOS.length - 1 ? '#19B8D2' : '#1B2A6B',
                  color: '#FFFFFF', display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontWeight: 800, fontSize: '1.05rem',
                }}
              >
                {trecho.km}
              </Box>
              <Box>
                <Typography variant="h6" sx={{ fontWeight: 800, color: '#1B2A6B' }}>
                  {trecho.titulo}
                </Typography>
                <Typography variant="body1" sx={{ color: '#44506B', mt: 0.5 }}>
                  {trecho.texto}
                </Typography>
              </Box>
            </Box>
          ))}

          <Box sx={{ textAlign: 'center', mt: 4 }}>
            <Button
              variant="contained"
              startIcon={<DirectionsRunIcon />}
              onClick={() => navigate('/run')}
              sx={{ fontWeight: 700, bgcolor: '#1B2A6B', '&:hover': { bgcolor: '#2E4FA3' }, px: 4, py: 1.2 }}
            >
              Quero correr os 7 km
            </Button>
            <Button
              startIcon={<ArrowBackIcon />}
              onClick={() => navigate('/run')}
              sx={{ ml: 1, fontWeight: 700, color: '#44506B' }}
            >
              Voltar
            </Button>
          </Box>
        </Paper>
      </Container>
    </Box>
  )
}

export default Percurso7kmPage

import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Alert, AppBar, Box, Button, Checkbox, Container, Dialog, DialogActions,
  DialogContent, DialogTitle, Divider, Drawer, FormControlLabel, Grid,
  IconButton, Link, List, ListItemButton, ListItemText, Paper,
  TextField, Toolbar, Typography,
} from '@mui/material'
import MenuIcon from '@mui/icons-material/Menu'
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft'
import ChevronRightIcon from '@mui/icons-material/ChevronRight'
import FavoriteIcon from '@mui/icons-material/Favorite'
import InstagramIcon from '@mui/icons-material/Instagram'
import VolunteerActivismIcon from '@mui/icons-material/VolunteerActivism'
import donationGoalService, { DonationGoalItem } from '../services/donationGoalService'

// Quantidade de fotos em /galeria (foto-01..foto-NN).
const GALLERY_COUNT = 22
import { getApiErrorMessage } from '../utils/apiError'

const navItems = [
  { label: 'Início', target: 'inicio' },
  { label: 'Nossa História', target: 'historia' },
  { label: 'Nosso Impacto', target: 'impacto' },
  { label: '2026', target: 'desafio' },
  { label: 'Itens da Cesta', target: 'cesta' },
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

function fmtQty(value: number, unit: string): string {
  const rounded = Number(value)
  const text = Number.isInteger(rounded)
    ? rounded.toLocaleString('pt-BR')
    : rounded.toLocaleString('pt-BR', { maximumFractionDigits: 2 })
  return `${text} ${unit}`
}

// Embalagem fracionada vira g/ml para o doador entender (0,5 kg → 500 g; 0,9 L → 900 ml)
function fmtPkg(value: number, unit: string): string {
  if (value < 1 && unit === 'kg') return `${Math.round(value * 1000)} g`
  if (value < 1 && unit === 'L') return `${Math.round(value * 1000)} ml`
  return fmtQty(value, unit)
}

function LandingPage() {
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)
  const [galleryIndex, setGalleryIndex] = useState(0)
  const [goalItems, setGoalItems] = useState<DonationGoalItem[]>([])
  const [donorNames, setDonorNames] = useState<string[]>([])
  const [pledgeItem, setPledgeItem] = useState<DonationGoalItem | null>(null)
  const [pledgeQty, setPledgeQty] = useState('')
  const [pledgeName, setPledgeName] = useState('')
  const [pledgeContact, setPledgeContact] = useState('')
  const [pledgeAnon, setPledgeAnon] = useState(false)
  const [pledgeSelfDelivery, setPledgeSelfDelivery] = useState(false)
  const [pledgeSending, setPledgeSending] = useState(false)
  const [pledgeError, setPledgeError] = useState<string | null>(null)
  const [pledgeDone, setPledgeDone] = useState(false)

  const loadGoals = async () => {
    try {
      const [items, names] = await Promise.all([
        donationGoalService.list(),
        donationGoalService.donors(),
      ])
      setGoalItems(items)
      setDonorNames(names)
    } catch {
      // A lista de doação é informativa: se a API não responder, a página continua funcionando.
    }
  }
  useEffect(() => { loadGoals() }, [])

  const openPledge = (item: DonationGoalItem) => {
    setPledgeItem(item)
    setPledgeQty('')
    setPledgeError(null)
    setPledgeDone(false)
    setPledgeSelfDelivery(false)
  }

  const submitPledge = async () => {
    if (!pledgeItem) return
    const packages = Number(pledgeQty)
    if (!Number.isFinite(packages) || packages < 1) {
      setPledgeError('Informe quantos pacotes você vai doar (mínimo 1).')
      return
    }
    if (!pledgeAnon && !pledgeName.trim()) {
      setPledgeError('Informe seu nome ou o da sua empresa (ou marque doação anônima).')
      return
    }
    if (!pledgeSelfDelivery && !pledgeContact.trim()) {
      setPledgeError('Informe um contato para combinarmos a retirada — ou marque que você mesmo leva até o MEVAM.')
      return
    }
    setPledgeSending(true)
    setPledgeError(null)
    try {
      await donationGoalService.pledge({
        goalItemId: pledgeItem.id,
        packages: Math.round(packages),
        donorName: pledgeAnon ? undefined : pledgeName.trim(),
        donorContact: pledgeContact.trim() || undefined,
        anonymous: pledgeAnon,
        selfDelivery: pledgeSelfDelivery,
      })
      setPledgeDone(true)
      loadGoals()
    } catch (error) {
      setPledgeError(getApiErrorMessage(error, 'Não foi possível registrar a promessa de doação.'))
    } finally {
      setPledgeSending(false)
    }
  }

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
                pt: 0,
                pb: 2.5,
                borderRadius: 2,
                mt: { sm: 3 },
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

      {/* Galeria — veja como foi o ano anterior */}
      <Box component="section" sx={{ py: { xs: 7, md: 10 }, bgcolor: '#FFFFFF' }}>
        <Container maxWidth="md" sx={{ textAlign: 'center' }}>
          <Typography variant="h4" component="h2" sx={{ color: '#1B2A6B', fontWeight: 800, mb: 2 }}>
            Veja como foi o ano anterior
          </Typography>
          <Typography sx={{ fontSize: '1.05rem', color: '#44506B', mb: 4, maxWidth: 640, mx: 'auto' }}>
            Momentos de solidariedade que já transformaram vidas — cada entrega
            é uma família recebendo dignidade.
          </Typography>

          {/* Foto em destaque + setas — foto-01 é a fixa de abertura */}
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
                bgcolor: 'rgba(255,255,255,0.92)', color: '#1B2A6B',
                '&:hover': { bgcolor: '#FFFFFF' },
              }}
            >
              <ChevronLeftIcon fontSize="large" />
            </IconButton>
            <IconButton
              onClick={() => setGalleryIndex((galleryIndex + 1) % GALLERY_COUNT)}
              aria-label="Próxima foto"
              sx={{
                position: 'absolute', right: 8, top: '50%', transform: 'translateY(-50%)',
                bgcolor: 'rgba(255,255,255,0.92)', color: '#1B2A6B',
                '&:hover': { bgcolor: '#FFFFFF' },
              }}
            >
              <ChevronRightIcon fontSize="large" />
            </IconButton>
          </Box>

          {/* Miniaturas clicáveis */}
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

      {/* Lista de itens para doação */}
      <Box id="cesta" component="section" sx={{ py: { xs: 7, md: 10 }, bgcolor: '#FFFFFF' }}>
        <Container maxWidth="lg">
          <Typography variant="h4" component="h2" sx={{ color: '#1B2A6B', fontWeight: 800, mb: 2, textAlign: 'center' }}>
            O que cada cesta precisa?
          </Typography>
          <Typography sx={{ fontSize: '1.05rem', color: '#44506B', mb: 5, maxWidth: 640, mx: 'auto', textAlign: 'center' }}>
            Esta é a lista do que precisamos para alcançar as 500 famílias.
            O que cada doador prometer é descontado do pendente em tempo real —
            assim ninguém compra duas vezes a mesma coisa.
          </Typography>
          {goalItems.length > 0 ? (
            <Grid container spacing={3}>
              {goalItems.map((item) => (
                <Grid item xs={12} sm={6} md={4} key={item.id}>
                  <Paper
                    elevation={0}
                    sx={{
                      p: 3,
                      height: '100%',
                      border: '1px solid #E3E8EF',
                      borderRadius: 3,
                      borderTop: '4px solid #FFC42E',
                      bgcolor: '#FFFFFF',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: 0.5,
                    }}
                  >
                    <Typography variant="subtitle1" sx={{ fontWeight: 800, color: '#1B2A6B' }}>
                      {item.name}
                    </Typography>
                    <Typography variant="caption" sx={{ color: '#44506B' }}>
                      {`${Math.round(item.perFamilyQuantity / item.packageSize)} ${Math.round(item.perFamilyQuantity / item.packageSize) === 1 ? 'pacote' : 'pacotes'} de ${fmtPkg(item.packageSize, item.unit)} por família`}
                    </Typography>
                    <Box sx={{ mt: 1 }}>
                      <Typography variant="h5" sx={{ fontWeight: 800, color: '#19B8D2' }}>
                        {fmtQty(item.remainingQuantity, item.unit)}
                      </Typography>
                      <Typography variant="body2" sx={{ color: '#44506B' }}>
                        pendentes de {fmtQty(item.neededQuantity, item.unit)} necessários
                        {item.remainingPackages > 0 && (
                          <> ≈ {item.remainingPackages.toLocaleString('pt-BR')} pacotes de {fmtPkg(item.packageSize, item.unit)}</>
                        )}
                      </Typography>
                    </Box>
                    <Button
                      variant="outlined"
                      size="small"
                      onClick={() => openPledge(item)}
                      disabled={item.remainingQuantity <= 0}
                      sx={{
                        mt: 'auto',
                        pt: 0,
                        alignSelf: 'flex-start',
                        borderColor: '#19B8D2',
                        color: '#19B8D2',
                        fontWeight: 700,
                        '&:hover': { borderColor: '#0E93A8', color: '#0E93A8' },
                      }}
                    >
                      {item.remainingQuantity <= 0 ? 'Coberto' : 'Quero doar'}
                    </Button>
                  </Paper>
                </Grid>
              ))}
            </Grid>
          ) : (
            <Typography sx={{ textAlign: 'center', color: '#44506B' }}>
              Carregando a lista de necessidades…
            </Typography>
          )}

          {donorNames.length > 0 && (
            <Paper
              elevation={0}
              sx={{
                mt: 6,
                p: 3,
                borderRadius: 3,
                bgcolor: '#FBF9F1',
                border: '1px solid #E3E8EF',
                textAlign: 'center',
              }}
            >
              <Typography variant="subtitle1" sx={{ fontWeight: 800, color: '#1B2A6B', mb: 1 }}>
                <VolunteerActivismIcon sx={{ verticalAlign: 'middle', mr: 1, color: '#19B8D2' }} />
                Obrigado a quem já doou
              </Typography>
              <Typography sx={{ color: '#44506B', lineHeight: 1.9 }}>
                {donorNames.join(', ')}
              </Typography>
            </Paper>
          )}
        </Container>
      </Box>

      {/* Diálogo de promessa de doação */}
      <Dialog open={pledgeItem !== null} onClose={() => setPledgeItem(null)} maxWidth="xs" fullWidth>
        <DialogTitle sx={{ fontWeight: 800, color: '#1B2A6B' }}>
          Doar {pledgeItem?.name}
        </DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
          {pledgeDone ? (
            <Alert severity="success">
              Promessa registrada. Nossa equipe vai validar quando os itens
              chegarem à igreja — aí seu nome entra na lista de agradecimentos.
            </Alert>
          ) : (
            <>
              {pledgeItem && (
                <Typography variant="body2" sx={{ color: '#44506B' }}>
                  Pendentes: {fmtQty(pledgeItem.remainingQuantity, pledgeItem.unit)}
                  {pledgeItem.remainingPackages > 0 && (
                    <> ≈ {pledgeItem.remainingPackages.toLocaleString('pt-BR')} pacotes de {fmtPkg(pledgeItem.packageSize, pledgeItem.unit)}</>
                  )}
                </Typography>
              )}
              <TextField
                label={`Quantidade de pacotes de ${fmtPkg(pledgeItem?.packageSize ?? 0, pledgeItem?.unit ?? '')}`}
                type="number"
                value={pledgeQty}
                onChange={(e) => setPledgeQty(e.target.value)}
                inputProps={{ min: 1 }}
                required
                fullWidth
              />
              {pledgeItem && Number(pledgeQty) > 0 && (
                <Paper
                  elevation={0}
                  sx={{
                    p: 2,
                    textAlign: 'center',
                    bgcolor: '#FFF6D6',
                    border: '2px solid #FFC42E',
                    borderRadius: 2,
                  }}
                >
                  <Typography variant="h6" sx={{ fontWeight: 800, color: '#1B2A6B', lineHeight: 1.3 }}>
                    {Number(pledgeQty).toLocaleString('pt-BR')} pacote(s) de {fmtPkg(pledgeItem.packageSize, pledgeItem.unit)}
                  </Typography>
                  <Typography sx={{ fontWeight: 700, color: '#44506B' }}>
                    = {fmtQty(Number(pledgeQty) * pledgeItem.packageSize, pledgeItem.unit)} no total
                  </Typography>
                </Paper>
              )}
              <TextField
                label="Seu nome ou empresa"
                value={pledgeName}
                onChange={(e) => setPledgeName(e.target.value)}
                disabled={pledgeAnon}
                fullWidth
              />
              <TextField
                label={pledgeSelfDelivery ? 'Contato (opcional)' : 'Contato'}
                value={pledgeContact}
                onChange={(e) => setPledgeContact(e.target.value)}
                required={!pledgeSelfDelivery}
                helperText={pledgeSelfDelivery
                  ? 'Opcional — você mesmo leva a doação'
                  : 'Obrigatório — para combinarmos a retirada com você'}
                fullWidth
              />
              <FormControlLabel
                control={<Checkbox checked={pledgeSelfDelivery} onChange={(e) => setPledgeSelfDelivery(e.target.checked)} />}
                label="Eu mesmo levo a doação até o MEVAM Petrópolis"
              />
              <FormControlLabel
                control={<Checkbox checked={pledgeAnon} onChange={(e) => setPledgeAnon(e.target.checked)} />}
                label="Doação anônima"
              />
              {pledgeError && <Alert severity="error">{pledgeError}</Alert>}
            </>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          {pledgeDone ? (
            <Button onClick={() => setPledgeItem(null)} variant="contained">Fechar</Button>
          ) : (
            <>
              <Button onClick={() => setPledgeItem(null)}>Cancelar</Button>
              <Button onClick={submitPledge} variant="contained" disabled={pledgeSending}>
                {pledgeSending ? 'Registrando…' : 'Prometer doação'}
              </Button>
            </>
          )}
        </DialogActions>
      </Dialog>

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
                sx={{ height: { xs: 70, md: 90 }, width: 'auto', bgcolor: '#FFFFFF', px: 1, pt: 0, pb: 2, borderRadius: 2 }}
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

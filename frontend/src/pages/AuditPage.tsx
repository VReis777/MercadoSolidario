import { useCallback, useEffect, useState, type KeyboardEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Alert, AppBar, Box, Button, Card, CardContent, Chip, CircularProgress, Container, Grid, IconButton,
  List, ListItem, ListItemIcon, ListItemText, Paper, Tab, Tabs, Toolbar, Tooltip, Typography,
} from '@mui/material'
import LogoutIcon from '@mui/icons-material/Logout'
import DashboardIcon from '@mui/icons-material/Dashboard'
import RefreshIcon from '@mui/icons-material/Refresh'
import TipsIcon from '@mui/icons-material/TipsAndUpdates'
import CheckIcon from '@mui/icons-material/CheckCircleOutline'
import VolunteerActivismIcon from '@mui/icons-material/VolunteerActivism'
import PublicIcon from '@mui/icons-material/Public'
import { useAuth } from '../auth/AuthContext'
import ThemeToggle from '../components/ThemeToggle'
import HistoryTab from '../components/audit/HistoryTab'
import RegistrationsTab from '../components/audit/RegistrationsTab'
import BasketTab from '../components/audit/BasketTab'
import campaignService, { CampaignOverview } from '../services/campaignService'
import { getApiErrorMessage } from '../utils/apiError'
import { formatRole } from '../utils/roleLabels'

const tips = [
  'Peça CPF e telefone em todo cadastro novo: é o que permite o cruzamento automático com 2024 e 2025 (a planilha de 2024 não tem CPF).',
  'Família marcada em vermelho (mesmo CPF) já foi contemplada: passe pela triagem antes de aprovar, priorizando quem ainda não recebeu.',
  'Coincidência só por nome (laranja) precisa de conferência humana: confirme telefone ou endereço antes de decidir.',
  'Mantenha a situação do cadastro atualizada (Em análise → Aprovada → Contemplada): o painel só conta o que está registrado.',
  'Registre toda entrada e saída de itens em Estoque; o painel avisa quando um item da cesta está sem estoque.',
  'Use a origem/indicação (Reage Mãe, igreja, projetos parceiros) para medir de onde vêm as famílias e buscar regiões ainda não alcançadas.',
  'Importe a planilha de cada campanha logo após a entrega: o histórico fica auditável e evita depender de arquivos soltos.',
]

function StatCard({ label, value, hint, onClick }: { label: string; value: string | number; hint?: string; onClick?: () => void }) {
  return (
    <Card
      sx={{
        height: '100%',
        ...(onClick ? { cursor: 'pointer', transition: 'box-shadow .15s', '&:hover': { boxShadow: 4 } } : {}),
      }}
      {...(onClick ? { onClick, role: 'button', tabIndex: 0,
        onKeyDown: (e: KeyboardEvent) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); onClick() } } } : {})}
    >
      <CardContent>
        <Typography variant="body2" color="text.secondary">{label}</Typography>
        <Typography variant="h4" sx={{ fontWeight: 700, color: 'primary.main' }}>{value}</Typography>
        {hint && <Typography variant="caption" color="text.secondary">{hint}</Typography>}
      </CardContent>
    </Card>
  )
}

function AuditPage() {
  const navigate = useNavigate()
  const { user, logout, hasAnyRole } = useAuth()
  const canManage = hasAnyRole(['ROLE_ADMIN', 'ROLE_RH_TESOURARIA'])
  const [tab, setTab] = useState(0)
  // Filtros aplicados quando um card/alerta leva o usuário a uma aba específica.
  const [historyYear, setHistoryYear] = useState<number | ''>('')
  const [regOnlyHistory, setRegOnlyHistory] = useState(false)
  const [navNonce, setNavNonce] = useState(0)

  const goToHistory = (year: number | '') => {
    setHistoryYear(year)
    setNavNonce((n) => n + 1)
    setTab(1)
  }

  const goToRegistrations = (withHistory: boolean) => {
    setRegOnlyHistory(withHistory)
    setNavNonce((n) => n + 1)
    setTab(2)
  }

  const goToBasket = () => setTab(3)

  /** Direciona cada alerta da auditoria automática para a lista correspondente. */
  const alertTarget = (alert: string): (() => void) | undefined => {
    if (alert.includes('já foram contempladas antes') || alert.includes('mesmo nome de alguém contemplado'))
      return () => goToRegistrations(true)
    if (alert.includes('CADASTRADA')) return () => goToRegistrations(false)
    if (alert.includes('sem CPF') || alert.includes('mais de uma campanha') || alert.includes('não importado'))
      return () => goToHistory('')
    if (alert.includes('cesta') || alert.includes('estoque')) return goToBasket
    return undefined
  }
  const [overview, setOverview] = useState<CampaignOverview | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadOverview = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setOverview(await campaignService.overview())
    } catch (e) {
      setError(getApiErrorMessage(e, 'Não foi possível carregar o painel.'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadOverview() }, [loadOverview])

  const campaignYear = overview?.campaignYear ?? new Date().getFullYear()
  const historyYears = Object.keys(overview?.historicalByYear ?? {}).map(Number).sort((a, b) => b - a)
  const roleLabel = user?.roles?.map((r) => formatRole(r)).join(', ')

  const handleLogout = async () => {
    await logout()
    navigate('/entrar')
  }

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default', pb: 6, overflowX: 'hidden' }}>
      <AppBar position="static" elevation={0} color="primary">
        <Toolbar sx={{ gap: 1, flexWrap: 'wrap' }}>
          <Typography variant="h6" component="h1" sx={{ flexGrow: 1 }}>
            Mercado Solidário — Painel de Gestão
          </Typography>
          <Typography variant="body2" sx={{ display: { xs: 'none', md: 'block' } }}>
            {user?.fullName}{roleLabel ? ` · ${roleLabel}` : ''}
          </Typography>
          <ThemeToggle />
          <Tooltip title="Doações">
            <IconButton color="inherit" onClick={() => navigate('/doacoes')} aria-label="Abrir doações">
              <VolunteerActivismIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="Página pública">
            <IconButton color="inherit" onClick={() => navigate('/')} aria-label="Ir para a página pública">
              <PublicIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="Painel anterior e demais módulos">
            <IconButton color="inherit" onClick={() => navigate('/modulos')} aria-label="Abrir módulos">
              <DashboardIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="Sair">
            <IconButton color="inherit" onClick={handleLogout} aria-label="Sair">
              <LogoutIcon />
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>

      <Container maxWidth="xl" sx={{ mt: 3 }}>
        <Paper sx={{ mb: 3 }}>
          <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="scrollable" scrollButtons="auto" allowScrollButtonsMobile>
            <Tab label="Visão geral" />
            <Tab label="Beneficiados anteriores" />
            <Tab label={`Cadastrados ${campaignYear}`} />
            <Tab label="Itens da cesta" />
          </Tabs>
        </Paper>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }} action={<Button color="inherit" onClick={loadOverview}>Tentar de novo</Button>}>
            {error}
          </Alert>
        )}

        {tab === 0 && (
          loading ? (
            <Box sx={{ textAlign: 'center', py: 6 }}><CircularProgress /></Box>
          ) : overview && (
            <Grid container spacing={2}>
              <Grid item xs={12} sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                <Button startIcon={<RefreshIcon />} onClick={loadOverview}>Atualizar</Button>
              </Grid>
              {historyYears.length === 0 ? (
                <Grid item xs={12} md={4}>
                  <StatCard label="Contemplados em anos anteriores" value="—" hint="Histórico ainda não importado"
                    onClick={() => goToHistory('')} />
                </Grid>
              ) : historyYears.map((y) => (
                <Grid item xs={6} md={2} key={y}>
                  <StatCard label={`Contemplados ${y}`} value={overview.historicalByYear[y]}
                    hint={overview.historicalWithoutCpfByYear[y] ? `${overview.historicalWithoutCpfByYear[y]} sem CPF` : 'todos com CPF'}
                    onClick={() => goToHistory(y)} />
                </Grid>
              ))}
              <Grid item xs={6} md={2}>
                <StatCard label={`Cadastrados ${campaignYear}`} value={overview.registeredThisCampaign} hint="pela data de cadastro"
                  onClick={() => goToRegistrations(false)} />
              </Grid>
              <Grid item xs={6} md={2}>
                <StatCard label="Pessoas nessas famílias" value={overview.peopleInRegisteredFamilies} hint="soma dos moradores"
                  onClick={() => goToRegistrations(false)} />
              </Grid>
              <Grid item xs={6} md={2}>
                <StatCard label="Já contemplados antes" value={overview.registeredWithHistoryByCpf}
                  hint={`+ ${overview.registeredWithHistoryByName} possível(is) por nome`}
                  onClick={() => goToRegistrations(true)} />
              </Grid>
              <Grid item xs={6} md={2}>
                <StatCard label="Itens da cesta" value={overview.activeProducts} hint={`${overview.productsWithoutStock} sem estoque`}
                  onClick={goToBasket} />
              </Grid>

              <Grid item xs={12} md={7}>
                <Paper sx={{ p: 2, height: '100%' }}>
                  <Typography variant="h6" gutterBottom>Auditoria automática</Typography>
                  {overview.alerts.length === 0 ? (
                    <Alert severity="success" icon={<CheckIcon />}>Nenhuma pendência detectada.</Alert>
                  ) : (
                    <StackedAlerts alerts={overview.alerts} onAlertClick={alertTarget} />
                  )}
                  <Box sx={{ mt: 2, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                    <Chip variant="outlined" label="Vermelho = mesmo CPF (confirmado)" color="error" />
                    <Chip variant="outlined" label="Laranja = mesmo nome (conferir)" color="warning" />
                  </Box>
                </Paper>
              </Grid>
              <Grid item xs={12} md={5}>
                <Paper sx={{ p: 2, height: '100%' }}>
                  <Typography variant="h6" gutterBottom>Dicas para alcançar mais famílias</Typography>
                  <List dense>
                    {tips.map((t) => (
                      <ListItem key={t} alignItems="flex-start" disableGutters>
                        <ListItemIcon sx={{ minWidth: 34, mt: 0.5 }}><TipsIcon color="secondary" fontSize="small" /></ListItemIcon>
                        <ListItemText primary={t} />
                      </ListItem>
                    ))}
                  </List>
                </Paper>
              </Grid>
            </Grid>
          )
        )}

        {tab === 1 && (
          <HistoryTab key={`hist-${navNonce}`} years={historyYears} origins={overview?.origins ?? []} campaignYear={campaignYear}
            canImport={canManage} onChanged={loadOverview} initialYear={historyYear} />
        )}
        {tab === 2 && <RegistrationsTab key={`reg-${navNonce}`} campaignYear={campaignYear}
          canImport={canManage} initialOnlyWithHistory={regOnlyHistory} onChanged={loadOverview} />}
        {tab === 3 && <BasketTab canEdit={canManage} onChanged={loadOverview} />}
      </Container>
    </Box>
  )
}

function StackedAlerts({ alerts, onAlertClick }: { alerts: string[]; onAlertClick: (alert: string) => (() => void) | undefined }) {
  return (
    <Box sx={{ display: 'grid', gap: 1 }}>
      {alerts.map((a) => {
        const target = onAlertClick(a)
        return (
          <Alert key={a} severity={a.includes('já foram contempladas') ? 'error' : 'warning'}
            {...(target ? { onClick: target, sx: { cursor: 'pointer', '&:hover': { filter: 'brightness(0.97)' } } } : {})}>
            {a}
            {target && (
              <Typography variant="caption" display="block" sx={{ mt: 0.5, fontWeight: 600 }}>
                Clique para ver a lista
              </Typography>
            )}
          </Alert>
        )
      })}
    </Box>
  )
}

export default AuditPage

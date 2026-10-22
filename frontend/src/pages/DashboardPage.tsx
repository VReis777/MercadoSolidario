import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Container,
  Box,
  Typography,
  Grid,
  Paper,
  Card,
  CardContent,
  CardActionArea,
  AppBar,
  Toolbar,
  IconButton,
  Menu,
  MenuItem,
  Divider,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Drawer,
  useMediaQuery,
  Chip,
  Alert,
  Button,
  CircularProgress,
  Stack,
  TextField,
} from '@mui/material'
import {
  Menu as MenuIcon,
  Logout as LogoutIcon,
  People as PeopleIcon,
  Inventory as InventoryIcon,
  VolunteerActivism as VolunteerActivismIcon,
  LocalShipping as LocalShippingIcon,
  Business as BusinessIcon,
  Assessment as AssessmentIcon,
  Settings as SettingsIcon,
  PriorityHigh as PriorityHighIcon,
  Close as CloseIcon,
  Refresh as RefreshIcon,
  Assignment as ServiceRequestsIcon,
  PersonAdd as UserManagementIcon,
  Public as PublicIcon,
} from '@mui/icons-material'
import { styled } from '@mui/material/styles'
import { useAuth } from '../auth/AuthContext'
import { dashboardService, type DashboardSummary } from '../services/dashboardService'
import ThemeToggle from '../components/ThemeToggle'
import { formatRole } from '../utils/roleLabels'

const DashboardContainer = styled(Box)(({ theme }) => ({
  minHeight: '100vh',
  background: theme.palette.background.default,
}))

const drawerWidth = 280

function toLocalDateString(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatDateTime(value: string | null | undefined): string {
  return value ? new Date(value).toLocaleString('pt-BR') : 'Data não informada'
}

const familyStatusLabels: Record<string, string> = {
  CADASTRADA: 'Cadastradas',
  EM_ANALISE: 'Em análise',
  APROVADA: 'Aprovadas',
  CONTEMPLADA: 'Contempladas',
  ATENDIDA: 'Atendidas',
  INATIVA: 'Inativas',
  CANCELADA: 'Canceladas',
}

interface Module {
  id: string
  title: string
  description: string
  icon: React.ReactNode
  requiredRoles: string[]
}

function DashboardPage() {
  const isMobile = useMediaQuery((theme: any) => theme.breakpoints.down('md'))
  const [mobileOpen, setMobileOpen] = useState(false)
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null)
  const { user, logout, hasAnyRole } = useAuth()
  const navigate = useNavigate()
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [refreshKey, setRefreshKey] = useState(0)
  const [period, setPeriod] = useState(() => {
    const today = new Date()
    const from = new Date(today)
    from.setDate(today.getDate() - 29)
    return { from: toLocalDateString(from), to: toLocalDateString(today) }
  })

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(null)
    dashboardService.summary(period.from, period.to)
      .then((data) => {
        if (active) setSummary(data)
      })
      .catch((requestError: unknown) => {
        if (active) {
          const message = requestError instanceof Error ? requestError.message : 'Não foi possível carregar o dashboard.'
          setError(message)
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => { active = false }
  }, [period.from, period.to, refreshKey])

  const setPresetPeriod = (days: number) => {
    const to = new Date()
    const from = new Date(to)
    from.setDate(to.getDate() - days + 1)
    setPeriod({ from: toLocalDateString(from), to: toLocalDateString(to) })
  }

  const setCurrentMonth = () => {
    const to = new Date()
    setPeriod({
      from: toLocalDateString(new Date(to.getFullYear(), to.getMonth(), 1)),
      to: toLocalDateString(to),
    })
  }

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen)
  }

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget)
  }

  const handleMenuClose = () => {
    setAnchorEl(null)
  }

  const handleLogout = async () => {
    handleMenuClose()
    await logout()
    navigate('/entrar')
  }

  const modules: Module[] = [
    {
      id: 'audit',
      title: 'Painel de Gestão',
      description: 'Beneficiados anteriores, cadastros do ano e itens',
      icon: <AssessmentIcon />,
      requiredRoles: ['ROLE_ADMIN', 'ROLE_RH_TESOURARIA', 'ROLE_DIACONO'],
    },
    {
      id: 'priority',
      title: 'Prioridade configurável',
      description: 'Regras versionadas e explicáveis',
      icon: <PriorityHighIcon />,
      requiredRoles: ['ROLE_ADMIN', 'ROLE_RH_TESOURARIA'],
    },
    {
      id: 'families',
      title: 'Famílias',
      description: 'Cadastro e acompanhamento das famílias',
      icon: <PeopleIcon />,
      requiredRoles: ['ROLE_ADMIN', 'ROLE_RH_TESOURARIA', 'ROLE_DIACONO'],
    },
    {
      id: 'service-requests',
      title: 'Solicitações de assistência',
      description: 'Acompanhamento de pedidos recebidos',
      icon: <ServiceRequestsIcon />,
      requiredRoles: ['ROLE_ADMIN', 'ROLE_RH_TESOURARIA', 'ROLE_DIACONO'],
    },
    {
      id: 'stock',
      title: 'Estoque',
      description: 'Produtos e movimentações',
      icon: <InventoryIcon />,
      requiredRoles: ['ROLE_ADMIN', 'ROLE_RH_TESOURARIA', 'ROLE_DIACONO'],
    },
    {
      id: 'donations',
      title: 'Doações',
      description: 'Doações financeiras e produtos',
      icon: <VolunteerActivismIcon />,
      requiredRoles: ['ROLE_ADMIN', 'ROLE_RH_TESOURARIA'],
    },
    {
      id: 'distribution',
      title: 'Distribuição',
      description: 'Registro dos atendimentos',
      icon: <LocalShippingIcon />,
      requiredRoles: ['ROLE_ADMIN', 'ROLE_RH_TESOURARIA', 'ROLE_DIACONO'],
    },
    {
      id: 'sponsors',
      title: 'Patrocinadores',
      description: 'Empresas e parceiros',
      icon: <BusinessIcon />,
      requiredRoles: ['ROLE_ADMIN', 'ROLE_RH_TESOURARIA'],
    },
    {
      id: 'reports',
      title: 'Relatórios',
      description: 'Consultas e exportações',
      icon: <AssessmentIcon />,
      requiredRoles: ['ROLE_ADMIN', 'ROLE_RH_TESOURARIA'],
    },
    {
      id: 'settings',
      title: 'Configurações',
      description: 'Configurações do sistema',
      icon: <SettingsIcon />,
      requiredRoles: ['ROLE_ADMIN'],
    },
    {
      id: 'users',
      title: 'Usuários',
      description: 'Cadastrar pessoas e enviar convites de acesso',
      icon: <UserManagementIcon />,
      requiredRoles: ['ROLE_ADMIN'],
    },
  ]

  const filteredModules = modules.filter(module =>
    hasAnyRole(module.requiredRoles)
  )

  const drawerContent = (
    <Box>
      <Toolbar sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', px: 2 }}>
        <Typography variant="h6" noWrap component="div" sx={{ fontWeight: 700, color: 'primary.main' }}>
          MERCADO SOLIDÁRIO
        </Typography>
        {isMobile && (
          <IconButton onClick={handleDrawerToggle}>
            <CloseIcon />
          </IconButton>
        )}
      </Toolbar>
      <Divider />
      <List sx={{ px: 2 }}>
        {filteredModules.map((module) => (
          <ListItem
            key={module.id}
            button
            onClick={() => {
              if (module.id === 'audit') {
                navigate('/painel')
              } else if (module.id === 'families') {
                navigate('/familias')
              } else if (module.id === 'service-requests') {
                navigate('/solicitacoes')
              } else if (module.id === 'priority') {
                navigate('/prioridades')
              } else if (module.id === 'stock') {
                navigate('/estoque')
              } else if (module.id === 'donations') {
                navigate('/doacoes')
              } else if (module.id === 'distribution') {
                navigate('/distribuicoes')
              } else if (module.id === 'sponsors') {
                navigate('/patrocinadores')
              } else if (module.id === 'reports') {
                navigate('/relatorios')
              } else if (module.id === 'settings') {
                navigate('/configuracoes')
              } else if (module.id === 'users') {
                navigate('/admin/usuarios')
              }
              handleDrawerToggle()
            }}
          >
            <ListItemIcon>{module.icon}</ListItemIcon>
            <ListItemText
              primary={module.title}
              secondary={module.description}
              secondaryTypographyProps={{ variant: 'caption' }}
            />
          </ListItem>
        ))}
      </List>
      <Divider />
      <List sx={{ px: 2 }}>
        <ListItem
          button
          onClick={() => {
            navigate('/')
            if (isMobile) handleDrawerToggle()
          }}
        >
          <ListItemIcon><PublicIcon /></ListItemIcon>
          <ListItemText
            primary="Página pública"
            secondary="Ver o site como visitante"
            secondaryTypographyProps={{ variant: 'caption' }}
          />
        </ListItem>
      </List>
    </Box>
  )

  return (
    <DashboardContainer>
      <AppBar position="static" elevation={0} color="primary">
        <Toolbar>
          <IconButton
            color="inherit"
            edge="start"
            onClick={handleDrawerToggle}
            sx={{ mr: 2, display: { md: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
            Dashboard
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <IconButton color="inherit" onClick={() => navigate('/')} aria-label="Ir para a página pública">
              <PublicIcon />
            </IconButton>
            <ThemeToggle />
            <Chip
              label={user?.roles[0] ? formatRole(user.roles[0]) : 'USER'}
              size="small"
              sx={{ bgcolor: 'rgba(255,255,255,0.2)', color: 'white' }}
            />
            <Typography variant="body2" sx={{ display: { xs: 'none', sm: 'block' } }}>
              {user?.fullName}
            </Typography>
            <IconButton
              color="inherit"
              onClick={handleMenuOpen}
              sx={{ display: { xs: 'none', sm: 'flex' } }}
            >
              <LogoutIcon />
            </IconButton>
          </Box>
        </Toolbar>
      </AppBar>

      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleMenuClose}
        onClick={handleMenuClose}
      >
        <MenuItem onClick={handleLogout}>
          <ListItemIcon>
            <LogoutIcon fontSize="small" />
          </ListItemIcon>
          Sair
        </MenuItem>
      </Menu>

      <Box component="nav" sx={{ width: { md: drawerWidth }, flexShrink: { md: 0 } }}>
        <Drawer
          variant={isMobile ? 'temporary' : 'permanent'}
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{
            keepMounted: true,
          }}
          sx={{
            display: { xs: 'block', md: 'none' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
          }}
        >
          {drawerContent}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', md: 'block' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
          }}
          open
        >
          {drawerContent}
        </Drawer>
      </Box>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: { md: `calc(100% - ${drawerWidth}px)` },
          ml: { md: `${drawerWidth}px` },
        }}
      >
        <Toolbar />
        <Container maxWidth="xl">
          <Typography variant="h4" component="h1" gutterBottom sx={{ fontWeight: 700, color: 'primary.main' }}>
            Bem-vindo, {user?.fullName?.split(' ')[0]}!
          </Typography>
          <Typography variant="body1" sx={{ mb: 4, color: 'text.secondary' }}>
            Visão geral do Mercado Solidário
          </Typography>

          {hasAnyRole(['ROLE_ADMIN', 'ROLE_RH_TESOURARIA', 'ROLE_DIACONO']) && (
            <Grid container spacing={2} sx={{ mb: 3 }}>
              <Grid item xs={12} sm={6} md={4}>
                <Card>
                  <CardActionArea onClick={() => navigate('/solicitacoes')} sx={{ height: '100%' }}>
                    <CardContent>
                      <Stack direction="row" spacing={1.5} alignItems="center">
                        <ServiceRequestsIcon color="primary" />
                        <Box>
                          <Typography variant="h6">Solicitações de assistência</Typography>
                          <Typography variant="body2" color="text.secondary">
                            Consultar e acompanhar pedidos recebidos
                          </Typography>
                        </Box>
                      </Stack>
                    </CardContent>
                  </CardActionArea>
                </Card>
              </Grid>
            </Grid>
          )}

          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} sx={{ mb: 3 }} alignItems={{ sm: 'center' }}>
            <Button variant="outlined" onClick={() => setPresetPeriod(1)}>Hoje</Button>
            <Button variant="outlined" onClick={() => setPresetPeriod(7)}>7 dias</Button>
            <Button variant="outlined" onClick={() => setPresetPeriod(30)}>30 dias</Button>
            <Button variant="outlined" onClick={setCurrentMonth}>Mês atual</Button>
            <TextField
              label="De"
              type="date"
              size="small"
              value={period.from}
              onChange={(event) => setPeriod((current) => ({ ...current, from: event.target.value }))}
              InputLabelProps={{ shrink: true }}
            />
            <TextField
              label="Até"
              type="date"
              size="small"
              value={period.to}
              onChange={(event) => setPeriod((current) => ({ ...current, to: event.target.value }))}
              InputLabelProps={{ shrink: true }}
            />
            <Button
              variant="contained"
              startIcon={<RefreshIcon />}
              onClick={() => setRefreshKey((key) => key + 1)}
              disabled={loading}
            >
              Atualizar
            </Button>
          </Stack>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Período selecionado: {period.from} a {period.to}
          </Typography>

          {error && <Alert severity="error" sx={{ mb: 2 }} action={
            <Button color="inherit" size="small" onClick={() => setRefreshKey((key) => key + 1)}>Tentar novamente</Button>
          }>{error}</Alert>}
          {loading && <Box sx={{ display: 'flex', justifyContent: 'center', py: 5 }}><CircularProgress /></Box>}

          {!loading && !error && summary && <>
            <Grid container spacing={2}>
              {[
                ['Famílias cadastradas', summary.families.total],
                ['Novos cadastros no período', summary.families.createdInPeriod],
                ['Em análise', summary.families.inAnalysis],
                ['Produtos ativos', summary.stock.activeProducts],
                ['Produtos inativos', summary.stock.inactiveProducts],
                ['Produtos com estoque baixo', summary.stock.lowStockProducts],
                ['Distribuições confirmadas', summary.distributions.confirmed],
                ['Distribuições canceladas', summary.distributions.cancelled],
                ['Famílias atendidas', summary.distributions.distinctFamiliesServed],
                ['Itens distribuídos', summary.distributions.itemsDistributed],
                ...Object.entries(summary.families.byStatus)
                  .filter(([status]) => status !== 'EM_ANALISE')
                  .map(([status, value]) => [familyStatusLabels[status] ?? status, value] as [string, number]),
                ...Object.entries({
                  ENTRADA_DOACAO: 'Entradas por doação',
                  ENTRADA_COMPRA: 'Entradas por compra',
                  AJUSTE: 'Ajustes de estoque',
                  SAIDA_DISTRIBUICAO: 'Saídas para distribuição',
                  PERDA: 'Perdas de estoque',
                  DEVOLUCAO: 'Devoluções',
                }).map(([type, label]) => [`${label} no período`, summary.stock.movementsByType[type] ?? 0] as [string, number]),
              ].map(([label, value]) => (
                <Grid item xs={12} sm={6} lg={3} key={label}>
                  <Card sx={{ height: '100%' }}>
                    <CardContent>
                      <Typography variant="body2" color="text.secondary" gutterBottom>{label}</Typography>
                      <Typography variant="h4" sx={{ fontWeight: 700, color: 'primary.main' }}>{value}</Typography>
                    </CardContent>
                  </Card>
                </Grid>
              ))}
              {summary.donations && [
                ['Doações criadas', summary.donations.created],
                ['Aguardando pagamento', summary.donations.awaitingPayment],
                ['Confirmadas manualmente', summary.donations.manuallyConfirmed],
                ['Canceladas', summary.donations.cancelled],
                ['Expiradas', summary.donations.expired],
                ['Doações de produtos', summary.donations.productDonations],
                ['Entradas de produtos por doação', summary.donations.donatedProductEntries],
              ].map(([label, value]) => (
                <Grid item xs={12} sm={6} lg={3} key={label}>
                  <Card sx={{ height: '100%' }}>
                    <CardContent>
                      <Typography variant="body2" color="text.secondary" gutterBottom>{label}</Typography>
                      <Typography variant="h4" sx={{ fontWeight: 700, color: 'primary.main' }}>{value}</Typography>
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>

            {summary.families.total === 0 && summary.stock.activeProducts === 0 && summary.stock.inactiveProducts === 0
              && Object.values(summary.stock.movementsByType).every((count) => count === 0)
              && (!summary.donations || summary.donations.created === 0)
              && summary.distributions.confirmed === 0 && summary.distributions.cancelled === 0
              && <Alert severity="info" sx={{ mt: 2 }}>Ainda não há dados suficientes para os indicadores neste sistema.</Alert>}
            {summary.donations && <Paper sx={{ mt: 2, p: 2 }}>
              <Typography variant="h6">Recebimentos confirmados no período</Typography>
              <Typography variant="h4" sx={{ color: 'primary.main', fontWeight: 700 }}>
                {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(summary.donations.confirmedFinancialAmount)}
              </Typography>
              <Typography variant="body2" color="text.secondary">Considera apenas doações financeiras pagas ou confirmadas manualmente.</Typography>
            </Paper>}

            {!summary.priorityConfigured && <Alert severity="info" sx={{ mt: 2 }}>
              Prioridade social ainda não configurada. Nenhum ranking ou pontuação é exibido.
            </Alert>}

            <Grid container spacing={2} sx={{ mt: 1 }}>
              <Grid item xs={12} lg={6}>
                <Paper sx={{ p: 2, height: '100%' }}>
                  <Typography variant="h6" gutterBottom>Alertas operacionais</Typography>
                  {summary.alerts.length === 0
                    ? <Typography color="text.secondary">Nenhum alerta operacional no momento.</Typography>
                    : <Stack spacing={1}>{summary.alerts.map((alert) =>
                      <Alert key={alert.code} severity="warning" action={alert.actionUrl
                        ? <Button color="inherit" size="small" onClick={() => navigate(alert.actionUrl!)}>{alert.actionLabel}</Button>
                        : undefined}>
                        <strong>{alert.source}:</strong> {alert.message}
                      </Alert>
                    )}</Stack>}
                  {summary.stock.lowStock.map((product) =>
                    <Typography variant="body2" key={product.productId} sx={{ mt: 1 }}>
                      {product.name}: {product.quantity} {product.unit} (mínimo: {product.minimum} {product.unit})
                    </Typography>
                  )}
                </Paper>
              </Grid>
              <Grid item xs={12} lg={6}>
                <Paper sx={{ p: 2, height: '100%' }}>
                  <Typography variant="h6" gutterBottom>Produtos mais distribuídos</Typography>
                  {summary.distributions.topProducts.length === 0
                    ? <Typography color="text.secondary">Ainda não há distribuições confirmadas neste período.</Typography>
                    : summary.distributions.topProducts.map((product) =>
                      <Typography variant="body2" key={product.productId} sx={{ py: 0.5 }}>
                        {product.productName}: {product.quantity} {product.unit}
                      </Typography>
                    )}
                </Paper>
              </Grid>
              <Grid item xs={12} lg={6}>
                <Paper sx={{ p: 2, height: '100%' }}>
                  <Typography variant="h6" gutterBottom>Últimas movimentações de estoque</Typography>
                  {summary.recentStockMovements.length === 0
                    ? <Typography color="text.secondary">Ainda não há movimentações de estoque.</Typography>
                    : summary.recentStockMovements.map((movement) =>
                      <Typography variant="body2" key={movement.id} sx={{ py: 0.5 }}>
                        {movement.productName} — {movement.type.replace(/_/g, ' ')}: {movement.quantity} {movement.unit}
                        {' · '}{formatDateTime(movement.occurredAt)}
                      </Typography>
                    )}
                </Paper>
              </Grid>
              <Grid item xs={12} lg={6}>
                <Paper sx={{ p: 2, height: '100%' }}>
                  <Typography variant="h6" gutterBottom>Distribuições recentes</Typography>
                  {summary.recentDistributions.length === 0
                    ? <Typography color="text.secondary">Ainda não há distribuições registradas.</Typography>
                    : summary.recentDistributions.map((distribution) =>
                      <Typography variant="body2" key={distribution.id} sx={{ py: 0.5 }}>
                        Atendimento #{distribution.id}: {distribution.itemCount} item(ns)
                        {' · '}{formatDateTime(distribution.occurredAt)}
                      </Typography>
                    )}
                </Paper>
              </Grid>
            </Grid>
          </>}
        </Container>
      </Box>
    </DashboardContainer>
  )
}

export default DashboardPage

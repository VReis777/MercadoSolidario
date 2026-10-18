import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Alert,
  AppBar,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Divider,
  Drawer,
  Grid,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
  useMediaQuery,
} from '@mui/material'
import MenuIcon from '@mui/icons-material/Menu'
import CloseIcon from '@mui/icons-material/Close'
import PeopleIcon from '@mui/icons-material/People'
import InventoryIcon from '@mui/icons-material/Inventory'
import VolunteerActivismIcon from '@mui/icons-material/VolunteerActivism'
import AssessmentIcon from '@mui/icons-material/Assessment'
import PreviewBanner from './PreviewBanner'
import { PREVIEW_FAMILIES, PREVIEW_SUMMARY } from './previewData'
import { maskCpf } from '../utils/cpf'

const drawerWidth = 280
type DemoState = 'list' | 'loading' | 'empty' | 'error'

const statusColor: Record<string, string> = {
  CADASTRADA: '#2196F3',
  EM_ANALISE: '#FF9800',
  APROVADA: '#4CAF50',
  ATENDIDA: '#00BCD4',
}

function PreviewAdminPage() {
  const navigate = useNavigate()
  const isMobile = useMediaQuery('(max-width:1023px)')
  const [open, setOpen] = useState(false)
  const [demoState, setDemoState] = useState<DemoState>('list')

  const drawer = (
    <Box>
      <Toolbar sx={{ justifyContent: 'space-between' }}>
        <Typography fontWeight={800} color="primary">MERCADO SOLIDÁRIO</Typography>
        {isMobile && (
          <IconButton onClick={() => setOpen(false)} aria-label="Fechar menu">
            <CloseIcon />
          </IconButton>
        )}
      </Toolbar>
      <Divider />
      <List>
        <ListItemButton selected>
          <ListItemIcon><PeopleIcon /></ListItemIcon>
          <ListItemText primary="Famílias" secondary="Demonstração" />
        </ListItemButton>
        <ListItemButton disabled>
          <ListItemIcon><InventoryIcon /></ListItemIcon>
          <ListItemText primary="Estoque" secondary="Em breve" />
        </ListItemButton>
        <ListItemButton disabled>
          <ListItemIcon><VolunteerActivismIcon /></ListItemIcon>
          <ListItemText primary="Doações" secondary="Próxima fase" />
        </ListItemButton>
        <ListItemButton disabled>
          <ListItemIcon><AssessmentIcon /></ListItemIcon>
          <ListItemText primary="Relatórios" secondary="Em breve" />
        </ListItemButton>
      </List>
    </Box>
  )

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default', overflowX: 'hidden' }}>
      <PreviewBanner />
      <AppBar position="static" elevation={0} sx={{ bgcolor: 'primary.main' }}>
        <Toolbar>
          <IconButton color="inherit" edge="start" onClick={() => setOpen(true)} sx={{ mr: 2, display: { lg: 'none' } }} aria-label="Abrir menu">
            <MenuIcon />
          </IconButton>
          <Typography sx={{ flexGrow: 1, fontWeight: 700 }}>Painel de demonstração</Typography>
          <Chip label="Administrador de demonstração" sx={{ bgcolor: 'rgba(255,255,255,0.2)', color: 'white', maxWidth: { xs: 160, sm: 'none' } }} />
        </Toolbar>
      </AppBar>

      <Drawer
        variant={isMobile ? 'temporary' : 'permanent'}
        open={isMobile ? open : true}
        onClose={() => setOpen(false)}
        sx={{
          '& .MuiDrawer-paper': {
            width: drawerWidth,
            top: { xs: 0, lg: 48 },
            height: { lg: 'calc(100% - 48px)' },
          },
        }}
      >
        {drawer}
      </Drawer>

      <Box sx={{ ml: { lg: `${drawerWidth}px` }, p: { xs: 2, md: 3 } }}>
        <Button onClick={() => navigate('/preview')} sx={{ mb: 2 }}>
          Voltar à apresentação pública
        </Button>
        <Typography variant="h4" color="primary" gutterBottom>
          Famílias
        </Typography>
        <Typography color="text.secondary" sx={{ mb: 3 }}>
          Valores e nomes abaixo são fictícios e não vêm do banco de dados.
        </Typography>

        <Grid container spacing={2} sx={{ mb: 3 }}>
          {[
            { label: 'Famílias cadastradas', value: PREVIEW_SUMMARY.cadastradas, color: '#183B56' },
            { label: 'Em análise', value: PREVIEW_SUMMARY.emAnalise, color: '#FF9800' },
            { label: 'Aprovadas', value: PREVIEW_SUMMARY.aprovadas, color: '#4CAF50' },
            { label: 'Atendidas', value: PREVIEW_SUMMARY.atendidas, color: '#247BA0' },
          ].map((card) => (
            <Grid item xs={12} sm={6} md={3} key={card.label}>
              <Card>
                <CardContent>
                  <Typography color="text.secondary">{card.label}</Typography>
                  <Typography variant="h3" sx={{ color: card.color, fontWeight: 800 }}>{card.value}</Typography>
                  <Typography variant="caption">Indicador fictício</Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>

        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 2 }}>
          <Button variant={demoState === 'list' ? 'contained' : 'outlined'} onClick={() => setDemoState('list')}>Lista</Button>
          <Button variant={demoState === 'loading' ? 'contained' : 'outlined'} onClick={() => setDemoState('loading')}>Carregando</Button>
          <Button variant={demoState === 'empty' ? 'contained' : 'outlined'} onClick={() => setDemoState('empty')}>Lista vazia</Button>
          <Button variant={demoState === 'error' ? 'contained' : 'outlined'} onClick={() => setDemoState('error')}>Erro</Button>
        </Box>

        {demoState === 'loading' && (
          <Box sx={{ textAlign: 'center', py: 6 }}><CircularProgress /></Box>
        )}
        {demoState === 'empty' && (
          <Alert severity="info">Nenhuma família fictícia encontrada neste estado de demonstração.</Alert>
        )}
        {demoState === 'error' && (
          <Alert severity="error">Não foi possível carregar os dados de demonstração. Este erro é simulado e não consulta o servidor.</Alert>
        )}
        {demoState === 'list' && (
          <Grid container spacing={2}>
            {PREVIEW_FAMILIES.map((family) => (
              <Grid item xs={12} md={6} key={family.id}>
                <Card>
                  <CardContent>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 1, mb: 1 }}>
                      <Typography fontWeight={700}>{family.responsibleName}</Typography>
                      <Chip size="small" label={family.status.replace('_', ' ')} sx={{ bgcolor: statusColor[family.status], color: 'white' }} />
                    </Box>
                    <Typography variant="body2">CPF demonstração: {maskCpf(family.cpf)}</Typography>
                    <Typography variant="body2">{family.householdSize} pessoas · renda fictícia R$ {family.totalIncome.toFixed(0)}</Typography>
                    <Button sx={{ mt: 2 }} variant="outlined" onClick={() => navigate(`/preview/admin/familias/${family.id}`)}>
                      Ver detalhe
                    </Button>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        )}
      </Box>
    </Box>
  )
}

export default PreviewAdminPage

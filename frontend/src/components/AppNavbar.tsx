import {
  AppBar,
  Button,
  Chip,
  IconButton,
  Stack,
  Toolbar,
  Tooltip,
  Typography,
} from '@mui/material'
import LoginIcon from '@mui/icons-material/Login'
import LogoutIcon from '@mui/icons-material/Logout'
import PersonOutlineIcon from '@mui/icons-material/PersonOutline'
import { Link as RouterLink, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import ThemeToggle from './ThemeToggle'

function AppNavbar() {
  const { user, isAuthenticated, isLoading, logout } = useAuth()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const isPreview = pathname.startsWith('/preview')
  const homePath = isPreview ? '/preview' : '/'
  const requestHelpPath = isPreview ? '/preview/request-help' : '/solicitar-atendimento'

  const handleLogout = async () => {
    await logout()
    navigate('/')
  }

  return (
    <AppBar
      component="nav"
      position="sticky"
      elevation={0}
      sx={{
        bgcolor: 'background.paper',
        color: 'text.primary',
        borderBottom: 1,
        borderColor: 'divider',
      }}
    >
      <Toolbar
        sx={{
          width: '100%',
          maxWidth: 1280,
          mx: 'auto',
          flexWrap: 'wrap',
          columnGap: { xs: 0.5, sm: 2 },
          rowGap: 0.5,
        }}
      >
        <Button
          component={RouterLink}
          to={homePath}
          color="inherit"
          sx={{ px: 0, mr: 'auto', minWidth: 0, textAlign: 'left' }}
          aria-label="Mercado Solidário — página inicial"
        >
          <Typography
            component="span"
            sx={{
              fontSize: { xs: '0.95rem', sm: '1.1rem' },
              fontWeight: 800,
              color: 'primary.main',
              whiteSpace: 'nowrap',
            }}
          >
            MERCADO SOLIDÁRIO
          </Typography>
        </Button>

        <Stack direction="row" spacing={{ xs: 0, sm: 1 }} alignItems="center">
          <Button component={RouterLink} to={homePath} color="inherit" size="small">
            Início
          </Button>
          <Button
            component={RouterLink}
            to={requestHelpPath}
            color="inherit"
            size="small"
            sx={{ display: { xs: 'none', sm: 'inline-flex' } }}
          >
            Solicitar atendimento
          </Button>
        </Stack>

        <Stack direction="row" spacing={{ xs: 0.25, sm: 1 }} alignItems="center">
          <Chip
            icon={<PersonOutlineIcon />}
            label={isLoading ? 'Verificando acesso' : user?.fullName ?? 'Visitante'}
            size="small"
            variant="outlined"
            aria-label={isLoading
              ? 'Verificando quem está acessando'
              : user
                ? `Acessando como ${user.fullName}`
                : 'Acessando como visitante'}
            sx={{
              maxWidth: { xs: 112, sm: 220 },
              '& .MuiChip-label': { overflow: 'hidden', textOverflow: 'ellipsis' },
            }}
          />
          <ThemeToggle />
          {isAuthenticated ? (
            <>
              <Button
                component={RouterLink}
                to="/painel"
                size="small"
                sx={{ display: { xs: 'none', sm: 'inline-flex' } }}
              >
                Painel
              </Button>
              <Tooltip title="Sair">
                <IconButton onClick={handleLogout} color="inherit" aria-label="Sair da conta">
                  <LogoutIcon />
                </IconButton>
              </Tooltip>
            </>
          ) : (
            <Button
              component={RouterLink}
              to="/entrar"
              aria-label="Entrar"
              variant="contained"
              size="small"
              startIcon={<LoginIcon />}
              sx={{ minWidth: 96, px: 1.5 }}
            >
              Entrar
            </Button>
          )}
        </Stack>
      </Toolbar>
    </AppBar>
  )
}

export default AppNavbar

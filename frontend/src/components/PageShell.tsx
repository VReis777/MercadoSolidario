import { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppBar, Box, Container, IconButton, Toolbar, Tooltip, Typography } from '@mui/material'
import LogoutIcon from '@mui/icons-material/Logout'
import HomeIcon from '@mui/icons-material/Home'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import PublicIcon from '@mui/icons-material/Public'
import { useAuth } from '../auth/AuthContext'
import ThemeToggle from './ThemeToggle'

interface Props {
  title: string
  children: ReactNode
}

/** Estrutura comum das páginas internas: barra superior, volta ao painel e sair. */
function PageShell({ title, children }: Props) {
  const navigate = useNavigate()
  const { user, logout } = useAuth()

  const handleLogout = async () => {
    await logout()
    navigate('/entrar')
  }

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default', pb: 6 }}>
      <AppBar position="static" elevation={0} color="primary">
        <Toolbar sx={{ gap: 1, flexWrap: 'wrap' }}>
          <Tooltip title="Voltar à página anterior">
            <IconButton color="inherit" onClick={() => navigate(-1)} aria-label="Voltar à página anterior">
              <ArrowBackIcon />
            </IconButton>
          </Tooltip>
          <Typography variant="h6" component="h1" sx={{ flexGrow: 1 }}>
            {title}
          </Typography>
          <Typography variant="body2" sx={{ display: { xs: 'none', md: 'block' } }}>
            {user?.fullName}
          </Typography>
          <ThemeToggle />
          <Tooltip title="Página pública">
            <IconButton color="inherit" onClick={() => navigate('/')} aria-label="Ir para a página pública">
              <PublicIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="Painel de gestão">
            <IconButton color="inherit" onClick={() => navigate('/painel')} aria-label="Voltar ao painel">
              <HomeIcon />
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
        {children}
      </Container>
    </Box>
  )
}

export default PageShell

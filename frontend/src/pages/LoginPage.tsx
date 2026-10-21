import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Container, Box, Typography, TextField, Button, Paper, Alert, CircularProgress, IconButton, InputAdornment } from '@mui/material'
import { Visibility, VisibilityOff } from '@mui/icons-material'
import { styled } from '@mui/material/styles'
import { useAuth } from '../auth/AuthContext'
import { authService } from '../services/authService'

const LoginContainer = styled(Box)(({ theme }) => ({
  minHeight: 'calc(100vh - 64px)',
  '@media (max-width: 600px)': {
    minHeight: 'calc(100vh - 112px)',
  },
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  background: 'linear-gradient(135deg, #183B56 0%, #247BA0 100%)',
  padding: theme.spacing(2),
}))

function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [sessionNotice] = useState(() => authService.consumeSessionNotice())
  const [firstAccess, setFirstAccess] = useState<{ message: string; emailSent: boolean } | null>(null)
  const [loading, setLoading] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setFirstAccess(null)
    setLoading(true)

    try {
      const response = await login({ username, password })
      if (response.passwordChangeRequired) {
        setFirstAccess({ message: response.message ?? 'É necessário trocar a senha temporária.', emailSent: !!response.emailSent })
        setPassword('')
        return
      }
      navigate('/painel')
    } catch (err: any) {
      setError(err.response?.status === 401
        ? 'E-mail ou senha incorretos.'
        : err.response?.data?.message || 'Erro ao fazer login. Verifique suas credenciais.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <LoginContainer>
      <Container maxWidth="sm">
        <Paper sx={{ p: 4, borderRadius: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', mb: 2 }}>
            <Typography variant="h4" component="h1" sx={{ fontWeight: 700, color: 'primary.main' }}>
              MERCADO SOLIDÁRIO
            </Typography>
          </Box>
          <Typography variant="h6" gutterBottom align="center" sx={{ mb: 4, color: 'text.secondary' }}>
            Acesso Administrativo
          </Typography>

          {sessionNotice && (
            <Alert severity="info" sx={{ mb: 3 }}>
              {sessionNotice}
            </Alert>
          )}
          {error && (
            <Alert severity="error" sx={{ mb: 3 }}>
              {error}
            </Alert>
          )}
          {firstAccess && (
            <Alert severity={firstAccess.emailSent ? 'info' : 'warning'} sx={{ mb: 3 }}>
              {firstAccess.message}
            </Alert>
          )}

          <Box component="form" onSubmit={handleSubmit} sx={{ mt: 2 }}>
            <TextField
              fullWidth
              label="E-mail ou usuário"
              margin="normal"
              variant="outlined"
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              disabled={loading}
            />
            <TextField
              fullWidth
              label="Senha"
              type={showPassword ? 'text' : 'password'}
              margin="normal"
              variant="outlined"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={loading}
              InputProps={{
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      onClick={() => setShowPassword((v) => !v)}
                      edge="end"
                      aria-label={showPassword ? 'Ocultar senha' : 'Mostrar senha'}
                    >
                      {showPassword ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />
            <Button
              fullWidth
              variant="contained"
              size="large"
              type="submit"
              sx={{ mt: 3, py: 1.5 }}
              disabled={loading}
            >
              {loading ? <CircularProgress size={24} color="inherit" /> : 'Entrar'}
            </Button>
          </Box>
        </Paper>
      </Container>
    </LoginContainer>
  )
}

export default LoginPage

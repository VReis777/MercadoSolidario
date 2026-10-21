import { FormEvent, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Container,
  IconButton,
  InputAdornment,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { Visibility, VisibilityOff } from '@mui/icons-material';
import { Link, useSearchParams } from 'react-router-dom';
import { styled } from '@mui/material/styles';
import { userAccountService } from '../services/userAccountService';
import { getApiErrorMessage } from '../utils/apiError';

const SetPasswordContainer = styled(Box)(({ theme }) => ({
  minHeight: '100vh',
  display: 'grid',
  placeItems: 'center',
  padding: theme.spacing(2),
  background: 'linear-gradient(135deg, #183B56 0%, #247BA0 100%)',
}))

function SetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') ?? '';
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [completed, setCompleted] = useState(false);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');
    if (!token) {
      setError('O link está incompleto. Solicite um novo convite à administração.');
      return;
    }
    if (password.length < 8) {
      setError('A senha precisa ter pelo menos 8 caracteres.');
      return;
    }
    if (password !== confirmPassword) {
      setError('As senhas não coincidem.');
      return;
    }
    setLoading(true);
    try {
      await userAccountService.setPassword(token, password);
      setCompleted(true);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Não foi possível definir a senha. Entre de novo com a senha temporária para receber outro link.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <SetPasswordContainer>
      <Container maxWidth="sm">
        <Paper sx={{ p: { xs: 3, sm: 4 }, borderRadius: 2 }}>
          <Stack spacing={3}>
            <Box>
              <Typography component="h1" variant="h4" align="center" sx={{ color: 'primary.main', fontWeight: 700 }}>
                MERCADO SOLIDÁRIO
              </Typography>
              <Typography variant="h6" align="center" color="text.secondary" sx={{ mt: 1 }}>
                Definir senha
              </Typography>
            </Box>

            {completed ? (
              <>
                <Alert severity="success">Senha criada. Agora entre no sistema com o seu e-mail e a nova senha.</Alert>
                <Button component={Link} to="/entrar" variant="contained" fullWidth>
                  Ir para o login
                </Button>
              </>
            ) : (
              <>
                <Typography color="text.secondary">
                  Escolha uma senha com pelo menos 8 caracteres, diferente da senha temporária. Pode ser simples.
                  O link recebido por e-mail só pode ser usado uma vez; se expirar, entre de novo com a senha temporária para receber outro.
                </Typography>
                {error && <Alert severity="error">{error}</Alert>}
                <Box component="form" onSubmit={submit}>
                  <Stack spacing={2}>
                    <TextField
                      required
                      fullWidth
                      type={showPassword ? 'text' : 'password'}
                      label="Nova senha"
                      autoComplete="new-password"
                      value={password}
                      onChange={(event) => setPassword(event.target.value)}
                      inputProps={{ minLength: 8, maxLength: 128 }}
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
                    <TextField
                      required
                      fullWidth
                      type={showPassword ? 'text' : 'password'}
                      label="Confirmar nova senha"
                      autoComplete="new-password"
                      value={confirmPassword}
                      onChange={(event) => setConfirmPassword(event.target.value)}
                      inputProps={{ minLength: 8, maxLength: 128 }}
                      disabled={loading}
                    />
                    <Button type="submit" variant="contained" size="large" disabled={loading}>
                      {loading ? <CircularProgress size={24} color="inherit" /> : 'Salvar senha'}
                    </Button>
                  </Stack>
                </Box>
              </>
            )}
          </Stack>
        </Paper>
      </Container>
    </SetPasswordContainer>
  );
}

export default SetPasswordPage;

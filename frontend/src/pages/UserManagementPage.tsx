import { FormEvent, useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  IconButton,
  InputLabel,
  List,
  ListItem,
  ListItemText,
  MenuItem,
  Select,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import AddTaskIcon from '@mui/icons-material/AddTask';
import CheckIcon from '@mui/icons-material/Check';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { userAccountService, type AssignableRole } from '../services/userAccountService';
import taskService, { type AssignableUser, type StaffTask } from '../services/taskService';
import { getApiErrorMessage } from '../utils/apiError';
import ThemeToggle from '../components/ThemeToggle';

const roleLabel: Record<string, string> = {
  ADMIN: 'Administração',
  RH_TESOURARIA: 'RH / Tesouraria',
  DIACONO: 'Diácono',
};

const formatRole = (name: string) => roleLabel[name.replace('ROLE_', '')] ?? name;

function UserManagementPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const canAssign = Boolean(user?.roles.some((r) => r === 'ROLE_ADMIN' || r === 'ROLE_RH_TESOURARIA'));
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [role, setRole] = useState<AssignableRole>('RH_TESOURARIA');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [people, setPeople] = useState<AssignableUser[]>([]);
  const [tasks, setTasks] = useState<StaffTask[]>([]);
  const [listsLoaded, setListsLoaded] = useState(false);
  const [taskDialog, setTaskDialog] = useState<AssignableUser | null>(null);
  const [taskTitle, setTaskTitle] = useState('');
  const [taskDescription, setTaskDescription] = useState('');

  const loadLists = useCallback(async () => {
    try {
      const taskResult = await taskService.listMine();
      setTasks(taskResult);
      if (canAssign) {
        setPeople(await taskService.listAssignableUsers());
      }
    } catch {
      setPeople([]);
    } finally {
      setListsLoaded(true);
    }
  }, [canAssign]);

  useEffect(() => {
    void loadLists();
  }, [loadLists]);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');
    try {
      const invited = await userAccountService.invite({ fullName: fullName.trim(), email: email.trim(), role });
      setSuccess(`Perfil criado para ${invited.email} com a senha temporária padrão. No primeiro login, a pessoa receberá neste e-mail um link para criar a própria senha.`);
      setFullName('');
      setEmail('');
      setRole('RH_TESOURARIA');
      await loadLists();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Não foi possível criar o perfil.'));
    } finally {
      setLoading(false);
    }
  };

  const assignTask = async () => {
    if (!taskDialog) return;
    setLoading(true);
    setError('');
    try {
      const created = await taskService.create({
        assigneeId: taskDialog.id,
        title: taskTitle.trim(),
        description: taskDescription.trim() || undefined,
      });
      setSuccess(created.emailSent
        ? `Tarefa atribuída a ${taskDialog.fullName}. E-mail enviado com você em cópia.`
        : `Tarefa atribuída a ${taskDialog.fullName}, mas o e-mail não pôde ser enviado (SMTP não configurado).`);
      setTaskDialog(null);
      setTaskTitle('');
      setTaskDescription('');
      await loadLists();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Não foi possível atribuir a tarefa.'));
    } finally {
      setLoading(false);
    }
  };

  const completeTask = async (task: StaffTask) => {
    setLoading(true);
    setError('');
    try {
      await taskService.complete(task.id);
      await loadLists();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Não foi possível concluir a tarefa.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="md" sx={{ py: 4 }}>
      <Stack spacing={3}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/modulos')}>
            Voltar ao painel
          </Button>
          <ThemeToggle />
        </Box>
        <Box>
          <Typography component="h1" variant="h4" sx={{ color: 'primary.main', fontWeight: 700 }}>
            Cadastrar pessoa
          </Typography>
          <Typography color="text.secondary" sx={{ mt: 1 }}>
            O perfil é criado com a senha temporária padrão. Ela não libera o sistema: no primeiro login, um link individual
            (válido por 30 minutos e de uso único) é enviado ao e-mail cadastrado para a pessoa criar a própria senha, com no mínimo 8 caracteres.
          </Typography>
        </Box>

        {error && <Alert severity="error">{error}</Alert>}
        {success && <Alert severity="success">{success}</Alert>}

        <Box component="form" onSubmit={submit}>
          <Stack spacing={2.5}>
            <TextField
              required
              fullWidth
              label="Nome completo"
              autoComplete="name"
              value={fullName}
              onChange={(event) => setFullName(event.target.value)}
              inputProps={{ maxLength: 255 }}
              disabled={loading}
            />
            <TextField
              required
              fullWidth
              type="email"
              label="E-mail"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              inputProps={{ maxLength: 100 }}
              disabled={loading}
            />
            <FormControl fullWidth required>
              <InputLabel id="user-role-label">Perfil</InputLabel>
              <Select<AssignableRole>
                labelId="user-role-label"
                label="Perfil"
                value={role}
                onChange={(event) => {
                  const selectedRole = event.target.value;
                  if (selectedRole === 'RH_TESOURARIA' || selectedRole === 'DIACONO') {
                    setRole(selectedRole);
                  }
                }}
                disabled={loading}
              >
                <MenuItem value="RH_TESOURARIA">RH / Tesouraria</MenuItem>
                <MenuItem value="DIACONO">Diácono</MenuItem>
              </Select>
            </FormControl>
            <Button type="submit" variant="contained" size="large" disabled={loading}>
              {loading ? <CircularProgress size={24} color="inherit" /> : 'Enviar convite'}
            </Button>
          </Stack>
        </Box>

        <Alert severity="info">
          Somente a conta configurada como administradora responsável pode enviar convites. O perfil Pastor ainda não
          existe no cadastro de permissões, e este formulário não permite criar outro ADMIN.
        </Alert>

        <Box>
          <Typography variant="h5" color="primary" gutterBottom>
            Equipe e tarefas
          </Typography>
          {!listsLoaded ? (
            <CircularProgress size={28} />
          ) : (
            <Card>
              <CardContent>
                {canAssign ? (
                  people.length === 0 ? (
                    <Typography color="text.secondary">Nenhuma pessoa cadastrada além de você.</Typography>
                  ) : (
                    <List dense>
                      {people.map((person) => (
                        <ListItem
                          key={person.id}
                          secondaryAction={
                            <Tooltip title="Atribuir tarefa">
                              <IconButton color="primary" onClick={() => setTaskDialog(person)} aria-label={`Atribuir tarefa a ${person.fullName}`}>
                                <AddTaskIcon />
                              </IconButton>
                            </Tooltip>
                          }
                        >
                          <ListItemText
                            primary={person.fullName}
                            secondary={`${person.email} · ${person.roles.map(formatRole).join(', ')}`}
                          />
                        </ListItem>
                      ))}
                    </List>
                  )
                ) : (
                  <Typography color="text.secondary">
                    A atribuição de tarefas é permitida aos perfis Administração e RH / Tesouraria.
                  </Typography>
                )}
              </CardContent>
            </Card>
          )}
        </Box>

        {tasks.length > 0 && (
          <Box>
            <Typography variant="h5" color="primary" gutterBottom>
              Tarefas que me envolvem
            </Typography>
            <List>
              {tasks.map((task) => (
                <Card key={task.id} sx={{ mb: 1 }}>
                  <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                    <Stack direction="row" alignItems="center" spacing={1}>
                      <Box sx={{ flexGrow: 1 }}>
                        <Typography fontWeight={600}>{task.title}</Typography>
                        <Typography variant="body2" color="text.secondary">
                          Para {task.assigneeName} · criada por {task.createdByName}
                          {task.description ? ` — ${task.description}` : ''}
                        </Typography>
                      </Box>
                      <Chip
                        size="small"
                        label={task.status === 'CONCLUIDA' ? 'Concluída' : 'Aberta'}
                        color={task.status === 'CONCLUIDA' ? 'success' : 'warning'}
                      />
                      {task.status === 'ABERTA' && (
                        <Tooltip title="Marcar como concluída">
                          <IconButton color="success" onClick={() => void completeTask(task)} disabled={loading} aria-label="Concluir tarefa">
                            <CheckIcon />
                          </IconButton>
                        </Tooltip>
                      )}
                    </Stack>
                  </CardContent>
                </Card>
              ))}
            </List>
          </Box>
        )}
      </Stack>

      <Dialog open={Boolean(taskDialog)} onClose={() => !loading && setTaskDialog(null)} fullWidth maxWidth="sm">
        <DialogTitle>Atribuir tarefa a {taskDialog?.fullName}</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            A pessoa recebe um e-mail com a tarefa e você fica em cópia.
          </Typography>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              required
              fullWidth
              label="Tarefa"
              value={taskTitle}
              onChange={(event) => setTaskTitle(event.target.value)}
              inputProps={{ maxLength: 200 }}
            />
            <TextField
              fullWidth
              multiline
              minRows={3}
              label="Detalhes (opcional)"
              value={taskDescription}
              onChange={(event) => setTaskDescription(event.target.value)}
              inputProps={{ maxLength: 2000 }}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setTaskDialog(null)} disabled={loading}>Voltar</Button>
          <Button variant="contained" onClick={() => void assignTask()} disabled={loading || !taskTitle.trim()}>
            Atribuir e enviar e-mail
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
}

export default UserManagementPage;

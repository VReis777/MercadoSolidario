import { FormEvent, useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Alert, Box, Button, Card, CardActionArea, CardContent, Chip, CircularProgress, Grid,
  Paper, Stack, TextField, Typography,
} from '@mui/material'
import PeopleIcon from '@mui/icons-material/People'
import PriorityHighIcon from '@mui/icons-material/PriorityHigh'
import PixIcon from '@mui/icons-material/Pix'
import AddIcon from '@mui/icons-material/Add'
import PageShell from '../components/PageShell'
import api from '../services/api'
import { WelcomeSlot } from '../services/serviceRequestService'
import { getApiErrorMessage } from '../utils/apiError'

const items = [
  {
    title: 'Usuários',
    description: 'Cadastrar perfis, redefinir acessos e acompanhar quem pode entrar no sistema.',
    path: '/admin/usuarios',
    icon: <PeopleIcon fontSize="large" color="primary" />,
  },
  {
    title: 'Regras de prioridade',
    description: 'Configurar os critérios que calculam a prioridade das famílias.',
    path: '/prioridades',
    icon: <PriorityHighIcon fontSize="large" color="primary" />,
  },
  {
    title: 'Chave PIX das doações',
    description: 'Configurar a chave PIX exibida na página pública de doações (na tela de Doações).',
    path: '/doacoes',
    icon: <PixIcon fontSize="large" color="primary" />,
  },
]

function SettingsPage() {
  const navigate = useNavigate()
  const [slots, setSlots] = useState<WelcomeSlot[]>([])
  const [slotsLoading, setSlotsLoading] = useState(true)
  const [slotsError, setSlotsError] = useState('')
  const [slotsMessage, setSlotsMessage] = useState('')
  const [newDate, setNewDate] = useState('')
  const [newNote, setNewNote] = useState('')
  const [saving, setSaving] = useState(false)
  const [whatsappUrl, setWhatsappUrl] = useState('')
  const [instagramUrl, setInstagramUrl] = useState('')
  const [siteSaving, setSiteSaving] = useState(false)
  const [siteMessage, setSiteMessage] = useState('')
  const [siteError, setSiteError] = useState('')

  const loadSlots = useCallback(async () => {
    setSlotsLoading(true)
    setSlotsError('')
    try {
      const response = await api.get<WelcomeSlot[]>('/service-requests/welcome-slots')
      setSlots(response.data)
    } catch (e) {
      setSlotsError(getApiErrorMessage(e, 'Não foi possível carregar a agenda de acolhimento.'))
    } finally {
      setSlotsLoading(false)
    }
  }, [])

  useEffect(() => { void loadSlots() }, [loadSlots])

  useEffect(() => {
    api.get<{ whatsappGroupUrl?: string; instagramUrl?: string }>('/admin/site-info')
      .then((response) => {
        setWhatsappUrl(response.data.whatsappGroupUrl ?? '')
        setInstagramUrl(response.data.instagramUrl ?? '')
      })
      .catch(() => undefined)
  }, [])

  const saveSiteInfo = async (event: FormEvent) => {
    event.preventDefault()
    setSiteSaving(true)
    setSiteError('')
    setSiteMessage('')
    try {
      await api.put('/admin/site-info', {
        whatsappGroupUrl: whatsappUrl.trim() || null,
        instagramUrl: instagramUrl.trim() || undefined,
      })
      setSiteMessage('Links públicos atualizados.')
    } catch (e) {
      setSiteError(getApiErrorMessage(e, 'Não foi possível salvar os links.'))
    } finally {
      setSiteSaving(false)
    }
  }

  const addSlot = async (event: FormEvent) => {
    event.preventDefault()
    if (!newDate || saving) return
    setSaving(true)
    setSlotsError('')
    setSlotsMessage('')
    try {
      await api.post('/service-requests/welcome-slots', {
        availableDate: newDate,
        note: newNote.trim() || undefined,
      })
      setNewDate('')
      setNewNote('')
      setSlotsMessage('Dia de acolhimento cadastrado.')
      await loadSlots()
    } catch (e) {
      setSlotsError(getApiErrorMessage(e, 'Não foi possível cadastrar o dia.'))
    } finally {
      setSaving(false)
    }
  }

  const toggleSlot = async (slot: WelcomeSlot) => {
    setSlotsError('')
    setSlotsMessage('')
    try {
      await api.patch(`/service-requests/welcome-slots/${slot.id}/active`, { active: !slot.active })
      setSlotsMessage(slot.active ? 'Dia desativado.' : 'Dia reativado.')
      await loadSlots()
    } catch (e) {
      setSlotsError(getApiErrorMessage(e, 'Não foi possível alterar o dia.'))
    }
  }

  const activeCount = slots.filter((s) => s.active).length

  return (
    <PageShell title="Configurações">
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        Áreas administrativas do sistema. Cada seção abre a página correspondente.
      </Typography>
      <Grid container spacing={2} sx={{ mb: 3 }}>
        {items.map((item) => (
          <Grid item xs={12} sm={6} md={4} key={item.path}>
            <Card>
              <CardActionArea onClick={() => navigate(item.path)}>
                <CardContent>
                  {item.icon}
                  <Typography variant="h6" sx={{ mt: 1 }}>{item.title}</Typography>
                  <Typography variant="body2" color="text.secondary">{item.description}</Typography>
                </CardContent>
              </CardActionArea>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Paper sx={{ p: 2.5, mb: 3 }}>
        <Typography variant="h6" gutterBottom>Links públicos</Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Link do grupo de WhatsApp exibido na página "Sou empresa" e o Instagram mostrado nas páginas públicas.
          Deixe o grupo em branco enquanto ele não existir — o botão ficará como "em breve".
        </Typography>
        {siteError && <Alert severity="error" sx={{ mb: 2 }}>{siteError}</Alert>}
        {siteMessage && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSiteMessage('')}>{siteMessage}</Alert>}
        <Box component="form" onSubmit={saveSiteInfo}>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} alignItems={{ sm: 'center' }}>
            <TextField
              label="Link do grupo de WhatsApp"
              size="small"
              fullWidth
              value={whatsappUrl}
              onChange={(e) => setWhatsappUrl(e.target.value)}
              inputProps={{ maxLength: 500 }}
              placeholder="https://chat.whatsapp.com/..."
            />
            <TextField
              label="Link do Instagram"
              size="small"
              fullWidth
              value={instagramUrl}
              onChange={(e) => setInstagramUrl(e.target.value)}
              inputProps={{ maxLength: 500 }}
            />
            <Button type="submit" variant="contained" disabled={siteSaving}>
              Salvar
            </Button>
          </Stack>
        </Box>
      </Paper>

      <Paper sx={{ p: 2.5 }}>
        <Typography variant="h6" gutterBottom>Agenda de acolhimento</Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Defina até 3 dias em que a equipe pode conversar pessoalmente com as famílias.
          Esses dias aparecem no formulário público de pedido de atendimento para a pessoa escolher.
        </Typography>
        {slotsError && <Alert severity="error" sx={{ mb: 2 }}>{slotsError}</Alert>}
        {slotsMessage && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSlotsMessage('')}>{slotsMessage}</Alert>}

        <Box component="form" onSubmit={addSlot} sx={{ mb: 2 }}>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} alignItems={{ sm: 'center' }}>
            <TextField
              label="Novo dia"
              type="date"
              size="small"
              required
              value={newDate}
              onChange={(e) => setNewDate(e.target.value)}
              InputLabelProps={{ shrink: true }}
              inputProps={{ min: new Date().toISOString().slice(0, 10) }}
            />
            <TextField
              label="Observação (ex.: após o culto)"
              size="small"
              value={newNote}
              onChange={(e) => setNewNote(e.target.value)}
              inputProps={{ maxLength: 255 }}
              fullWidth
            />
            <Button type="submit" variant="contained" startIcon={<AddIcon />}
              disabled={!newDate || saving || activeCount >= 3}>
              Adicionar
            </Button>
          </Stack>
          {activeCount >= 3 && (
            <Typography variant="caption" color="text.secondary">
              Limite de 3 dias ativos atingido — desative um dia para adicionar outro.
            </Typography>
          )}
        </Box>

        {slotsLoading ? (
          <Box sx={{ py: 2, textAlign: 'center' }}><CircularProgress size={28} /></Box>
        ) : slots.length === 0 ? (
          <Alert severity="info">Nenhum dia de acolhimento cadastrado. No formulário público, a opção de visita só aparece quando houver dias ativos.</Alert>
        ) : (
          <Stack spacing={1}>
            {slots.map((slot) => (
              <Stack key={slot.id} direction="row" spacing={1} alignItems="center" sx={{ flexWrap: 'wrap' }}>
                <Chip
                  color={slot.active ? 'primary' : 'default'}
                  variant={slot.active ? 'filled' : 'outlined'}
                  label={new Date(slot.availableDate + 'T12:00:00').toLocaleDateString('pt-BR', {
                    weekday: 'short', day: '2-digit', month: '2-digit', year: 'numeric',
                  })}
                />
                {slot.note && <Typography variant="body2" color="text.secondary">{slot.note}</Typography>}
                <Box sx={{ flexGrow: 1 }} />
                <Button size="small" onClick={() => void toggleSlot(slot)}>
                  {slot.active ? 'Desativar' : 'Reativar'}
                </Button>
              </Stack>
            ))}
          </Stack>
        )}
      </Paper>
    </PageShell>
  )
}

export default SettingsPage

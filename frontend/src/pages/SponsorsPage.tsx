import { FormEvent, useEffect, useState } from 'react'
import {
  Alert, Box, Button, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle,
  FormControl, FormControlLabel, Grid, InputLabel, MenuItem, Paper, Select, Stack, Switch,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField, Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import PageShell from '../components/PageShell'
import sponsorService, { Sponsor, SponsorRequest, SponsorType } from '../services/sponsorService'
import { getApiErrorMessage } from '../utils/apiError'

const typeLabels: Record<SponsorType, string> = {
  PESSOA_FISICA: 'Pessoa física',
  EMPRESA: 'Empresa',
  INSTITUICAO: 'Instituição',
  ANONIMO: 'Anônimo',
}

const emptyForm: SponsorRequest = {
  name: '', type: 'EMPRESA', companyName: '', phone: '', whatsapp: '', email: '', observations: '',
}

function SponsorsPage() {
  const [sponsors, setSponsors] = useState<Sponsor[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [appliedSearch, setAppliedSearch] = useState('')
  const [onlyActive, setOnlyActive] = useState(false)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editing, setEditing] = useState<Sponsor | null>(null)
  const [form, setForm] = useState<SponsorRequest>(emptyForm)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')
  const [message, setMessage] = useState('')

  const load = async () => {
    setLoading(true)
    setError('')
    try {
      setSponsors(await sponsorService.list({
        search: appliedSearch || undefined,
        onlyActive: onlyActive || undefined,
      }))
    } catch (e) {
      setError(getApiErrorMessage(e, 'Não foi possível carregar os patrocinadores.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [appliedSearch, onlyActive])

  const openDialog = (sponsor?: Sponsor) => {
    setEditing(sponsor ?? null)
    setForm(sponsor ? {
      name: sponsor.name, type: sponsor.type, companyName: sponsor.companyName ?? '',
      phone: sponsor.phone ?? '', whatsapp: sponsor.whatsapp ?? '', email: sponsor.email ?? '',
      observations: sponsor.observations ?? '',
    } : emptyForm)
    setFormError('')
    setDialogOpen(true)
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setFormError('')
    if (!form.name.trim()) {
      setFormError('Informe o nome do patrocinador.')
      return
    }
    setSaving(true)
    try {
      const payload: SponsorRequest = {
        ...form,
        name: form.name.trim(),
        companyName: form.companyName?.trim() || undefined,
        phone: form.phone?.trim() || undefined,
        whatsapp: form.whatsapp?.trim() || undefined,
        email: form.email?.trim() || undefined,
        observations: form.observations?.trim() || undefined,
      }
      if (editing) {
        await sponsorService.update(editing.id, payload)
        setMessage('Patrocinador atualizado.')
      } else {
        await sponsorService.create(payload)
        setMessage('Patrocinador cadastrado.')
      }
      setDialogOpen(false)
      await load()
    } catch (e) {
      setFormError(getApiErrorMessage(e, 'Não foi possível salvar.'))
    } finally {
      setSaving(false)
    }
  }

  const toggleActive = async (sponsor: Sponsor) => {
    try {
      await sponsorService.setActive(sponsor.id, !sponsor.active)
      setMessage(sponsor.active ? 'Patrocinador desativado.' : 'Patrocinador reativado.')
      await load()
    } catch (e) {
      setError(getApiErrorMessage(e, 'Não foi possível alterar a situação.'))
    }
  }

  return (
    <PageShell title="Patrocinadores e Parceiros">
      <Stack spacing={2}>
        {message && <Alert severity="success" onClose={() => setMessage('')}>{message}</Alert>}
        {error && <Alert severity="error">{error}</Alert>}

        <Paper sx={{ p: 2 }}>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems={{ md: 'center' }}>
            <Box component="form" sx={{ display: 'flex', gap: 1, flexGrow: 1 }}
              onSubmit={(e) => { e.preventDefault(); setAppliedSearch(search.trim()) }}>
              <TextField size="small" fullWidth label="Buscar por nome" value={search}
                onChange={(e) => setSearch(e.target.value)} />
              <Button type="submit" variant="outlined">Buscar</Button>
            </Box>
            <FormControlLabel
              control={<Switch checked={onlyActive} onChange={(e) => setOnlyActive(e.target.checked)} />}
              label="Somente ativos" />
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => openDialog()}>
              Novo patrocinador
            </Button>
          </Stack>
        </Paper>

        <Paper>
          {loading ? (
            <Box sx={{ textAlign: 'center', py: 6 }}><CircularProgress /></Box>
          ) : sponsors.length === 0 ? (
            <Box sx={{ p: 3 }}>
              <Alert severity="info">Nenhum patrocinador cadastrado com estes filtros.</Alert>
            </Box>
          ) : (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Nome</TableCell>
                    <TableCell>Tipo</TableCell>
                    <TableCell>Contato</TableCell>
                    <TableCell>Observações</TableCell>
                    <TableCell>Situação</TableCell>
                    <TableCell align="right">Ações</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {sponsors.map((s) => (
                    <TableRow key={s.id} hover sx={{ opacity: s.active ? 1 : 0.6 }}>
                      <TableCell>
                        {s.name}
                        {s.companyName && (
                          <Typography variant="caption" display="block" color="text.secondary">
                            {s.companyName}
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell>{typeLabels[s.type] ?? s.type}</TableCell>
                      <TableCell>
                        {s.phone && <Typography variant="body2">{s.phone}</Typography>}
                        {s.whatsapp && <Typography variant="body2">WhatsApp: {s.whatsapp}</Typography>}
                        {s.email && <Typography variant="body2">{s.email}</Typography>}
                        {!s.phone && !s.whatsapp && !s.email && '—'}
                      </TableCell>
                      <TableCell>{s.observations ?? '—'}</TableCell>
                      <TableCell>
                        <Chip size="small" color={s.active ? 'success' : 'default'}
                          label={s.active ? 'Ativo' : 'Inativo'} variant="outlined" />
                      </TableCell>
                      <TableCell align="right">
                        <Button size="small" startIcon={<EditIcon />} onClick={() => openDialog(s)}>
                          Editar
                        </Button>
                        <Button size="small" color={s.active ? 'error' : 'primary'} onClick={() => toggleActive(s)}>
                          {s.active ? 'Desativar' : 'Reativar'}
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </Paper>
      </Stack>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <Box component="form" onSubmit={submit}>
          <DialogTitle>{editing ? 'Editar patrocinador' : 'Novo patrocinador'}</DialogTitle>
          <DialogContent>
            <Stack spacing={2} sx={{ mt: 1 }}>
              <TextField required fullWidth label="Nome" value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })} inputProps={{ maxLength: 255 }} />
              <FormControl fullWidth required>
                <InputLabel>Tipo</InputLabel>
                <Select label="Tipo" value={form.type}
                  onChange={(e) => setForm({ ...form, type: e.target.value as SponsorType })}>
                  {Object.entries(typeLabels).map(([value, label]) => (
                    <MenuItem key={value} value={value}>{label}</MenuItem>
                  ))}
                </Select>
              </FormControl>
              <TextField fullWidth label="Empresa / razão social" value={form.companyName}
                onChange={(e) => setForm({ ...form, companyName: e.target.value })} inputProps={{ maxLength: 255 }} />
              <Grid container spacing={2}>
                <Grid item xs={6}>
                  <TextField fullWidth label="Telefone" value={form.phone}
                    onChange={(e) => setForm({ ...form, phone: e.target.value })} inputProps={{ maxLength: 20 }} />
                </Grid>
                <Grid item xs={6}>
                  <TextField fullWidth label="WhatsApp" value={form.whatsapp}
                    onChange={(e) => setForm({ ...form, whatsapp: e.target.value })} inputProps={{ maxLength: 20 }} />
                </Grid>
              </Grid>
              <TextField fullWidth label="E-mail" type="email" value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })} inputProps={{ maxLength: 255 }} />
              <TextField fullWidth multiline rows={3} label="Observações" value={form.observations}
                onChange={(e) => setForm({ ...form, observations: e.target.value })} />
              {formError && <Alert severity="error">{formError}</Alert>}
            </Stack>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setDialogOpen(false)} disabled={saving}>Cancelar</Button>
            <Button type="submit" variant="contained" disabled={saving}>
              {saving ? <CircularProgress size={22} color="inherit" /> : 'Salvar'}
            </Button>
          </DialogActions>
        </Box>
      </Dialog>
    </PageShell>
  )
}

export default SponsorsPage

import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Alert, Box, Button, Chip, CircularProgress, FormControl, FormControlLabel, InputLabel, MenuItem, Paper,
  Select, Stack, Switch, Table, TableBody, TableCell, TableContainer, TableHead, TablePagination, TableRow,
  TextField, Typography,
} from '@mui/material'
import UploadIcon from '@mui/icons-material/Upload'
import campaignService, { CampaignRegistration, Page } from '../../services/campaignService'
import { getApiErrorMessage } from '../../utils/apiError'
import RegistrationImportDialog from './RegistrationImportDialog'

export const familyStatusLabels: Record<string, string> = {
  CADASTRADA: 'Cadastrada',
  EM_ANALISE: 'Em análise',
  APROVADA: 'Aprovada',
  CONTEMPLADA: 'Contemplada',
  ATENDIDA: 'Atendida',
  INATIVA: 'Inativa',
  CANCELADA: 'Cancelada',
}

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

interface Props {
  campaignYear: number
  canImport?: boolean
  initialOnlyWithHistory?: boolean
  onChanged?: () => void
}

function RegistrationsTab({ campaignYear, canImport = false, initialOnlyWithHistory = false, onChanged }: Props) {
  const navigate = useNavigate()
  const [status, setStatus] = useState('')
  const [onlyWithHistory, setOnlyWithHistory] = useState(initialOnlyWithHistory)
  const [search, setSearch] = useState('')
  const [appliedSearch, setAppliedSearch] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const [data, setData] = useState<Page<CampaignRegistration> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [importOpen, setImportOpen] = useState(false)
  const [reload, setReload] = useState(0)

  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    campaignService.registrations({
      year: campaignYear, status: status || undefined, onlyWithHistory,
      search: appliedSearch || undefined, page, size,
    })
      .then((result) => { if (active) setData(result) })
      .catch((e) => { if (active) setError(getApiErrorMessage(e, 'Não foi possível carregar os cadastros.')) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [campaignYear, status, onlyWithHistory, appliedSearch, page, size, reload])

  return (
    <Stack spacing={2}>
      <Alert severity="info">
        Famílias cadastradas em {campaignYear}, conforme a data de criação do registro.
        Cadastro <strong>não</strong> significa aprovação nem contemplação: acompanhe a coluna Situação.
      </Alert>

      <Paper sx={{ p: 2 }}>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems={{ md: 'center' }}>
          <FormControl size="small" sx={{ minWidth: 170 }}>
            <InputLabel id="reg-status">Situação</InputLabel>
            <Select labelId="reg-status" label="Situação" value={status}
              onChange={(e) => { setStatus(e.target.value); setPage(0) }}>
              <MenuItem value="">Todas</MenuItem>
              {Object.entries(familyStatusLabels).map(([value, label]) => (
                <MenuItem key={value} value={value}>{label}</MenuItem>
              ))}
            </Select>
          </FormControl>
          <FormControlLabel
            control={<Switch checked={onlyWithHistory} onChange={(e) => { setOnlyWithHistory(e.target.checked); setPage(0) }} />}
            label="Só quem já foi contemplado antes"
          />
          <Box component="form" sx={{ display: 'flex', gap: 1, flexGrow: 1 }}
            onSubmit={(e) => { e.preventDefault(); setAppliedSearch(search.trim()); setPage(0) }}>
            <TextField size="small" fullWidth label="Buscar por nome ou CPF completo" value={search}
              onChange={(e) => setSearch(e.target.value)} />
            <Button type="submit" variant="outlined">Buscar</Button>
          </Box>
          {canImport && (
            <Button variant="outlined" startIcon={<UploadIcon />} onClick={() => setImportOpen(true)}>
              Importar cadastros
            </Button>
          )}
          <Button variant="contained" onClick={() => navigate('/familias')}>Gerenciar famílias</Button>
        </Stack>
      </Paper>

      {error && <Alert severity="error">{error}</Alert>}

      <Paper>
        {loading ? (
          <Box sx={{ textAlign: 'center', py: 6 }}><CircularProgress /></Box>
        ) : data && data.content.length === 0 ? (
          <Box sx={{ p: 3 }}>
            <Alert severity="info">Nenhuma família cadastrada em {campaignYear} com estes filtros.</Alert>
          </Box>
        ) : data ? (
          <>
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Responsável</TableCell>
                    <TableCell>CPF</TableCell>
                    <TableCell>Pessoas</TableCell>
                    <TableCell>Renda</TableCell>
                    <TableCell>Origem</TableCell>
                    <TableCell>Cadastro</TableCell>
                    <TableCell>Situação</TableCell>
                    <TableCell>Histórico</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {data.content.map((f) => (
                    <TableRow key={f.id} hover>
                      <TableCell>
                        {f.responsibleName}
                        {f.referrerName && (
                          <Typography variant="caption" display="block" color="text.secondary">Indicação: {f.referrerName}</Typography>
                        )}
                      </TableCell>
                      <TableCell>{f.cpfMasked ?? '—'}</TableCell>
                      <TableCell>{f.householdSize ?? '—'}</TableCell>
                      <TableCell>{f.totalIncome != null ? currency.format(f.totalIncome) : '—'}</TableCell>
                      <TableCell>{f.origin ?? '—'}</TableCell>
                      <TableCell>{new Date(f.createdAt).toLocaleDateString('pt-BR')}</TableCell>
                      <TableCell>{f.status ? familyStatusLabels[f.status] ?? f.status : '—'}</TableCell>
                      <TableCell>
                        {f.historyMatch === 'CPF' && (
                          <Chip size="small" color="error" label={`Contemplada em ${f.historyYears.join(', ')} (CPF)`} />
                        )}
                        {f.historyMatch === 'NOME' && (
                          <Chip size="small" color="warning" variant="outlined"
                            label={`Possível: ${f.historyYears.join(', ')} (nome) — conferir`} />
                        )}
                        {f.historyMatch === 'NENHUM' && (
                          <Typography variant="caption" color="text.secondary">Sem registro anterior</Typography>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
            <TablePagination component="div" count={data.totalElements} page={page} rowsPerPage={size}
              rowsPerPageOptions={[20, 50, 100]} labelRowsPerPage="Por página"
              labelDisplayedRows={({ from, to, count }) => `${from}–${to} de ${count}`}
              onPageChange={(_, p) => setPage(p)}
              onRowsPerPageChange={(e) => { setSize(Number(e.target.value)); setPage(0) }} />
          </>
        ) : null}
      </Paper>

      <RegistrationImportDialog open={importOpen} onClose={() => setImportOpen(false)}
        onImported={() => { setReload((r) => r + 1); onChanged?.() }} />
    </Stack>
  )
}

export default RegistrationsTab

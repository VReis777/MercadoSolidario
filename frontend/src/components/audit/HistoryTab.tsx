import { useEffect, useState } from 'react'
import {
  Alert, Box, Button, Chip, CircularProgress, FormControl, InputLabel, MenuItem, Paper, Select, Stack,
  Table, TableBody, TableCell, TableContainer, TableHead, TablePagination, TableRow, TextField, Typography,
} from '@mui/material'
import UploadIcon from '@mui/icons-material/Upload'
import campaignService, { HistoricalBeneficiary, Page } from '../../services/campaignService'
import { getApiErrorMessage } from '../../utils/apiError'
import HistoryImportDialog from './HistoryImportDialog'

interface Props {
  years: number[]
  origins: string[]
  campaignYear: number
  canImport: boolean
  onChanged: () => void
  initialYear?: number | ''
}

function HistoryTab({ years, origins, campaignYear, canImport, onChanged, initialYear = '' }: Props) {
  const [year, setYear] = useState<number | ''>(initialYear)
  const [origin, setOrigin] = useState('')
  const [search, setSearch] = useState('')
  const [appliedSearch, setAppliedSearch] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const [data, setData] = useState<Page<HistoricalBeneficiary> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [importOpen, setImportOpen] = useState(false)
  const [reload, setReload] = useState(0)

  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    campaignService.history({
      year: year === '' ? undefined : year, origin: origin || undefined,
      search: appliedSearch || undefined, page, size,
    })
      .then((result) => { if (active) setData(result) })
      .catch((e) => { if (active) setError(getApiErrorMessage(e, 'Não foi possível carregar o histórico.')) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [year, origin, appliedSearch, page, size, reload])

  const noHistoryAtAll = !loading && !error && years.length === 0

  return (
    <Stack spacing={2}>
      <Typography color="text.secondary">
        Famílias <strong>efetivamente contempladas</strong> em campanhas anteriores, conforme as planilhas importadas.
        Esta lista não se mistura com os cadastros de {campaignYear}.
      </Typography>

      <Paper sx={{ p: 2 }}>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems={{ md: 'center' }}>
          <FormControl size="small" sx={{ minWidth: 140 }}>
            <InputLabel id="hist-year">Ano</InputLabel>
            <Select labelId="hist-year" label="Ano" value={year}
              onChange={(e) => { setYear(e.target.value === '' ? '' : Number(e.target.value)); setPage(0) }}>
              <MenuItem value="">Todos</MenuItem>
              {years.map((y) => <MenuItem key={y} value={y}>{y}</MenuItem>)}
            </Select>
          </FormControl>
          <FormControl size="small" sx={{ minWidth: 200 }}>
            <InputLabel id="hist-origin">Origem / indicação</InputLabel>
            <Select labelId="hist-origin" label="Origem / indicação" value={origin}
              onChange={(e) => { setOrigin(e.target.value); setPage(0) }}>
              <MenuItem value="">Todas</MenuItem>
              {origins.map((o) => <MenuItem key={o} value={o}>{o}</MenuItem>)}
            </Select>
          </FormControl>
          <Box component="form" sx={{ display: 'flex', gap: 1, flexGrow: 1 }}
            onSubmit={(e) => { e.preventDefault(); setAppliedSearch(search.trim()); setPage(0) }}>
            <TextField size="small" fullWidth label="Buscar por nome ou CPF completo" value={search}
              onChange={(e) => setSearch(e.target.value)} />
            <Button type="submit" variant="outlined">Buscar</Button>
          </Box>
          {canImport && (
            <Button variant="contained" startIcon={<UploadIcon />} onClick={() => setImportOpen(true)}>
              Importar planilha
            </Button>
          )}
        </Stack>
      </Paper>

      {error && <Alert severity="error">{error}</Alert>}
      {noHistoryAtAll && (
        <Alert severity="warning">
          O histórico de campanhas anteriores ainda não foi importado. Isso <strong>não</strong> significa que ninguém foi contemplado.
          {canImport ? ' Use "Importar planilha" para carregar 2024 e 2025.' : ' Peça ao RH/Tesouraria ou ao administrador para importar as planilhas.'}
        </Alert>
      )}

      <Paper>
        {loading ? (
          <Box sx={{ textAlign: 'center', py: 6 }}><CircularProgress /></Box>
        ) : data && data.content.length === 0 && !noHistoryAtAll ? (
          <Box sx={{ p: 3 }}><Alert severity="info">Nenhum registro encontrado com estes filtros.</Alert></Box>
        ) : data && data.content.length > 0 ? (
          <>
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Ano</TableCell>
                    <TableCell>Nome</TableCell>
                    <TableCell>CPF</TableCell>
                    <TableCell>Telefone</TableCell>
                    <TableCell>Pessoas</TableCell>
                    <TableCell>Origem</TableCell>
                    <TableCell>Fonte</TableCell>
                    <TableCell>{campaignYear}</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {data.content.map((h) => (
                    <TableRow key={h.id} hover>
                      <TableCell><Chip size="small" color="primary" label={h.campaignYear} /></TableCell>
                      <TableCell>{h.name}</TableCell>
                      <TableCell>{h.cpfMasked ?? <Typography variant="caption" color="text.secondary">sem CPF</Typography>}</TableCell>
                      <TableCell>{h.phoneMasked ?? '—'}</TableCell>
                      <TableCell>{h.householdSize ?? '—'}</TableCell>
                      <TableCell>{h.origin ?? '—'}</TableCell>
                      <TableCell>
                        <Typography variant="caption" color="text.secondary">
                          {h.sourceSheet} · linha {h.sourceRow}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        {h.registeredThisCampaign
                          ? <Chip size="small" color="warning" label="Cadastrou-se de novo" />
                          : <Typography variant="caption" color="text.secondary">—</Typography>}
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

      <HistoryImportDialog open={importOpen} onClose={() => setImportOpen(false)}
        onImported={() => { setReload((r) => r + 1); onChanged() }} />
    </Stack>
  )
}

export default HistoryTab

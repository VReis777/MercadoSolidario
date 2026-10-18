import { useState } from 'react'
import {
  Alert, Box, Button, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle,
  FormControl, InputLabel, MenuItem, Select, Stack, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Typography, useMediaQuery,
} from '@mui/material'
import campaignService, {
  HistoricalImportPreview, HistoricalImportResult, ImportRowStatus,
} from '../../services/campaignService'
import { getApiErrorMessage } from '../../utils/apiError'

const statusLabel: Record<ImportRowStatus, { label: string; color: 'success' | 'default' | 'warning' | 'error' }> = {
  NOVO: { label: 'Será importado', color: 'success' },
  JA_IMPORTADO: { label: 'Já importado', color: 'default' },
  DUPLICADO_NO_ARQUIVO: { label: 'Duplicado', color: 'warning' },
  INVALIDO: { label: 'Inválido', color: 'error' },
}

interface Props {
  open: boolean
  onClose: () => void
  onImported: () => void
}

function HistoryImportDialog({ open, onClose, onImported }: Props) {
  const fullScreen = useMediaQuery('(max-width:899px)')
  const currentYear = new Date().getFullYear()
  const years = Array.from({ length: 4 }, (_, i) => currentYear - 1 - i)
  const [year, setYear] = useState(currentYear - 1)
  const [file, setFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<HistoricalImportPreview | null>(null)
  const [result, setResult] = useState<HistoricalImportResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const reset = () => {
    setFile(null); setPreview(null); setResult(null); setError('')
  }

  const close = () => {
    reset()
    onClose()
  }

  const runPreview = async () => {
    if (!file) return
    setLoading(true); setError(''); setResult(null)
    try {
      setPreview(await campaignService.previewHistoryImport(file, year))
    } catch (e) {
      setError(getApiErrorMessage(e, 'Não foi possível gerar a prévia.'))
    } finally {
      setLoading(false)
    }
  }

  const confirm = async () => {
    if (!file) return
    setLoading(true); setError('')
    try {
      setResult(await campaignService.confirmHistoryImport(file, year))
      setPreview(null)
      onImported()
    } catch (e) {
      setError(getApiErrorMessage(e, 'Não foi possível importar.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <Dialog open={open} onClose={close} fullScreen={fullScreen} maxWidth="lg" fullWidth>
      <DialogTitle>Importar contemplados de campanha anterior</DialogTitle>
      <DialogContent dividers>
        <Stack spacing={2}>
          <Alert severity="info">
            Envie a planilha do Excel (.xlsx ou .xls) com a lista de famílias <strong>efetivamente contempladas</strong> no ano escolhido.
            Primeiro é gerada uma prévia; nada é gravado até você confirmar. Registros repetidos são ignorados automaticamente.
          </Alert>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }}>
            <FormControl size="small" sx={{ minWidth: 160 }}>
              <InputLabel id="history-year">Ano da campanha</InputLabel>
              <Select labelId="history-year" label="Ano da campanha" value={year}
                onChange={(e) => { setYear(Number(e.target.value)); setPreview(null); setResult(null) }}>
                {years.map((y) => <MenuItem key={y} value={y}>{y}</MenuItem>)}
              </Select>
            </FormControl>
            <Button variant="outlined" component="label">
              {file ? 'Trocar arquivo' : 'Escolher planilha'}
              <input hidden type="file" accept=".xlsx,.xls"
                onChange={(e) => { setFile(e.target.files?.[0] ?? null); setPreview(null); setResult(null) }} />
            </Button>
            <Typography variant="body2" color="text.secondary" sx={{ wordBreak: 'break-all' }}>
              {file?.name ?? 'Nenhum arquivo selecionado'}
            </Typography>
          </Stack>

          {error && <Alert severity="error">{error}</Alert>}
          {result && (
            <Alert severity="success">
              {result.imported} registro(s) de {result.campaignYear} importado(s) de "{result.fileName}".
              Ignorados: {result.skippedDuplicates} duplicado(s), {result.skippedAlreadyImported} já existente(s), {result.skippedInvalid} inválido(s).
            </Alert>
          )}

          {preview && (
            <>
              <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                <Chip label={`${preview.totalRows} linhas lidas`} />
                <Chip color="success" label={`${preview.newRows} novos`} />
                <Chip color="warning" label={`${preview.duplicateInFile} duplicados`} />
                <Chip label={`${preview.alreadyImported} já importados`} />
                <Chip color="error" variant="outlined" label={`${preview.invalidRows} inválidos`} />
                <Chip variant="outlined" label={`${preview.withoutCpf} sem CPF`} />
                <Chip variant="outlined" color="info" label={`${preview.alsoInOtherYears} também em outro ano`} />
                <Chip variant="outlined" color="info" label={`${preview.matchingCurrentFamilies} com cadastro atual`} />
              </Box>
              {preview.sheetWarnings.map((w) => <Alert key={w} severity="warning">{w}</Alert>)}
              <TableContainer sx={{ maxHeight: 420 }}>
                <Table size="small" stickyHeader>
                  <TableHead>
                    <TableRow>
                      <TableCell>Aba / linha</TableCell>
                      <TableCell>Nome</TableCell>
                      <TableCell>CPF</TableCell>
                      <TableCell>Telefone</TableCell>
                      <TableCell>Pessoas</TableCell>
                      <TableCell>Origem</TableCell>
                      <TableCell>Situação</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {preview.rows.map((row) => (
                      <TableRow key={`${row.sheet}-${row.rowNumber}`}>
                        <TableCell sx={{ whiteSpace: 'nowrap' }}>{row.sheet} / {row.rowNumber}</TableCell>
                        <TableCell>{row.name || '—'}</TableCell>
                        <TableCell>{row.cpfMasked ?? '—'}</TableCell>
                        <TableCell>{row.phoneMasked ?? '—'}</TableCell>
                        <TableCell>{row.householdSize ?? '—'}</TableCell>
                        <TableCell>{row.origin ?? '—'}</TableCell>
                        <TableCell>
                          <Chip size="small" color={statusLabel[row.status].color} label={statusLabel[row.status].label} />
                          {row.messages.map((m) => (
                            <Typography key={m} variant="caption" display="block" color="text.secondary">{m}</Typography>
                          ))}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </>
          )}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={close}>Fechar</Button>
        {!preview && (
          <Button variant="outlined" onClick={runPreview} disabled={!file || loading}>
            {loading ? <CircularProgress size={22} /> : 'Gerar prévia'}
          </Button>
        )}
        {preview && (
          <Button variant="contained" onClick={confirm} disabled={loading || preview.newRows === 0}>
            {loading ? <CircularProgress size={22} color="inherit" /> : `Confirmar importação de ${preview.newRows} registro(s) de ${preview.campaignYear}`}
          </Button>
        )}
      </DialogActions>
    </Dialog>
  )
}

export default HistoryImportDialog

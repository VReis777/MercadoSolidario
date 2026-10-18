import { useState } from 'react'
import {
  Alert, Box, Button, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle,
  Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography, useMediaQuery,
} from '@mui/material'
import DownloadIcon from '@mui/icons-material/Download'
import campaignService, {
  RegistrationImportPreview, RegistrationImportResult, RegistrationRowStatus,
} from '../../services/campaignService'
import { getApiErrorMessage } from '../../utils/apiError'

const statusLabel: Record<RegistrationRowStatus, { label: string; color: 'success' | 'default' | 'warning' | 'error' }> = {
  NOVO: { label: 'Será cadastrada', color: 'success' },
  JA_CADASTRADO: { label: 'Já cadastrada', color: 'default' },
  DUPLICADO_NO_ARQUIVO: { label: 'Duplicada', color: 'warning' },
  INVALIDO: { label: 'Inválida', color: 'error' },
}

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

interface Props {
  open: boolean
  onClose: () => void
  onImported: () => void
}

function RegistrationImportDialog({ open, onClose, onImported }: Props) {
  const fullScreen = useMediaQuery('(max-width:899px)')
  const [file, setFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<RegistrationImportPreview | null>(null)
  const [result, setResult] = useState<RegistrationImportResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [downloading, setDownloading] = useState(false)
  const [error, setError] = useState('')

  const reset = () => {
    setFile(null); setPreview(null); setResult(null); setError('')
  }

  const close = () => {
    reset()
    onClose()
  }

  const downloadTemplate = async () => {
    setDownloading(true)
    setError('')
    try {
      await campaignService.downloadRegistrationTemplate()
    } catch (e) {
      setError(getApiErrorMessage(e, 'Não foi possível baixar a planilha modelo.'))
    } finally {
      setDownloading(false)
    }
  }

  const runPreview = async () => {
    if (!file) return
    setLoading(true); setError(''); setResult(null)
    try {
      setPreview(await campaignService.previewRegistrationImport(file))
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
      setResult(await campaignService.confirmRegistrationImport(file))
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
      <DialogTitle>Importar cadastros {preview?.campaignYear ?? new Date().getFullYear()}</DialogTitle>
      <DialogContent dividers>
        <Stack spacing={2}>
          <Alert severity="info">
            Baixe a <strong>planilha modelo</strong>, preencha uma família por linha e envie aqui.
            Campos obrigatórios: Nome, CPF, Telefone, Endereço, Cônjuge, Grupo Familiar e Renda
            (escreva 0 se não houver renda e "não tem" em Cônjuge).
            Nada é gravado até você confirmar a prévia; cadastros repetidos são ignorados.
          </Alert>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }}>
            <Button variant="outlined" startIcon={<DownloadIcon />} onClick={downloadTemplate} disabled={downloading}>
              {downloading ? 'Baixando…' : 'Baixar planilha modelo'}
            </Button>
            <Button variant="outlined" component="label">
              {file ? 'Trocar arquivo' : 'Escolher planilha preenchida'}
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
              {result.imported} família(s) cadastrada(s) de "{result.fileName}".
              Ignoradas: {result.skippedDuplicates} duplicada(s), {result.skippedAlreadyRegistered} já cadastrada(s), {result.skippedInvalid} com erro.
            </Alert>
          )}

          {preview && (
            <>
              <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                <Chip label={`${preview.totalRows} linhas lidas`} />
                <Chip color="success" label={`${preview.newRows} serão cadastradas`} />
                <Chip color="warning" label={`${preview.duplicateInFile} duplicadas`} />
                <Chip label={`${preview.alreadyRegistered} já cadastradas`} />
                <Chip color="error" variant="outlined" label={`${preview.invalidRows} com erro`} />
                <Chip variant="outlined" color="info" label={`${preview.withPreviousHistory} já contempladas antes`} />
              </Box>
              {preview.warnings.map((w) => <Alert key={w} severity="warning">{w}</Alert>)}
              <TableContainer sx={{ maxHeight: 420 }}>
                <Table size="small" stickyHeader>
                  <TableHead>
                    <TableRow>
                      <TableCell>Aba / linha</TableCell>
                      <TableCell>Nome</TableCell>
                      <TableCell>CPF</TableCell>
                      <TableCell>Telefone</TableCell>
                      <TableCell>Cônjuge</TableCell>
                      <TableCell>Pessoas</TableCell>
                      <TableCell>Renda</TableCell>
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
                        <TableCell>{row.spouseName ?? '—'}</TableCell>
                        <TableCell>{row.householdSize ?? '—'}</TableCell>
                        <TableCell>{row.totalIncome != null ? currency.format(row.totalIncome) : '—'}</TableCell>
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
            {loading ? <CircularProgress size={22} color="inherit" /> : `Confirmar cadastro de ${preview.newRows} família(s)`}
          </Button>
        )}
      </DialogActions>
    </Dialog>
  )
}

export default RegistrationImportDialog

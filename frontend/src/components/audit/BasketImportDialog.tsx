import { useState } from 'react'
import {
  Alert, Box, Button, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle,
  Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography, useMediaQuery,
} from '@mui/material'
import campaignService, { BasketImportPreview, BasketImportResult } from '../../services/campaignService'
import { getApiErrorMessage } from '../../utils/apiError'

const statusLabel = {
  NOVO: { label: 'Será criado', color: 'success' as const },
  JA_CADASTRADO: { label: 'Já cadastrado', color: 'default' as const },
  DUPLICADO_NO_ARQUIVO: { label: 'Duplicado', color: 'warning' as const },
}

interface Props {
  open: boolean
  onClose: () => void
  onImported: () => void
}

function BasketImportDialog({ open, onClose, onImported }: Props) {
  const fullScreen = useMediaQuery('(max-width:899px)')
  const [file, setFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<BasketImportPreview | null>(null)
  const [result, setResult] = useState<BasketImportResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const close = () => {
    setFile(null); setPreview(null); setResult(null); setError('')
    onClose()
  }

  const run = async (confirm: boolean) => {
    if (!file) return
    setLoading(true); setError('')
    try {
      if (confirm) {
        setResult(await campaignService.confirmBasketImport(file))
        setPreview(null)
        onImported()
      } else {
        setResult(null)
        setPreview(await campaignService.previewBasketImport(file))
      }
    } catch (e) {
      setError(getApiErrorMessage(e, 'Não foi possível processar a planilha.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <Dialog open={open} onClose={close} fullScreen={fullScreen} maxWidth="md" fullWidth>
      <DialogTitle>Importar lista de itens da cesta (COMANDA)</DialogTitle>
      <DialogContent dividers>
        <Stack spacing={2}>
          <Alert severity="info">
            Os itens da coluna A são criados com <strong>estoque 0</strong> e unidade <strong>"un"</strong> (edite depois em Estoque).
            Uma aba que comece com "ITENS" é lida como lista de marcas preferidas. Itens já cadastrados não são alterados.
          </Alert>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }}>
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
              {result.created} item(ns) criado(s). Ignorados: {result.skippedExisting} já cadastrado(s), {result.skippedDuplicates} duplicado(s).
            </Alert>
          )}
          {preview && (
            <>
              <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                <Chip label={`${preview.totalItems} itens lidos`} />
                <Chip color="success" label={`${preview.newItems} novos`} />
                <Chip label={`${preview.existingItems} já cadastrados`} />
                <Chip color="warning" label={`${preview.duplicateItems} duplicados`} />
              </Box>
              {preview.warnings.map((w) => <Alert key={w} severity="info">{w}</Alert>)}
              {preview.unmatchedBrands.length > 0 && (
                <Alert severity="warning">
                  Marcas sem item correspondente (cadastre ou ajuste o nome manualmente): {preview.unmatchedBrands.join('; ')}
                </Alert>
              )}
              <TableContainer sx={{ maxHeight: 380 }}>
                <Table size="small" stickyHeader>
                  <TableHead>
                    <TableRow>
                      <TableCell>Item</TableCell>
                      <TableCell>Marca preferida</TableCell>
                      <TableCell>Situação</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {preview.items.map((item, index) => (
                      <TableRow key={`${item.name}-${index}`}>
                        <TableCell>{item.name}</TableCell>
                        <TableCell>{item.preferredBrand ?? '—'}</TableCell>
                        <TableCell>
                          <Chip size="small" color={statusLabel[item.status].color} label={statusLabel[item.status].label} />
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
          <Button variant="outlined" onClick={() => run(false)} disabled={!file || loading}>
            {loading ? <CircularProgress size={22} /> : 'Gerar prévia'}
          </Button>
        )}
        {preview && (
          <Button variant="contained" onClick={() => run(true)} disabled={loading || preview.newItems === 0}>
            {loading ? <CircularProgress size={22} color="inherit" /> : `Criar ${preview.newItems} item(ns)`}
          </Button>
        )}
      </DialogActions>
    </Dialog>
  )
}

export default BasketImportDialog

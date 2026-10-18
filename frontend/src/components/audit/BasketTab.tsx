import { FormEvent, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Alert, Box, Button, Chip, CircularProgress, Grid, Paper, Stack, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, TextField, Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import UploadIcon from '@mui/icons-material/Upload'
import stockService, { Product } from '../../services/stockService'
import { getApiErrorMessage } from '../../utils/apiError'
import BasketImportDialog from './BasketImportDialog'

const emptyForm = { name: '', unit: 'un', category: '', minimumQuantity: '0', notes: '' }

interface Props {
  canEdit: boolean
  onChanged: () => void
}

function BasketTab({ canEdit, onChanged }: Props) {
  const navigate = useNavigate()
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [filter, setFilter] = useState('')
  const [form, setForm] = useState(emptyForm)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')
  const [success, setSuccess] = useState('')
  const [importOpen, setImportOpen] = useState(false)

  const load = async () => {
    setLoading(true)
    setError('')
    try {
      setProducts(await stockService.listProducts())
    } catch (e) {
      setError(getApiErrorMessage(e, 'Não foi possível carregar os itens.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const visible = useMemo(() => {
    const term = filter.trim().toLowerCase()
    return products
      .filter((p) => !term || p.name.toLowerCase().includes(term) || (p.category ?? '').toLowerCase().includes(term))
      .sort((a, b) => a.name.localeCompare(b.name, 'pt-BR'))
  }, [products, filter])

  const duplicateName = products.some((p) => p.name.trim().toLowerCase() === form.name.trim().toLowerCase())

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setFormError('')
    setSuccess('')
    if (!form.name.trim() || !form.unit.trim()) {
      setFormError('Informe o nome e a unidade do item.')
      return
    }
    if (duplicateName) {
      setFormError('Já existe um item com este nome.')
      return
    }
    setSaving(true)
    try {
      const created = await stockService.createProduct({
        name: form.name.trim(), unit: form.unit.trim(), category: form.category.trim() || undefined,
        minimumQuantity: Math.max(0, Number(form.minimumQuantity) || 0), notes: form.notes.trim() || undefined,
      })
      setSuccess(`Item "${created.name}" adicionado com estoque 0. Registre as entradas em Estoque.`)
      setForm(emptyForm)
      await load()
      onChanged()
    } catch (e) {
      setFormError(getApiErrorMessage(e, 'Não foi possível adicionar o item.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Stack spacing={2}>
      {canEdit && (
        <Paper sx={{ p: 2 }} component="form" onSubmit={submit}>
          <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }} flexWrap="wrap" gap={1}>
            <Typography variant="h6">Acrescentar item</Typography>
            <Button variant="outlined" startIcon={<UploadIcon />} onClick={() => setImportOpen(true)}>
              Importar COMANDA (.xlsx)
            </Button>
          </Stack>
          <Grid container spacing={2}>
            <Grid item xs={12} md={4}>
              <TextField size="small" fullWidth required label="Nome do item" value={form.name}
                error={!!form.name && duplicateName} helperText={form.name && duplicateName ? 'Já cadastrado' : ' '}
                onChange={(e) => setForm({ ...form, name: e.target.value })} inputProps={{ maxLength: 255 }} />
            </Grid>
            <Grid item xs={6} md={2}>
              <TextField size="small" fullWidth required label="Unidade" value={form.unit}
                helperText="un, kg, pct, L…" onChange={(e) => setForm({ ...form, unit: e.target.value })} inputProps={{ maxLength: 50 }} />
            </Grid>
            <Grid item xs={6} md={2}>
              <TextField size="small" fullWidth label="Estoque mínimo" type="number" value={form.minimumQuantity}
                helperText=" " onChange={(e) => setForm({ ...form, minimumQuantity: e.target.value })} inputProps={{ min: 0 }} />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField size="small" fullWidth label="Categoria" value={form.category} helperText="Ex.: Alimentos, Higiene, Limpeza"
                onChange={(e) => setForm({ ...form, category: e.target.value })} inputProps={{ maxLength: 100 }} />
            </Grid>
            <Grid item xs={12} md={9}>
              <TextField size="small" fullWidth label="Observação (marca preferida, tamanho…)" value={form.notes}
                onChange={(e) => setForm({ ...form, notes: e.target.value })} inputProps={{ maxLength: 500 }} />
            </Grid>
            <Grid item xs={12} md={3}>
              <Button type="submit" fullWidth variant="contained" startIcon={<AddIcon />} disabled={saving}>
                {saving ? <CircularProgress size={22} color="inherit" /> : 'Adicionar'}
              </Button>
            </Grid>
          </Grid>
          {formError && <Alert severity="error" sx={{ mt: 2 }}>{formError}</Alert>}
          {success && <Alert severity="success" sx={{ mt: 2 }} onClose={() => setSuccess('')}>{success}</Alert>}
        </Paper>
      )}

      <Paper sx={{ p: 2 }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }} sx={{ mb: 2 }}>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>Lista de itens ({products.length})</Typography>
          <TextField size="small" label="Filtrar" value={filter} onChange={(e) => setFilter(e.target.value)} />
          <Button onClick={() => navigate('/estoque')}>Movimentar estoque</Button>
        </Stack>
        {error && <Alert severity="error">{error}</Alert>}
        {loading ? (
          <Box sx={{ textAlign: 'center', py: 6 }}><CircularProgress /></Box>
        ) : products.length === 0 ? (
          <Alert severity="info">
            Nenhum item cadastrado. {canEdit ? 'Acrescente manualmente ou importe a planilha COMANDA.' : ''}
          </Alert>
        ) : (
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Item</TableCell>
                  <TableCell>Categoria</TableCell>
                  <TableCell align="right">Estoque</TableCell>
                  <TableCell align="right">Mínimo</TableCell>
                  <TableCell align="right">Por família</TableCell>
                  <TableCell>Observação</TableCell>
                  <TableCell>Situação</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {visible.map((p) => (
                  <TableRow key={p.id} hover sx={{ opacity: p.active ? 1 : 0.6 }}>
                    <TableCell>{p.name}</TableCell>
                    <TableCell>{p.category ?? '—'}</TableCell>
                    <TableCell align="right">{p.currentQuantity} {p.unit}</TableCell>
                    <TableCell align="right">{p.minimumQuantity}</TableCell>
                    <TableCell align="right">{p.basketQuantity != null && p.basketQuantity > 0 ? `${p.basketQuantity} ${p.unit}` : '—'}</TableCell>
                    <TableCell>{p.notes ?? '—'}</TableCell>
                    <TableCell>
                      {!p.active ? <Chip size="small" label="Inativo" />
                        : p.currentQuantity <= 0 ? <Chip size="small" color="error" variant="outlined" label="Sem estoque" />
                          : p.lowStock ? <Chip size="small" color="warning" variant="outlined" label="Estoque baixo" />
                            : <Chip size="small" color="success" variant="outlined" label="OK" />}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Paper>

      <BasketImportDialog open={importOpen} onClose={() => setImportOpen(false)}
        onImported={() => { load(); onChanged() }} />
    </Stack>
  )
}

export default BasketTab

import { FormEvent, useCallback, useEffect, useState } from 'react'
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
  Grid,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Toolbar,
  Typography,
  useMediaQuery,
} from '@mui/material'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import AddIcon from '@mui/icons-material/Add'
import InventoryIcon from '@mui/icons-material/Inventory'
import SwapVertIcon from '@mui/icons-material/SwapVert'
import EditIcon from '@mui/icons-material/Edit'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import stockService, {
  MovementRequest,
  MovementType,
  Product,
  ProductRequest,
  StockMovement,
  StockSummary,
} from '../services/stockService'
import { getApiErrorMessage } from '../utils/apiError'
import ThemeToggle from '../components/ThemeToggle'

const movementLabels: Record<MovementType, string> = {
  ENTRADA_DOACAO: 'Entrada — doação de produtos',
  ENTRADA_COMPRA: 'Entrada — compra',
  AJUSTE: 'Ajuste',
  SAIDA_DISTRIBUICAO: 'Saída — distribuição',
  PERDA: 'Perda',
  DEVOLUCAO: 'Devolução',
}

const emptyProduct: ProductRequest = { name: '', category: '', unit: '', minimumQuantity: 0 }

function StockPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const isMobileDialog = useMediaQuery('(max-width:599px)')
  const canEdit = Boolean(user?.roles.some((role) => ['ROLE_ADMIN', 'ROLE_RH_TESOURARIA'].includes(role)))
  const [summary, setSummary] = useState<StockSummary | null>(null)
  const [products, setProducts] = useState<Product[]>([])
  const [movements, setMovements] = useState<StockMovement[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [productDialog, setProductDialog] = useState(false)
  const [editingProduct, setEditingProduct] = useState<Product | null>(null)
  const [productForm, setProductForm] = useState<ProductRequest>(emptyProduct)
  const [movementDialog, setMovementDialog] = useState(false)
  const [confirmMovement, setConfirmMovement] = useState(false)
  const [movementForm, setMovementForm] = useState<MovementRequest>({
    productId: 0,
    type: 'ENTRADA_DOACAO',
    quantity: 1,
    reason: '',
  })
  const [saving, setSaving] = useState(false)
  const [movementError, setMovementError] = useState('')
  const [confirmProduct, setConfirmProduct] = useState<Product | null>(null)
  const [showProductHistory, setShowProductHistory] = useState<number | null>(null)
  const [productHistory, setProductHistory] = useState<StockMovement[]>([])

  const loadStock = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [summaryResult, productResult, movementResult] = await Promise.all([
        stockService.getSummary(),
        stockService.listProducts(),
        stockService.listMovements(),
      ])
      setSummary(summaryResult)
      setProducts(productResult)
      setMovements(movementResult)
    } catch (loadError) {
      setError(getApiErrorMessage(loadError, 'Não foi possível carregar o estoque.'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadStock()
  }, [loadStock])

  const openProductForm = (product?: Product) => {
    setEditingProduct(product || null)
    setProductForm(product
      ? { name: product.name, category: product.category || '', unit: product.unit, minimumQuantity: product.minimumQuantity, basketQuantity: product.basketQuantity }
      : emptyProduct)
    setMovementError('')
    setProductDialog(true)
  }

  const saveProduct = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSaving(true)
    setMovementError('')
    try {
      if (editingProduct) {
        await stockService.updateProduct(editingProduct.id, productForm)
        setMessage('Produto atualizado.')
      } else {
        await stockService.createProduct(productForm)
        setMessage('Produto cadastrado com saldo inicial zero. Registre uma entrada para adicionar quantidade.')
      }
      setProductDialog(false)
      await loadStock()
    } catch (saveError) {
      setMovementError(getApiErrorMessage(saveError, 'Não foi possível salvar o produto.'))
    } finally {
      setSaving(false)
    }
  }

  const openMovementForm = (product: Product, type: MovementType) => {
    setMovementForm({
      productId: product.id,
      type,
      quantity: 1,
      adjustmentDirection: type === 'AJUSTE' ? 'ENTRADA' : undefined,
      reason: '',
      observation: '',
    })
    setMovementError('')
    setMovementDialog(true)
  }

  const saveMovement = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (movementForm.type === 'AJUSTE' || movementForm.type === 'PERDA') {
      setConfirmMovement(true)
      return
    }
    await registerMovement()
  }

  const registerMovement = async () => {
    setSaving(true)
    setMovementError('')
    try {
      await stockService.registerMovement(movementForm)
      setMessage(`${movementLabels[movementForm.type]} registrada; saldo atualizado.`)
      setMovementDialog(false)
      setConfirmMovement(false)
      await loadStock()
      if (showProductHistory !== null) await openHistory(showProductHistory)
    } catch (saveError) {
      setMovementError(getApiErrorMessage(saveError, 'Não foi possível registrar a movimentação.'))
    } finally {
      setSaving(false)
    }
  }

  const openHistory = async (productId: number) => {
    setShowProductHistory(productId)
    try {
      setProductHistory(await stockService.listProductMovements(productId))
    } catch (historyError) {
      setError(getApiErrorMessage(historyError, 'Não foi possível carregar o histórico.'))
    }
  }

  const toggleProductStatus = async () => {
    if (!confirmProduct) return
    setSaving(true)
    try {
      await stockService.updateProductStatus(confirmProduct.id, !confirmProduct.active)
      setMessage(confirmProduct.active ? 'Produto inativado. Histórico preservado.' : 'Produto ativado.')
      setConfirmProduct(null)
      await loadStock()
    } catch (statusError) {
      setError(getApiErrorMessage(statusError, 'Não foi possível alterar o status do produto.'))
      setConfirmProduct(null)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default', pb: 5, overflowX: 'hidden' }}>
      <Box sx={{ bgcolor: 'primary.main', color: 'white' }}>
        <Toolbar>
          <IconButton color="inherit" onClick={() => navigate('/modulos')} aria-label="Voltar ao painel">
            <ArrowBackIcon />
          </IconButton>
          <InventoryIcon sx={{ mr: 1 }} />
          <Typography variant="h6" sx={{ flexGrow: 1 }}>Estoque</Typography>
          <ThemeToggle />
          {canEdit && <Button color="inherit" startIcon={<AddIcon />} onClick={() => openProductForm()}>Novo produto</Button>}
        </Toolbar>
      </Box>
      <Container maxWidth="xl" sx={{ mt: 3 }}>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        {message && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setMessage('')}>{message}</Alert>}
        {loading ? (
          <Box sx={{ py: 8, textAlign: 'center' }}><CircularProgress /></Box>
        ) : (
          <>
            <Grid container spacing={2} sx={{ mb: 3 }}>
              <Grid item xs={12} sm={6}>
                <Card><CardContent>
                  <Typography color="text.secondary">Produtos ativos</Typography>
                  <Typography variant="h3" fontWeight={800}>{summary?.activeProducts ?? 0}</Typography>
                </CardContent></Card>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Card><CardContent>
                  <Typography color="text.secondary">Com estoque baixo</Typography>
                  <Typography variant="h3" fontWeight={800} color={summary?.lowStockProducts ? 'warning.main' : 'primary.main'}>
                    {summary?.lowStockProducts ?? 0}
                  </Typography>
                </CardContent></Card>
              </Grid>
            </Grid>
            <Typography variant="h5" color="primary" gutterBottom>Produtos</Typography>
            {products.length === 0 ? (
              <Alert severity="info">Nenhum produto cadastrado ainda.</Alert>
            ) : (
              <Grid container spacing={2}>
                {products.map((product) => (
                  <Grid item xs={12} md={6} xl={4} key={product.id}>
                    <Card>
                      <CardContent>
                        <Stack direction="row" justifyContent="space-between" alignItems="flex-start" gap={1}>
                          <Box sx={{ minWidth: 0 }}>
                            <Typography variant="h6" sx={{ overflowWrap: 'anywhere' }}>{product.name}</Typography>
                            <Typography variant="body2" color="text.secondary">{product.category || 'Sem categoria'} · {product.unit}</Typography>
                          </Box>
                          <Chip size="small" color={product.active ? 'success' : 'default'} label={product.active ? 'Ativo' : 'Inativo'} />
                        </Stack>
                        <Typography sx={{ mt: 2 }}>
                          Saldo: <strong>{product.currentQuantity} {product.unit}</strong> · Mínimo: {product.minimumQuantity} {product.unit}
                          {product.basketQuantity != null && product.basketQuantity > 0 && ` · Cesta: ${product.basketQuantity}/família`}
                        </Typography>
                        {product.lowStock && <Alert severity="warning" sx={{ mt: 1 }}>Estoque baixo</Alert>}
                        <Stack direction="row" flexWrap="wrap" gap={1} sx={{ mt: 2 }}>
                          {canEdit && product.active && (
                            <>
                              <Button size="small" startIcon={<EditIcon />} onClick={() => openProductForm(product)}>Editar</Button>
                              <Button size="small" startIcon={<AddIcon />} onClick={() => openMovementForm(product, 'ENTRADA_DOACAO')}>Entrada</Button>
                              <Button size="small" startIcon={<SwapVertIcon />} onClick={() => openMovementForm(product, 'PERDA')}>Saída (perda)</Button>
                              <Button size="small" onClick={() => openMovementForm(product, 'AJUSTE')}>Ajuste</Button>
                            </>
                          )}
                          {canEdit && !product.active && (
                            <Button size="small" onClick={() => setConfirmProduct(product)}>Ativar</Button>
                          )}
                          {canEdit && product.active && (
                            <Button size="small" color="warning" onClick={() => setConfirmProduct(product)}>Inativar</Button>
                          )}
                          <Button size="small" onClick={() => void openHistory(product.id)}>Histórico</Button>
                        </Stack>
                      </CardContent>
                    </Card>
                  </Grid>
                ))}
              </Grid>
            )}

            <Typography variant="h5" color="primary" sx={{ mt: 4, mb: 1 }}>Movimentações recentes</Typography>
            {movements.length === 0 ? (
              <Alert severity="info">Nenhuma movimentação registrada ainda.</Alert>
            ) : (
              <Grid container spacing={1}>
                {movements.slice(0, 50).map((movement) => (
                  <Grid item xs={12} md={6} key={movement.id}>
                    <Card variant="outlined">
                      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                        <Typography fontWeight={700}>{movement.productName} · {movementLabels[movement.type]}</Typography>
                        <Typography variant="body2">
                          {movement.previousBalance ?? '—'} → {movement.newBalance ?? '—'} {products.find((p) => p.id === movement.productId)?.unit || ''}
                          {' · '}Quantidade: {movement.quantity}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">{movement.reason}</Typography>
                        <Typography variant="caption" color="text.secondary">
                          {movement.username || 'Usuário registrado'} · {new Date(movement.createdAt).toLocaleString('pt-BR')}
                        </Typography>
                      </CardContent>
                    </Card>
                  </Grid>
                ))}
              </Grid>
            )}
          </>
        )}
      </Container>

      <Dialog open={productDialog} onClose={() => !saving && setProductDialog(false)} fullWidth maxWidth="sm" fullScreen={isMobileDialog}>
        <Box component="form" onSubmit={saveProduct}>
          <DialogTitle>{editingProduct ? 'Editar produto' : 'Cadastrar produto'}</DialogTitle>
          <DialogContent>
            {movementError && <Alert severity="error" sx={{ mb: 2 }}>{movementError}</Alert>}
            <Stack spacing={2} sx={{ mt: 1 }}>
              <TextField required fullWidth label="Nome" value={productForm.name} onChange={(event) => setProductForm({ ...productForm, name: event.target.value })} inputProps={{ maxLength: 255 }} />
              <TextField fullWidth label="Categoria" value={productForm.category} onChange={(event) => setProductForm({ ...productForm, category: event.target.value })} inputProps={{ maxLength: 100 }} />
              <TextField required fullWidth label="Unidade de medida" placeholder="Ex.: unidade, kg, pacote" value={productForm.unit} onChange={(event) => setProductForm({ ...productForm, unit: event.target.value })} inputProps={{ maxLength: 50 }} />
              <TextField required fullWidth type="number" label="Estoque mínimo" value={productForm.minimumQuantity} onChange={(event) => setProductForm({ ...productForm, minimumQuantity: Number(event.target.value) })} inputProps={{ min: 0, step: 1 }} />
              <TextField fullWidth type="number" label="Quantidade por família (cesta)" value={productForm.basketQuantity ?? ''} onChange={(event) => setProductForm({ ...productForm, basketQuantity: event.target.value === '' ? undefined : Number(event.target.value) })} helperText="Quanto cada família contemplada deve levar deste item. Em branco = fora da cesta." inputProps={{ min: 0, step: 1 }} />
              {!editingProduct && <Alert severity="info">O produto começa com saldo zero; use uma movimentação de entrada para registrar estoque.</Alert>}
            </Stack>
          </DialogContent>
          <DialogActions sx={{ p: 2, gap: 1, flexWrap: 'wrap' }}>
            <Button onClick={() => setProductDialog(false)} disabled={saving}>Cancelar</Button>
            <Button type="submit" variant="contained" disabled={saving}>{saving ? 'Salvando...' : 'Salvar'}</Button>
          </DialogActions>
        </Box>
      </Dialog>

      <Dialog open={movementDialog} onClose={() => !saving && setMovementDialog(false)} fullWidth maxWidth="sm" fullScreen={isMobileDialog}>
        <Box component="form" onSubmit={saveMovement}>
          <DialogTitle>Registrar movimentação</DialogTitle>
          <DialogContent>
            {movementError && <Alert severity="error" sx={{ mb: 2 }}>{movementError}</Alert>}
            <Stack spacing={2} sx={{ mt: 1 }}>
              <FormControl fullWidth>
                <InputLabel>Tipo de movimentação</InputLabel>
                <Select
                  value={movementForm.type}
                  label="Tipo de movimentação"
                  onChange={(event) => setMovementForm({ ...movementForm, type: event.target.value as MovementType })}
                >
                  {Object.entries(movementLabels)
                    .filter(([value]) => value !== 'SAIDA_DISTRIBUICAO')
                    .map(([value, label]) => (
                    <MenuItem key={value} value={value}>{label}</MenuItem>
                  ))}
                </Select>
              </FormControl>
              {movementForm.type === 'AJUSTE' && (
                <FormControl fullWidth>
                  <InputLabel>Direção do ajuste</InputLabel>
                  <Select value={movementForm.adjustmentDirection || ''} label="Direção do ajuste" onChange={(event) => setMovementForm({ ...movementForm, adjustmentDirection: event.target.value as 'ENTRADA' | 'SAIDA' })}>
                    <MenuItem value="ENTRADA">Aumentar saldo</MenuItem>
                    <MenuItem value="SAIDA">Reduzir saldo</MenuItem>
                  </Select>
                </FormControl>
              )}
              <TextField required fullWidth type="number" label="Quantidade" value={movementForm.quantity} onChange={(event) => setMovementForm({ ...movementForm, quantity: Number(event.target.value) })} inputProps={{ min: 1, step: 1 }} />
              <TextField required fullWidth label="Motivo" value={movementForm.reason} onChange={(event) => setMovementForm({ ...movementForm, reason: event.target.value })} inputProps={{ maxLength: 2000 }} />
              <TextField fullWidth multiline minRows={2} label="Observação" value={movementForm.observation || ''} onChange={(event) => setMovementForm({ ...movementForm, observation: event.target.value })} inputProps={{ maxLength: 2000 }} />
            </Stack>
          </DialogContent>
          <DialogActions sx={{ p: 2, gap: 1, flexWrap: 'wrap' }}>
            <Button onClick={() => setMovementDialog(false)} disabled={saving}>Cancelar</Button>
            <Button type="submit" variant="contained" disabled={saving}>{saving ? 'Registrando...' : 'Confirmar movimentação'}</Button>
          </DialogActions>
        </Box>
      </Dialog>

      <Dialog open={confirmMovement} onClose={() => !saving && setConfirmMovement(false)} fullWidth maxWidth="xs">
        <DialogTitle>Confirmar movimentação</DialogTitle>
        <DialogContent>
          <Typography>
            Confirma {movementForm.type === 'AJUSTE' ? 'o ajuste do saldo' : 'o registro da perda'} de {movementForm.quantity} unidade(s)?
            Essa ação ficará registrada no histórico.
          </Typography>
          {movementError && <Alert severity="error" sx={{ mt: 2 }}>{movementError}</Alert>}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmMovement(false)} disabled={saving}>Voltar</Button>
          <Button variant="contained" color="warning" onClick={() => void registerMovement()} disabled={saving}>
            {saving ? 'Registrando...' : 'Confirmar'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={showProductHistory !== null} onClose={() => setShowProductHistory(null)} fullWidth maxWidth="md" fullScreen={isMobileDialog}>
        <DialogTitle>Histórico do produto</DialogTitle>
        <DialogContent>
          {productHistory.length === 0 ? (
            <Alert severity="info">Nenhuma movimentação registrada para este produto.</Alert>
          ) : productHistory.map((movement) => (
            <Card variant="outlined" sx={{ mb: 1 }} key={movement.id}>
              <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography fontWeight={700}>{movementLabels[movement.type]} · {movement.quantity}</Typography>
                <Typography>Saldo: {movement.previousBalance ?? '—'} → {movement.newBalance ?? '—'}</Typography>
                <Typography variant="body2">{movement.reason}</Typography>
                <Typography variant="caption" color="text.secondary">
                  {movement.username || 'Usuário registrado'} · {new Date(movement.createdAt).toLocaleString('pt-BR')}
                </Typography>
              </CardContent>
            </Card>
          ))}
        </DialogContent>
        <DialogActions><Button onClick={() => setShowProductHistory(null)}>Fechar</Button></DialogActions>
      </Dialog>

      <Dialog open={Boolean(confirmProduct)} onClose={() => !saving && setConfirmProduct(null)} fullWidth maxWidth="xs">
        <DialogTitle>{confirmProduct?.active ? 'Inativar produto?' : 'Ativar produto?'}</DialogTitle>
        <DialogContent>
          <Typography>
            {confirmProduct?.active
              ? `O produto ${confirmProduct.name} deixará de aceitar movimentações. O histórico será preservado.`
              : `Deseja ativar o produto ${confirmProduct?.name}?`}
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmProduct(null)} disabled={saving}>Cancelar</Button>
          <Button variant="contained" onClick={() => void toggleProductStatus()} disabled={saving}>Confirmar</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default StockPage

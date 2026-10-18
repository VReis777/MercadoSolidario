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
import AddIcon from '@mui/icons-material/Add'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import LocalShippingIcon from '@mui/icons-material/LocalShipping'
import RemoveCircleOutlineIcon from '@mui/icons-material/RemoveCircleOutline'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import distributionService, { Distribution } from '../services/distributionService'
import familyService from '../services/familyService'
import stockService, { Product } from '../services/stockService'
import { Family } from '../types/family'
import { getApiErrorMessage } from '../utils/apiError'
import { newRequestReference } from '../utils/idempotency'
import ThemeToggle from '../components/ThemeToggle'

interface DistributionLine {
  productId: string
  quantity: string
}

const emptyLine = (): DistributionLine => ({ productId: '', quantity: '1' })

function DistributionsPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const isMobile = useMediaQuery('(max-width:599px)')
  const canCancel = Boolean(user?.roles.includes('ROLE_ADMIN'))
  const [families, setFamilies] = useState<Family[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [distributions, setDistributions] = useState<Distribution[]>([])
  const [familyId, setFamilyId] = useState('')
  const [lines, setLines] = useState<DistributionLine[]>([emptyLine()])
  const [observation, setObservation] = useState('')
  const [reference, setReference] = useState('')
  const [idempotencyKey, setIdempotencyKey] = useState(() => newRequestReference())
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [confirmationOpen, setConfirmationOpen] = useState(false)
  const [cancelTarget, setCancelTarget] = useState<Distribution | null>(null)
  const [cancelReason, setCancelReason] = useState('')
  const [filter, setFilter] = useState('')
  const [dateFilter, setDateFilter] = useState('')

  const loadData = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [familyPage, productList, distributionList] = await Promise.all([
        familyService.search({ page: 0, size: 100, sortBy: 'responsibleName', sortDir: 'asc' }),
        stockService.listProducts(),
        distributionService.list(),
      ])
      const remainingFamilyPages = await Promise.all(
        Array.from({ length: Math.max(0, familyPage.totalPages - 1) }, (_, index) =>
          familyService.search({ page: index + 1, size: 100, sortBy: 'responsibleName', sortDir: 'asc' }),
        ),
      )
      const allFamilies = [familyPage, ...remainingFamilyPages].flatMap((page) => page.content)
      setFamilies(allFamilies.filter(
        (family) => family.status !== 'INATIVA' && family.status !== 'CANCELADA',
      ))
      setProducts(productList.filter((product) => product.active))
      setDistributions(distributionList)
    } catch (loadError) {
      setError(getApiErrorMessage(loadError, 'Não foi possível carregar os dados de distribuição.'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadData()
  }, [loadData])

  const lineIssues = lines.map((line) => {
    const product = products.find((item) => item.id === Number(line.productId))
    const quantity = Number(line.quantity)
    if (!product || !Number.isInteger(quantity) || quantity < 1) return ''
    if (quantity > product.currentQuantity) return `Disponível: ${product.currentQuantity} ${product.unit}`
    return ''
  })
  const duplicateProduct = lines.some((line, index) =>
    Boolean(line.productId) && lines.findIndex((item) => item.productId === line.productId) !== index,
  )
  const selectedFamily = families.find((family) => family.id === Number(familyId))
  const canSubmit = Boolean(
    selectedFamily
      && lines.length > 0
      && lines.every((line) => {
        const product = products.find((item) => item.id === Number(line.productId))
        const quantity = Number(line.quantity)
        return product && Number.isInteger(quantity) && quantity > 0 && quantity <= product.currentQuantity
      })
      && !duplicateProduct
      && !saving,
  )

  const requestCreate = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (canSubmit) setConfirmationOpen(true)
  }

  const submitDistribution = async () => {
    if (!selectedFamily || !canSubmit) return
    setSaving(true)
    setError('')
    try {
      const result = await distributionService.create({
        familyId: selectedFamily.id,
        items: lines.map((line) => ({ productId: Number(line.productId), quantity: Number(line.quantity) })),
        observation: observation.trim() || undefined,
        reference: reference.trim() || undefined,
        idempotencyKey,
      })
      setConfirmationOpen(false)
      setMessage(`Distribuição #${result.id} confirmada; o estoque e o histórico foram atualizados.`)
      setFamilyId('')
      setLines([emptyLine()])
      setObservation('')
      setReference('')
      setIdempotencyKey(newRequestReference())
      await loadData()
    } catch (createError) {
      setConfirmationOpen(false)
      setError(getApiErrorMessage(createError, 'Não foi possível registrar a distribuição.'))
    } finally {
      setSaving(false)
    }
  }

  const cancelDistribution = async () => {
    if (!cancelTarget) return
    setSaving(true)
    setError('')
    try {
      await distributionService.cancel(cancelTarget.id, cancelReason)
      setMessage('Distribuição cancelada no registro. Nenhum produto foi devolvido automaticamente ao estoque.')
      setCancelTarget(null)
      setCancelReason('')
      await loadData()
    } catch (cancelError) {
      setError(getApiErrorMessage(cancelError, 'Não foi possível cancelar a distribuição.'))
      setCancelTarget(null)
    } finally {
      setSaving(false)
    }
  }

  const filteredDistributions = distributions.filter((distribution) => {
    if (!filter) return true
    const term = filter.toLocaleLowerCase('pt-BR')
    return distribution.familyResponsibleName.toLocaleLowerCase('pt-BR').includes(term)
      || distribution.items.some((item) => item.productName.toLocaleLowerCase('pt-BR').includes(term))
      || distribution.createdBy.toLocaleLowerCase('pt-BR').includes(term)
  }).filter((distribution) => !dateFilter
    || (distribution.completedAt || distribution.createdAt).slice(0, 10) === dateFilter)

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default', pb: 5, overflowX: 'hidden' }}>
      <Box sx={{ bgcolor: 'primary.main', color: 'white' }}>
        <Toolbar>
          <IconButton color="inherit" onClick={() => navigate('/modulos')} aria-label="Voltar ao painel"><ArrowBackIcon /></IconButton>
          <LocalShippingIcon sx={{ mr: 1 }} />
          <Typography variant="h6" sx={{ flexGrow: 1 }}>Distribuições</Typography>
          <ThemeToggle />
        </Toolbar>
      </Box>
      <Container maxWidth="xl" sx={{ mt: 3 }}>
        {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}
        {message && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setMessage('')}>{message}</Alert>}
        {loading ? (
          <Box sx={{ textAlign: 'center', py: 6 }}><CircularProgress /></Box>
        ) : (
          <>
            <Card sx={{ mb: 4 }}>
              <CardContent sx={{ p: { xs: 2, sm: 3 } }}>
                <Typography variant="h5" color="primary" fontWeight={700} gutterBottom>Distribuir benefício</Typography>
                <Alert severity="info" sx={{ mb: 2 }}>
                  A equipe registra manualmente a entrega. A seleção não usa pontuação de prioridade e não altera o status da família.
                </Alert>
                {families.length === 0 && <Alert severity="warning" sx={{ mb: 2 }}>Não há famílias elegíveis para seleção.</Alert>}
                {products.length === 0 && <Alert severity="warning" sx={{ mb: 2 }}>Não há produtos ativos disponíveis.</Alert>}
                <Box component="form" onSubmit={requestCreate}>
                  <Stack spacing={2}>
                    <FormControl fullWidth required>
                      <InputLabel>Família</InputLabel>
                      <Select value={familyId} label="Família" onChange={(event) => setFamilyId(event.target.value)}>
                        {families.map((family) => (
                          <MenuItem value={family.id} key={family.id}>
                            {family.responsibleName} · {family.status}
                          </MenuItem>
                        ))}
                      </Select>
                    </FormControl>
                    <Stack direction={{ xs: 'column', sm: 'row' }} alignItems={{ sm: 'center' }} justifyContent="space-between" gap={1}>
                      <Typography fontWeight={700}>Produtos e quantidades</Typography>
                      {products.some((item) => (item.basketQuantity ?? 0) > 0) && (
                        <Button
                          size="small"
                          variant="outlined"
                          onClick={() => setLines(
                            products
                              .filter((item) => (item.basketQuantity ?? 0) > 0 && item.currentQuantity > 0)
                              .map((item) => ({
                                productId: String(item.id),
                                quantity: String(Math.min(item.basketQuantity as number, item.currentQuantity)),
                              })),
                          )}
                        >
                          Preencher com a cesta completa
                        </Button>
                      )}
                    </Stack>
                    <Typography variant="body2" color="text.secondary">
                      Presumimos que a família levou a cesta completa (a quantidade por família definida em cada item do estoque).
                      Confira a lista e ajuste apenas o que ela não pegou ou pegou a menos.
                    </Typography>
                    {lines.map((line, index) => {
                      const product = products.find((item) => item.id === Number(line.productId))
                      const issue = lineIssues[index]
                      return (
                        <Grid container spacing={1} alignItems="center" key={index}>
                          <Grid item xs={12} sm={7}>
                            <FormControl fullWidth required>
                              <InputLabel>Produto</InputLabel>
                              <Select
                                value={line.productId}
                                label="Produto"
                                onChange={(event) => setLines((current) => current.map((item, itemIndex) =>
                                  itemIndex === index ? { ...item, productId: event.target.value } : item,
                                ))}
                              >
                                {products.map((item) => (
                                  <MenuItem key={item.id} value={item.id}>
                                    {item.name} · disponível {item.currentQuantity} {item.unit}
                                  </MenuItem>
                                ))}
                              </Select>
                            </FormControl>
                          </Grid>
                          <Grid item xs={9} sm={3}>
                            <TextField
                              required
                              fullWidth
                              type="number"
                              label={`Quantidade${product ? ` (${product.unit})` : ''}`}
                              value={line.quantity}
                              onChange={(event) => setLines((current) => current.map((item, itemIndex) =>
                                itemIndex === index ? { ...item, quantity: event.target.value } : item,
                              ))}
                              inputProps={{ min: 1, step: 1 }}
                              error={Boolean(issue)}
                              helperText={issue}
                            />
                          </Grid>
                          <Grid item xs={3} sm={2}>
                            <Button
                              fullWidth
                              aria-label="Remover produto"
                              disabled={lines.length === 1}
                              onClick={() => setLines((current) => current.filter((_, itemIndex) => itemIndex !== index))}
                            >
                              <RemoveCircleOutlineIcon />
                            </Button>
                          </Grid>
                        </Grid>
                      )
                    })}
                    {duplicateProduct && <Alert severity="warning">Cada produto deve aparecer uma única vez na mesma distribuição.</Alert>}
                    <Button
                      startIcon={<AddIcon />}
                      sx={{ alignSelf: 'flex-start', minHeight: 44 }}
                      onClick={() => setLines((current) => [...current, emptyLine()])}
                    >
                      Adicionar produto
                    </Button>
                    <TextField fullWidth label="Observação operacional" multiline minRows={2} value={observation} onChange={(event) => setObservation(event.target.value)} inputProps={{ maxLength: 2000 }} />
                    <TextField fullWidth label="Referência/comprovante (opcional)" value={reference} onChange={(event) => setReference(event.target.value)} inputProps={{ maxLength: 255 }} />
                    <Button type="submit" variant="contained" size="large" disabled={!canSubmit} sx={{ minHeight: 48 }}>
                      Revisar distribuição
                    </Button>
                  </Stack>
                </Box>
              </CardContent>
            </Card>

            <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={2} sx={{ mb: 2 }}>
              <Typography variant="h5" color="primary">Histórico</Typography>
              <Stack direction={{ xs: 'column', sm: 'row' }} gap={1} sx={{ width: { xs: '100%', md: 'auto' } }}>
                <TextField
                  label="Família, produto ou usuário"
                  value={filter}
                  onChange={(event) => setFilter(event.target.value)}
                  sx={{ width: { xs: '100%', sm: 320 } }}
                />
                <TextField
                  type="date"
                  label="Data"
                  value={dateFilter}
                  onChange={(event) => setDateFilter(event.target.value)}
                  InputLabelProps={{ shrink: true }}
                  sx={{ width: { xs: '100%', sm: 180 } }}
                />
              </Stack>
            </Stack>
            {filteredDistributions.length === 0 ? (
              <Alert severity="info">Nenhuma distribuição registrada.</Alert>
            ) : (
              <Grid container spacing={2}>
                {filteredDistributions.map((distribution) => (
                  <Grid item xs={12} md={6} xl={4} key={distribution.id}>
                    <Card variant="outlined">
                      <CardContent>
                        <Stack direction="row" justifyContent="space-between" gap={1}>
                          <Typography variant="h6">Distribuição #{distribution.id}</Typography>
                          <Chip size="small" label={distribution.status} color={distribution.status === 'CONFIRMADA' ? 'success' : 'default'} />
                        </Stack>
                        <Typography fontWeight={600}>{distribution.familyResponsibleName}</Typography>
                        <Typography variant="body2" color="text.secondary">
                          {new Date(distribution.completedAt || distribution.createdAt).toLocaleString('pt-BR')} · {distribution.createdBy}
                        </Typography>
                        <Stack spacing={0.5} sx={{ mt: 1 }}>
                          {distribution.items.map((item) => (
                            <Typography variant="body2" key={item.id}>
                              {item.quantity} {item.unit} — {item.productName}
                            </Typography>
                          ))}
                        </Stack>
                        {distribution.observation && <Typography variant="body2" sx={{ mt: 1 }}>{distribution.observation}</Typography>}
                        {distribution.reference && <Typography variant="caption" display="block">Referência: {distribution.reference}</Typography>}
                        {distribution.cancellationReason && <Typography variant="body2" color="warning.main">Motivo do cancelamento: {distribution.cancellationReason}</Typography>}
                        {canCancel && distribution.status === 'CONFIRMADA' && (
                          <Button
                            color="warning"
                            sx={{ mt: 1, minHeight: 44 }}
                            onClick={() => setCancelTarget(distribution)}
                          >
                            Cancelar registro
                          </Button>
                        )}
                      </CardContent>
                    </Card>
                  </Grid>
                ))}
              </Grid>
            )}
          </>
        )}
      </Container>

      <Dialog open={confirmationOpen} onClose={() => !saving && setConfirmationOpen(false)} fullWidth maxWidth="sm" fullScreen={isMobile}>
        <DialogTitle>Confirmar entrega manual</DialogTitle>
        <DialogContent>
          <Typography gutterBottom>Família: <strong>{selectedFamily?.responsibleName}</strong></Typography>
          <Typography gutterBottom>Produtos a baixar do estoque:</Typography>
          <Stack spacing={1}>
            {lines.map((line) => {
              const product = products.find((item) => item.id === Number(line.productId))
              return (
                <Typography key={line.productId}>
                  {line.quantity} {product?.unit} — {product?.name} (saldo atual: {product?.currentQuantity})
                </Typography>
              )
            })}
          </Stack>
          {observation.trim() && <Typography sx={{ mt: 2 }}>Observação: {observation.trim()}</Typography>}
          <Alert severity="warning" sx={{ mt: 2 }}>
            Ao confirmar, o estoque será reduzido e o movimento ficará auditado. O status da família não será alterado.
          </Alert>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setConfirmationOpen(false)} disabled={saving}>Voltar</Button>
          <Button variant="contained" onClick={() => void submitDistribution()} disabled={saving}>
            {saving ? 'Registrando...' : 'Confirmar distribuição'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={Boolean(cancelTarget)} onClose={() => !saving && setCancelTarget(null)} fullWidth maxWidth="sm">
        <DialogTitle>Cancelar registro da distribuição?</DialogTitle>
        <DialogContent>
          <Alert severity="warning" sx={{ mb: 2 }}>
            Cancelar o registro não devolve os produtos ao estoque. Registre a devolução separadamente somente se os produtos retornarem fisicamente.
          </Alert>
          <TextField
            required
            fullWidth
            multiline
            minRows={2}
            label="Motivo do cancelamento"
            value={cancelReason}
            onChange={(event) => setCancelReason(event.target.value)}
            inputProps={{ maxLength: 2000 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCancelTarget(null)} disabled={saving}>Voltar</Button>
          <Button variant="contained" color="warning" onClick={() => void cancelDistribution()} disabled={saving || !cancelReason.trim()}>
            Cancelar registro
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default DistributionsPage

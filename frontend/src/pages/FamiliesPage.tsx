import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import {
  Container,
  Box,
  Typography,
  Button,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  TextField,
  IconButton,
  CircularProgress,
  Alert,
  Toolbar,
  Fab,
  Tooltip,
  Grid,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
  Card,
  CardContent,
  AppBar,
  useMediaQuery,
} from '@mui/material'
import {
  Add as AddIcon,
  Search as SearchIcon,
  Edit as EditIcon,
  Visibility as VisibilityIcon,
  Refresh as RefreshIcon,
  ArrowBack as ArrowBackIcon,
  Upload as UploadIcon,
  History as HistoryIcon,
} from '@mui/icons-material'
import familyService from '../services/familyService'
import { Family } from '../types/family'
import FamilyForm from '../components/FamilyForm'
import FamilyDetails from '../components/FamilyDetails'
import ImportPreviousBeneficiaries from '../components/ImportPreviousBeneficiaries'
import ThemeToggle from '../components/ThemeToggle'
import { getApiErrorMessage } from '../utils/apiError'
import { maskCpf } from '../utils/cpf'

function FamiliesPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const isMobile = useMediaQuery('(max-width:767px)')
  const [families, setFamilies] = useState<Family[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [maxIncome, setMaxIncome] = useState('')
  const [onlyDisabled, setOnlyDisabled] = useState(false)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [selectedFamily, setSelectedFamily] = useState<Family | null>(null)
  const [formOpen, setFormOpen] = useState(false)
  const [detailsOpen, setDetailsOpen] = useState(false)
  const [importOpen, setImportOpen] = useState(false)
  const [showPreviousBeneficiaries, setShowPreviousBeneficiaries] = useState(false)

  const hasAdvancedFilter = Boolean(maxIncome) || onlyDisabled

  useEffect(() => {
    loadFamilies()
  }, [page, statusFilter, showPreviousBeneficiaries])

  const loadFamilies = async () => {
    setLoading(true)
    setError('')
    try {
      const params: Record<string, string | number> = {
        page: hasAdvancedFilter ? 0 : page,
        size: hasAdvancedFilter ? 500 : 10,
      }
      if (statusFilter) params.status = statusFilter
      if (searchTerm) params.name = searchTerm
      if (showPreviousBeneficiaries) params.previousBeneficiary = 'true'

      const response = await familyService.search(params)
      setFamilies(response.content)
      setTotalPages(response.totalPages)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Erro ao carregar famílias'))
    } finally {
      setLoading(false)
    }
  }

  const visibleFamilies = families.filter((family) => {
    if (maxIncome && Number(family.totalIncome) >= Number(maxIncome.replace(',', '.'))) return false
    if (onlyDisabled && !(family.disabledCount && family.disabledCount > 0)) return false
    return true
  })

  const handleSearch = () => {
    setPage(0)
    loadFamilies()
  }

  const handleView = async (family: Family) => {
    try {
      const details = await familyService.getById(family.id)
      setSelectedFamily(details)
      setDetailsOpen(true)
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível abrir os detalhes da família.'))
    }
  }

  const handleEdit = (family: Family) => {
    setSelectedFamily(family)
    setFormOpen(true)
  }

  const handleNewFamily = () => {
    setSelectedFamily(null)
    setFormOpen(true)
  }

  const handleFormSuccess = () => {
    loadFamilies()
  }

  const handleDetailsEdit = () => {
    setDetailsOpen(false)
    setFormOpen(true)
  }

  const canEdit = () => {
    if (!user?.roles) return false
    return user.roles.some((role: string) =>
      ['ROLE_ADMIN', 'ROLE_RH_TESOURARIA'].includes(role)
    )
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'CADASTRADA': return 'default'
      case 'EM_ANALISE': return 'warning'
      case 'APROVADA': return 'info'
      case 'CONTEMPLADA': return 'primary'
      case 'ATENDIDA': return 'success'
      case 'INATIVA': return 'secondary'
      case 'CANCELADA': return 'error'
      default: return 'default'
    }
  }

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value)
  }

  const familyActions = (family: Family) => (
    <Box>
      <Tooltip title="Visualizar">
        <IconButton size="small" onClick={() => handleView(family)} aria-label={`Visualizar ${family.responsibleName}`}>
          <VisibilityIcon />
        </IconButton>
      </Tooltip>
      {canEdit() && (
        <Tooltip title="Editar">
          <IconButton size="small" onClick={() => handleEdit(family)} aria-label={`Editar ${family.responsibleName}`}>
            <EditIcon />
          </IconButton>
        </Tooltip>
      )}
    </Box>
  )

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default', pb: 8, overflowX: 'hidden' }}>
      <AppBar position="static" elevation={0} color="primary">
        <Toolbar>
          <IconButton color="inherit" onClick={() => navigate('/modulos')} aria-label="Voltar ao dashboard">
            <ArrowBackIcon />
          </IconButton>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>Famílias</Typography>
          <ThemeToggle />
          {canEdit() && (
            <Button color="inherit" startIcon={<UploadIcon />} onClick={() => setImportOpen(true)} sx={{ mr: 1 }}>
              Importar
            </Button>
          )}
          <Button color="inherit" startIcon={<RefreshIcon />} onClick={loadFamilies} disabled={loading}>
            Atualizar
          </Button>
        </Toolbar>
      </AppBar>

      <Container maxWidth="xl" sx={{ mt: 3 }}>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
            {error}
          </Alert>
        )}

        <Paper sx={{ p: 3, mb: 3 }}>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={6} md={4}>
              <TextField
                fullWidth
                label="Buscar por nome"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                InputProps={{
                  endAdornment: (
                    <IconButton onClick={handleSearch} size="small" aria-label="Buscar">
                      <SearchIcon />
                    </IconButton>
                  ),
                }}
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <FormControl fullWidth>
                <InputLabel>Status</InputLabel>
                <Select
                  value={statusFilter}
                  label="Status"
                  onChange={(e) => {
                    setStatusFilter(e.target.value)
                    setPage(0)
                  }}
                >
                  <MenuItem value="">Todos</MenuItem>
                  <MenuItem value="CADASTRADA">Cadastrada</MenuItem>
                  <MenuItem value="EM_ANALISE">Em Análise</MenuItem>
                  <MenuItem value="APROVADA">Aprovada</MenuItem>
                  <MenuItem value="CONTEMPLADA">Contemplada</MenuItem>
                  <MenuItem value="ATENDIDA">Atendida</MenuItem>
                  <MenuItem value="INATIVA">Inativa</MenuItem>
                  <MenuItem value="CANCELADA">Cancelada</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6} md={2}>
              <TextField
                fullWidth
                type="number"
                label="Renda até (R$)"
                value={maxIncome}
                onChange={(e) => { setMaxIncome(e.target.value); setPage(0) }}
                inputProps={{ min: 0, step: 100 }}
                helperText="Ex.: 1000"
              />
            </Grid>
            <Grid item xs={12} sm={6} md={2}>
              <Button
                fullWidth
                variant={onlyDisabled ? 'contained' : 'outlined'}
                onClick={() => { setOnlyDisabled(!onlyDisabled); setPage(0) }}
                sx={{ height: 56 }}
              >
                {onlyDisabled ? 'Todas' : 'Com deficiência'}
              </Button>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Button
                fullWidth
                variant={showPreviousBeneficiaries ? "contained" : "outlined"}
                color={showPreviousBeneficiaries ? "secondary" : "primary"}
                startIcon={<HistoryIcon />}
                onClick={() => {
                  setShowPreviousBeneficiaries(!showPreviousBeneficiaries)
                  setPage(0)
                }}
                sx={{ height: 56 }}
              >
                {showPreviousBeneficiaries ? 'Todas as famílias' : 'Beneficiários anteriores'}
              </Button>
            </Grid>
          </Grid>
        </Paper>

        {loading ? (
          <Box sx={{ textAlign: 'center', py: 6 }}><CircularProgress /></Box>
        ) : families.length === 0 ? (
          <Alert severity="info">Nenhuma família encontrada</Alert>
        ) : isMobile ? (
          <Grid container spacing={2}>
            {visibleFamilies.map((family) => (
              <Grid item xs={12} key={family.id}>
                <Card>
                  <CardContent>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 1, mb: 1 }}>
                      <Typography fontWeight={700}>
                        {family.responsibleName}
                        {(family.disabledCount || 0) > 0 && (
                          <Tooltip title={`${family.disabledCount} pessoa(s) com deficiência na família`}>
                            <Chip label="#" size="small" color="info" sx={{ ml: 1 }} />
                          </Tooltip>
                        )}
                      </Typography>
                      <Chip label={family.status.replace('_', ' ')} color={getStatusColor(family.status)} size="small" />
                    </Box>
                    <Typography variant="body2">CPF: {maskCpf(family.cpf)}</Typography>
                    <Typography variant="body2">{family.householdSize} pessoas · {formatCurrency(family.totalIncome)}</Typography>
                    {familyActions(family)}
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        ) : (
          <Paper>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Responsável</TableCell>
                    <TableCell>CPF</TableCell>
                    <TableCell>Pessoas</TableCell>
                    <TableCell>Renda</TableCell>
                    <TableCell>Origem</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell align="right">Ações</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {visibleFamilies.map((family) => (
                    <TableRow key={family.id}>
                      <TableCell>
                        {family.responsibleName}
                        {(family.disabledCount || 0) > 0 && (
                          <Tooltip title={`${family.disabledCount} pessoa(s) com deficiência na família`}>
                            <Chip label="#" size="small" color="info" sx={{ ml: 1 }} />
                          </Tooltip>
                        )}
                      </TableCell>
                      <TableCell>{maskCpf(family.cpf)}</TableCell>
                      <TableCell>{family.householdSize}</TableCell>
                      <TableCell>{formatCurrency(family.totalIncome)}</TableCell>
                      <TableCell>{family.origin.replace('_', ' ')}</TableCell>
                      <TableCell>
                        <Chip
                          label={family.status.replace('_', ' ')}
                          color={getStatusColor(family.status)}
                          size="small"
                        />
                      </TableCell>
                      <TableCell align="right">{familyActions(family)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Paper>
        )}

        <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', mt: 3, gap: 1, flexWrap: 'wrap' }}>
          <Button disabled={page === 0} onClick={() => setPage(page - 1)}>Anterior</Button>
          <Typography>Página {page + 1} de {Math.max(totalPages, 1)}</Typography>
          <Button disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}>Próxima</Button>
        </Box>

        {canEdit() && (
          <Fab
            color="primary"
            aria-label="Nova família"
            sx={{ position: 'fixed', bottom: 16, right: 16 }}
            onClick={handleNewFamily}
          >
            <AddIcon />
          </Fab>
        )}

        <FamilyForm
          open={formOpen}
          onClose={() => setFormOpen(false)}
          onSuccess={handleFormSuccess}
          family={selectedFamily}
        />

        <FamilyDetails
          open={detailsOpen}
          onClose={() => setDetailsOpen(false)}
          family={selectedFamily}
          onEdit={handleDetailsEdit}
          onUpdated={loadFamilies}
          userRoles={user?.roles || []}
        />

        <ImportPreviousBeneficiaries
          open={importOpen}
          onClose={() => setImportOpen(false)}
          onImportComplete={loadFamilies}
        />
      </Container>
    </Box>
  )
}

export default FamiliesPage

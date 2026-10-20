import { useState, useEffect } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  useMediaQuery,
  Grid,
  MenuItem,
  Select,
  FormControl,
  FormControlLabel,
  InputLabel,
  Switch,
  Box,
  Alert,
  CircularProgress,
  Typography,
  Divider,
} from '@mui/material'
import { styled } from '@mui/material/styles'
import { getApiErrorMessage } from '../utils/apiError'

const FormSection = styled(Box)(({ theme }) => ({
  marginBottom: theme.spacing(3),
}))

interface FamilyFormProps {
  open: boolean
  onClose: () => void
  onSuccess: () => void
  family?: any // FamilyResponse para edição
}

interface FormData {
  responsibleName: string
  cpf: string
  spouseName: string
  phone: string
  address: string
  householdSize: number
  totalIncome: string
  paysRent: boolean
  rentValue: string
  requestReason: string
  origin: string
  referrerName: string
  observations: string
}

const initialFormData: FormData = {
  responsibleName: '',
  cpf: '',
  spouseName: '',
  phone: '',
  address: '',
  householdSize: 1,
  totalIncome: '',
  paysRent: false,
  rentValue: '',
  requestReason: '',
  origin: '',
  referrerName: '',
  observations: '',
}

function FamilyForm({ open, onClose, onSuccess, family }: FamilyFormProps) {
  const fullScreen = useMediaQuery('(max-width:599px)')
  const [formData, setFormData] = useState<FormData>(initialFormData)
  const [errors, setErrors] = useState<Partial<Record<keyof FormData, string>>>({})
  const [loading, setLoading] = useState(false)
  const [submitError, setSubmitError] = useState('')

  useEffect(() => {
    if (family) {
      setFormData({
        responsibleName: family.responsibleName || '',
        cpf: family.cpf || '',
        spouseName: family.spouseName || '',
        phone: family.phone || '',
        address: family.address || '',
        householdSize: family.householdSize || 1,
        totalIncome: family.totalIncome?.toString() || '',
        paysRent: Boolean(family.paysRent),
        rentValue: family.rentValue?.toString() || '',
        requestReason: family.requestReason || '',
        origin: family.origin || '',
        referrerName: family.referrerName || '',
        observations: family.observations || '',
      })
    } else {
      setFormData(initialFormData)
    }
    setErrors({})
    setSubmitError('')
  }, [family, open])

  const handleChange = (field: keyof FormData) => (e: React.ChangeEvent<HTMLInputElement | { value: unknown }>) => {
    const value = e.target.value
    setFormData(prev => ({ ...prev, [field]: value }))
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: '' }))
    }
  }

  const validate = (): boolean => {
    const newErrors: Partial<Record<keyof FormData, string>> = {}

    if (!formData.responsibleName.trim()) {
      newErrors.responsibleName = 'Informe o nome completo'
    }

    if (!formData.cpf.trim()) {
      newErrors.cpf = 'CPF é obrigatório'
    } else if (!validateCPF(formData.cpf)) {
      newErrors.cpf = 'CPF inválido'
    }

    if (!formData.phone.trim()) {
      newErrors.phone = 'Telefone é obrigatório'
    } else if (formData.phone.replace(/\D/g, '').length < 8) {
      newErrors.phone = 'Telefone inválido'
    }

    if (!formData.address.trim()) {
      newErrors.address = 'Endereço é obrigatório'
    }

    if (!formData.spouseName.trim()) {
      newErrors.spouseName = 'Informe o cônjuge ou escreva "não tem"'
    }

    if (formData.householdSize < 1) {
      newErrors.householdSize = 'A quantidade de pessoas deve ser pelo menos 1'
    }

    if (!formData.totalIncome || parseFloat(formData.totalIncome) < 0) {
      newErrors.totalIncome = 'Informe uma renda válida'
    }

    if (!formData.requestReason) {
      newErrors.requestReason = 'Selecione o motivo'
    }

    if (!formData.origin) {
      newErrors.origin = 'Selecione a origem'
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const validateCPF = (cpf: string): boolean => {
    const cleaned = cpf.replace(/\D/g, '')
    if (cleaned.length !== 11) return false
    if (/^(\d)\1+$/.test(cleaned)) return false
    
    let sum = 0
    for (let i = 0; i < 9; i++) {
      sum += parseInt(cleaned[i]) * (10 - i)
    }
    const remainder = sum % 11
    const digit1 = remainder < 2 ? 0 : 11 - remainder

    sum = 0
    for (let i = 0; i < 10; i++) {
      sum += parseInt(cleaned[i]) * (11 - i)
    }
    const remainder2 = sum % 11
    const digit2 = remainder2 < 2 ? 0 : 11 - remainder2

    return digit1 === parseInt(cleaned[9]) && digit2 === parseInt(cleaned[10])
  }

  const formatCPF = (value: string): string => {
    const cleaned = value.replace(/\D/g, '')
    return cleaned.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4')
  }

  const handleCPFChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const formatted = formatCPF(e.target.value)
    setFormData(prev => ({ ...prev, cpf: formatted }))
    if (errors.cpf) {
      setErrors(prev => ({ ...prev, cpf: '' }))
    }
  }

  const handleSubmit = async () => {
    if (!validate()) return

    setLoading(true)
    setSubmitError('')

    try {
      const familyService = (await import('../services/familyService')).default
      
      const payload = {
        ...formData,
        householdSize: parseInt(formData.householdSize.toString()),
        totalIncome: parseFloat(formData.totalIncome),
        rentValue: formData.paysRent && formData.rentValue.trim() ? parseFloat(formData.rentValue) : undefined,
      }

      if (family) {
        await familyService.update(family.id, payload)
      } else {
        await familyService.create(payload)
      }

      onSuccess()
      onClose()
    } catch (error) {
      setSubmitError(getApiErrorMessage(error, 'Erro ao salvar família. Tente novamente.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth fullScreen={fullScreen}>
      <DialogTitle>
        {family ? 'Editar Família' : 'Nova Família'}
      </DialogTitle>
      <DialogContent>
        {submitError && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {submitError}
          </Alert>
        )}

        <FormSection>
          <Typography variant="h6" gutterBottom>
            Responsável
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Nome completo *"
                value={formData.responsibleName}
                onChange={handleChange('responsibleName')}
                error={!!errors.responsibleName}
                helperText={errors.responsibleName}
                disabled={loading}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="CPF *"
                value={formData.cpf}
                onChange={handleCPFChange}
                error={!!errors.cpf}
                helperText={errors.cpf}
                disabled={loading || Boolean(family)}
                placeholder="000.000.000-00"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Telefone *"
                value={formData.phone}
                onChange={handleChange('phone')}
                error={!!errors.phone}
                helperText={errors.phone || 'Celular ou WhatsApp, com DDD'}
                disabled={loading}
                placeholder="(24) 99999-0000"
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Endereço *"
                value={formData.address}
                onChange={handleChange('address')}
                error={!!errors.address}
                helperText={errors.address || 'Rua, número, bairro e ponto de referência'}
                disabled={loading}
              />
            </Grid>
          </Grid>
        </FormSection>

        <Divider />

        <FormSection>
          <Typography variant="h6" gutterBottom>
            Composição Familiar
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                required
                label="Cônjuge"
                value={formData.spouseName}
                onChange={handleChange('spouseName')}
                error={!!errors.spouseName}
                helperText={errors.spouseName || 'Escreva "não tem" se não houver'}
                disabled={loading}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Número de pessoas *"
                type="number"
                value={formData.householdSize}
                onChange={handleChange('householdSize')}
                error={!!errors.householdSize}
                helperText={errors.householdSize}
                disabled={loading}
                inputProps={{ min: 1 }}
              />
            </Grid>
          </Grid>
        </FormSection>

        <Divider />

        <FormSection>
          <Typography variant="h6" gutterBottom>
            Situação Financeira
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Renda familiar total *"
                type="number"
                value={formData.totalIncome}
                onChange={handleChange('totalIncome')}
                error={!!errors.totalIncome}
                helperText={errors.totalIncome}
                disabled={loading}
                inputProps={{ min: 0, step: 0.01 }}
                InputProps={{
                  startAdornment: <Typography variant="body2">R$</Typography>,
                }}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControlLabel
                control={
                  <Switch checked={formData.paysRent}
                    onChange={(e) => setFormData(prev => ({
                      ...prev,
                      paysRent: e.target.checked,
                      rentValue: e.target.checked ? prev.rentValue : '',
                    }))}
                    disabled={loading} />
                }
                label="Paga aluguel?" />
            </Grid>
            {formData.paysRent && (
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Valor do aluguel (opcional)"
                  type="number"
                  value={formData.rentValue}
                  onChange={handleChange('rentValue')}
                  disabled={loading}
                  inputProps={{ min: 0, step: 0.01 }}
                  helperText="Em branco, usamos a estimativa padrão por tamanho da família"
                  InputProps={{
                    startAdornment: <Typography variant="body2">R$</Typography>,
                  }}
                />
              </Grid>
            )}
          </Grid>
        </FormSection>

        <Divider />

        <FormSection>
          <Typography variant="h6" gutterBottom>
            Solicitação
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth error={!!errors.requestReason} disabled={loading}>
                <InputLabel>Motivo *</InputLabel>
                <Select
                  value={formData.requestReason}
                  onChange={(e) => setFormData(prev => ({ ...prev, requestReason: e.target.value }))}
                  label="Motivo *"
                >
                  <MenuItem value="DESEMPREGO">Desemprego</MenuItem>
                  <MenuItem value="BAIXA_RENDA">Baixa Renda</MenuItem>
                  <MenuItem value="EMERGENCIA_FINANCEIRA">Emergência Financeira</MenuItem>
                  <MenuItem value="DOENCA_FAMILIA">Doença na Família</MenuItem>
                  <MenuItem value="PERDA_RENDA">Perda de Renda</MenuItem>
                  <MenuItem value="SITUACAO_HABITACIONAL">Situação Habitacional</MenuItem>
                  <MenuItem value="FAMILIA_NUMEROSA">Família Numerosa</MenuItem>
                  <MenuItem value="OUTRO">Outro</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth error={!!errors.origin} disabled={loading}>
                <InputLabel>Origem *</InputLabel>
                <Select
                  value={formData.origin}
                  onChange={(e) => setFormData(prev => ({ ...prev, origin: e.target.value }))}
                  label="Origem *"
                >
                  <MenuItem value="CADASTRO_PROPRIO">Cadastro Próprio</MenuItem>
                  <MenuItem value="INDICACAO">Indicação</MenuItem>
                  <MenuItem value="IGREJA">Igreja</MenuItem>
                  <MenuItem value="DIACONO">Diácono</MenuItem>
                  <MenuItem value="PASTOR">Pastor</MenuItem>
                  <MenuItem value="MEMBRO_IGREJA">Membro da Igreja</MenuItem>
                  <MenuItem value="INSTITUICAO_PARCEIRA">Instituição Parceira</MenuItem>
                  <MenuItem value="OUTRO">Outro</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Indicado por"
                value={formData.referrerName}
                onChange={handleChange('referrerName')}
                disabled={loading}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Observações"
                value={formData.observations}
                onChange={handleChange('observations')}
                disabled={loading}
                multiline
                rows={3}
              />
            </Grid>
          </Grid>
        </FormSection>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={loading}>
          Cancelar
        </Button>
        <Button onClick={handleSubmit} variant="contained" disabled={loading}>
          {loading ? <CircularProgress size={20} /> : family ? 'Atualizar' : 'Salvar'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default FamilyForm

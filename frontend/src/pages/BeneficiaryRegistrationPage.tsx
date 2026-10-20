import { FormEvent, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Container,
  FormControl,
  FormControlLabel,
  Grid,
  InputLabel,
  MenuItem,
  Select,
  Switch,
  TextField,
  Typography,
} from '@mui/material'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import { Link as RouterLink } from 'react-router-dom'
import api from '../services/api'
import { getApiErrorMessage } from '../utils/apiError'

const needOptions = [
  { value: 'DESEMPREGO', label: 'Desemprego' },
  { value: 'BAIXA_RENDA', label: 'Baixa renda' },
  { value: 'EMERGENCIA_FINANCEIRA', label: 'Emergência financeira' },
  { value: 'DOENCA_FAMILIA', label: 'Doença na família' },
  { value: 'PERDA_RENDA', label: 'Perda de renda' },
  { value: 'SITUACAO_HABITACIONAL', label: 'Situação habitacional' },
  { value: 'FAMILIA_NUMEROSA', label: 'Família numerosa' },
  { value: 'OUTRO', label: 'Outro' },
]

const normalizeCpf = (value: string) => value.replace(/\D/g, '').slice(0, 11)

function BeneficiaryRegistrationPage() {
  const [responsibleName, setResponsibleName] = useState('')
  const [cpf, setCpf] = useState('')
  const [phone, setPhone] = useState('')
  const [address, setAddress] = useState('')
  const [spouseName, setSpouseName] = useState('')
  const [householdSize, setHouseholdSize] = useState('')
  const [totalIncome, setTotalIncome] = useState('')
  const [paysRent, setPaysRent] = useState(false)
  const [rentValue, setRentValue] = useState('')
  const [requestReason, setRequestReason] = useState('')
  const [observations, setObservations] = useState('')
  const [referrerName, setReferrerName] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [done, setDone] = useState(false)

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      await api.post('/public/beneficiaries', {
        responsibleName: responsibleName.trim(),
        cpf: normalizeCpf(cpf),
        phone: phone.trim(),
        address: address.trim(),
        spouseName: spouseName.trim() || undefined,
        householdSize: Number(householdSize),
        totalIncome: Number(totalIncome.replace(',', '.')),
        paysRent,
        rentValue: paysRent && rentValue.trim() ? Number(rentValue.replace(',', '.')) : undefined,
        requestReason,
        origin: referrerName.trim() ? 'INDICACAO' : 'CADASTRO_PROPRIO',
        referrerName: referrerName.trim() || undefined,
        observations: observations.trim() || undefined,
      })
      setDone(true)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Não foi possível registrar o cadastro. Confira os dados e tente novamente.'))
    } finally {
      setSaving(false)
    }
  }

  if (done) {
    return (
      <Container maxWidth="sm" sx={{ py: 6 }}>
        <Card>
          <CardContent sx={{ textAlign: 'center', py: 5 }}>
            <CheckCircleIcon color="success" sx={{ fontSize: 56, mb: 2 }} />
            <Typography variant="h5" gutterBottom fontWeight={700}>
              Cadastro recebido!
            </Typography>
            <Typography color="text.secondary" sx={{ mb: 3 }}>
              Suas informações já estão registradas no sistema e serão analisadas pela nossa equipe.
              Se precisarmos de mais algum dado, entraremos em contato pelo telefone informado.
            </Typography>
            <Button component={RouterLink} to="/" variant="contained">
              Voltar à página inicial
            </Button>
          </CardContent>
        </Card>
      </Container>
    )
  }

  return (
    <Container maxWidth="md" sx={{ py: { xs: 3, md: 6 } }}>
      <Card>
        <CardContent sx={{ p: { xs: 2, sm: 4 } }}>
          <Typography variant="h5" color="primary" fontWeight={700} gutterBottom>
            Quero ser beneficiado
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Preencha seus dados com atenção. Os campos marcados com * são obrigatórios.
            Cadastro não garante contemplação: cada família passa por análise da equipe.
          </Typography>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <Box component="form" onSubmit={submit}>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={8}>
                <TextField required fullWidth label="Nome completo" value={responsibleName}
                  onChange={(e) => setResponsibleName(e.target.value)} inputProps={{ maxLength: 255 }} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField required fullWidth label="CPF" value={cpf}
                  onChange={(e) => setCpf(normalizeCpf(e.target.value))}
                  helperText="Somente números" inputProps={{ inputMode: 'numeric', maxLength: 11 }}
                  error={Boolean(cpf) && cpf.length !== 11} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField required fullWidth label="Telefone" value={phone}
                  onChange={(e) => setPhone(e.target.value)} inputProps={{ maxLength: 20 }}
                  placeholder="(24) 99999-0000" />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Cônjuge (se houver)" value={spouseName}
                  onChange={(e) => setSpouseName(e.target.value)} inputProps={{ maxLength: 255 }} />
              </Grid>
              <Grid item xs={12}>
                <TextField required fullWidth label="Endereço" value={address}
                  onChange={(e) => setAddress(e.target.value)} inputProps={{ maxLength: 500 }}
                  placeholder="Rua, número, bairro" />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField required fullWidth type="number" label="Pessoas na família" value={householdSize}
                  onChange={(e) => setHouseholdSize(e.target.value)}
                  helperText="Total de pessoas que moram com você" inputProps={{ min: 1, step: 1 }} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField required fullWidth type="number" label="Renda familiar (R$)" value={totalIncome}
                  onChange={(e) => setTotalIncome(e.target.value)}
                  helperText="Se não tiver renda, informe 0" inputProps={{ min: 0, step: '0.01' }} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <FormControlLabel
                  control={<Switch checked={paysRent} onChange={(e) => setPaysRent(e.target.checked)} />}
                  label="Paga aluguel?" sx={{ mt: 1 }} />
                {paysRent && (
                  <TextField fullWidth type="number" label="Valor do aluguel (R$, opcional)" value={rentValue}
                    onChange={(e) => setRentValue(e.target.value)}
                    helperText="Se não souber informar, deixe em branco" inputProps={{ min: 0, step: '0.01' }}
                    sx={{ mt: 1 }} />
                )}
              </Grid>
              <Grid item xs={12} sm={4}>
                <FormControl fullWidth required>
                  <InputLabel>Maior necessidade</InputLabel>
                  <Select value={requestReason} label="Maior necessidade"
                    onChange={(e) => setRequestReason(e.target.value)}>
                    {needOptions.map((option) => (
                      <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Indicado por grupo/apoio (se houver)" value={referrerName}
                  onChange={(e) => setReferrerName(e.target.value)} inputProps={{ maxLength: 255 }}
                  helperText="Quem indicou você? Ex.: grupo de mães, instituição" />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth multiline minRows={2} label="Observação" value={observations}
                  onChange={(e) => setObservations(e.target.value)} inputProps={{ maxLength: 2000 }} />
              </Grid>
              <Grid item xs={12}>
                <Button type="submit" variant="contained" size="large" fullWidth disabled={saving}>
                  {saving ? 'Enviando...' : 'Enviar cadastro'}
                </Button>
              </Grid>
            </Grid>
          </Box>
          <Typography variant="body2" sx={{ mt: 3 }}>
            <RouterLink to="/">← Voltar à página inicial</RouterLink>
          </Typography>
        </CardContent>
      </Card>
    </Container>
  )
}

export default BeneficiaryRegistrationPage

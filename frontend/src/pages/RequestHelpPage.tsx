import { FormEvent, useEffect, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Chip,
  Container,
  FormControl,
  FormControlLabel,
  FormHelperText,
  InputLabel,
  MenuItem,
  Paper,
  Radio,
  RadioGroup,
  Select,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { Link as RouterLink, useNavigate } from 'react-router-dom'
import serviceRequestService, {
  ServiceRequestAssistanceType,
  ServiceRequestContactPreference,
  WelcomeSlot,
} from '../services/serviceRequestService'
import PreviewBanner from '../preview/PreviewBanner'
import api from '../services/api'

type RequestForm = {
  fullName: string
  contactValue: string
  contactPreference: ServiceRequestContactPreference
  location: string
  householdSize: string
  assistanceType: ServiceRequestAssistanceType | ''
  message: string
  consentAccepted: boolean
  welcomeChoice: 'NONE' | 'VISIT' | 'PHONE'
  welcomeDate: string
  welcomeTime: string
}

const initialForm: RequestForm = {
  fullName: '',
  contactValue: '',
  contactPreference: 'PHONE',
  location: '',
  householdSize: '',
  assistanceType: '',
  message: '',
  consentAccepted: false,
  welcomeChoice: 'NONE',
  welcomeDate: '',
  welcomeTime: '',
}

function isAssistanceType(value: string): value is ServiceRequestAssistanceType {
  return value === 'FOOD' || value === 'HYGIENE' || value === 'OTHER'
}

interface ServiceRequestLookupResult {
  status: string
  createdAt: string
  updatedAt: string
  contactMasked: string
}

interface RequestHelpPageProps {
  demoMode?: boolean
}

function RequestHelpPage({ demoMode = false }: RequestHelpPageProps) {
  const navigate = useNavigate()
  const [form, setForm] = useState<RequestForm>(initialForm)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [loading, setLoading] = useState(false)
  const [protocol, setProtocol] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [lookupMode, setLookupMode] = useState(false)
  const [lookupProtocol, setLookupProtocol] = useState('')
  const [lookupContact, setLookupContact] = useState('')
  const [lookupResult, setLookupResult] = useState<ServiceRequestLookupResult | null>(null)
  const [lookupError, setLookupError] = useState('')
  const [lookupLoading, setLookupLoading] = useState(false)
  const [welcomeSlots, setWelcomeSlots] = useState<WelcomeSlot[]>([])
  const [submittedWelcome, setSubmittedWelcome] = useState<{ date: string; time: string; byPhone: boolean } | null>(null)

  useEffect(() => {
    if (demoMode) return
    serviceRequestService.welcomeSlots()
      .then(setWelcomeSlots)
      .catch(() => setWelcomeSlots([]))
  }, [demoMode])

  const lookupRequest = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!lookupProtocol.trim() || !lookupContact.trim() || lookupLoading) return
    setLookupLoading(true)
    setLookupError('')
    setLookupResult(null)
    try {
      const response = await api.post<ServiceRequestLookupResult>('/public/service-requests/lookup', {
        protocol: lookupProtocol.trim(),
        contactValue: lookupContact.trim(),
      })
      setLookupResult(response.data)
    } catch {
      setLookupError('Não foi possível consultar o pedido. Confira os dados ou tente novamente mais tarde.')
    } finally {
      setLookupLoading(false)
    }
  }

  const updateForm = (field: keyof RequestForm, value: string | boolean) => {
    setForm((current) => ({ ...current, [field]: value }))
    setErrors((current) => ({ ...current, [field]: '' }))
    setErrorMessage('')
  }

  const validate = () => {
    const next: Record<string, string> = {}
    if (!form.fullName.trim()) next.fullName = 'Informe seu nome ou identificação.'
    else if (form.fullName.length > 255) next.fullName = 'Use até 255 caracteres.'
    if (!form.contactValue.trim()) {
      next.contactValue = 'Informe um contato para retorno.'
    } else if (form.contactValue.length > 254) {
      next.contactValue = 'Use até 254 caracteres para o contato.'
    } else if (form.contactPreference === 'EMAIL') {
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(form.contactValue.trim())) {
        next.contactValue = 'Informe um e-mail válido.'
      }
    } else {
      const digits = form.contactValue.replace(/\D/g, '')
      if (digits.length < 10 || digits.length > 13) {
        next.contactValue = 'Informe um telefone válido com DDD.'
      }
    }
    if (!form.location.trim()) next.location = 'Informe sua cidade ou bairro.'
    else if (form.location.length > 255) next.location = 'Use até 255 caracteres.'
    const people = Number(form.householdSize)
    if (!Number.isInteger(people) || people <= 0) {
      next.householdSize = 'Informe uma quantidade inteira maior que zero.'
    }
    if (!isAssistanceType(form.assistanceType)) next.assistanceType = 'Selecione o tipo de ajuda.'
    if (form.message.length > 1000) next.message = 'A mensagem deve ter até 1000 caracteres.'
    if (!form.consentAccepted) next.consentAccepted = 'É necessário aceitar o uso dos dados para contato.'
    if (form.welcomeChoice === 'VISIT' && !form.welcomeDate) {
      next.welcomeDate = 'Escolha um dos dias disponíveis para conversar.'
    }
    setErrors(next)
    return Object.keys(next).length === 0
  }

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!validate() || loading) return
    const assistanceType = form.assistanceType
    if (!isAssistanceType(assistanceType)) {
      setErrors((current) => ({
        ...current,
        assistanceType: 'Selecione o tipo de ajuda.',
      }))
      return
    }
    setLoading(true)
    setErrorMessage('')

    if (demoMode) {
      await new Promise((resolve) => window.setTimeout(resolve, 350))
      setProtocol('DEMO-SEM-REGISTRO')
      setLoading(false)
      return
    }

    try {
      const response = await serviceRequestService.create({
        fullName: form.fullName.trim(),
        contactValue: form.contactValue.trim(),
        contactPreference: form.contactPreference,
        location: form.location.trim(),
        householdSize: Number(form.householdSize),
        assistanceType,
        message: form.message.trim(),
        consentAccepted: form.consentAccepted,
        ...(form.welcomeChoice === 'VISIT' && form.welcomeDate
          ? {
              welcomePreference: 'VISIT' as const,
              welcomeDate: form.welcomeDate,
              ...(form.welcomeTime ? { welcomeTime: form.welcomeTime } : {}),
            }
          : form.welcomeChoice === 'PHONE'
            ? { welcomePreference: 'PHONE' as const }
            : {}),
      })
      setProtocol(response.protocol)
      setSubmittedWelcome(
        form.welcomeChoice === 'VISIT'
          ? { date: form.welcomeDate, time: form.welcomeTime, byPhone: false }
          : form.welcomeChoice === 'PHONE'
            ? { date: '', time: '', byPhone: true }
            : null,
      )
    } catch {
      setErrorMessage('Não foi possível enviar seu pedido agora. Tente novamente mais tarde.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      {demoMode && <PreviewBanner />}
      <Box sx={{ minHeight: '100vh', bgcolor: 'background.default', py: { xs: 3, sm: 5 } }}>
        <Container maxWidth="sm">
          <Button component={RouterLink} to={demoMode ? '/preview' : '/'} sx={{ mb: 2 }}>
            ← Voltar à página inicial
          </Button>
          <Paper elevation={2} sx={{ p: { xs: 2.5, sm: 4 }, borderRadius: 3 }}>
            {protocol ? (
              <Stack spacing={2.5}>
                <Typography variant="h4" component="h1" color="primary" fontWeight={800}>
                  Cadastro recebido!
                </Typography>
                {demoMode && (
                  <Alert severity="warning">
                    MODO DEMONSTRAÇÃO — DADOS FICTÍCIOS. Este envio não foi gravado.
                  </Alert>
                )}
                <Typography>
                  Pronto! Suas informações já estão registradas no sistema do Mercado Solidário
                  e a nossa equipe vai analisar com carinho.
                </Typography>
                {submittedWelcome?.byPhone && (
                  <Alert severity="info">
                    Você escolheu conversar por telefone. Vamos te ligar no número informado.
                  </Alert>
                )}
                {submittedWelcome && !submittedWelcome.byPhone && submittedWelcome.date && (
                  <Alert severity="info">
                    Você escolheu conversar pessoalmente no dia{' '}
                    <strong>{new Date(submittedWelcome.date + 'T12:00:00').toLocaleDateString('pt-BR')}</strong>
                    {submittedWelcome.time && <> por volta das <strong>{submittedWelcome.time}</strong></>}.
                    Esperamos você!
                  </Alert>
                )}
                <Alert severity="info">
                  {demoMode
                    ? 'Código demonstrativo (não é válido para acompanhamento):'
                    : 'Anote este código se quiser acompanhar seu pedido depois:'}{' '}
                  <strong>{protocol}</strong>
                </Alert>
                <Typography variant="body2" color="text.secondary">
                  O envio do pedido não representa aprovação automática de benefício. A equipe do
                  Mercado Solidário analisará as informações e poderá entrar em contato.
                </Typography>
                <Button variant="contained" onClick={() => navigate(demoMode ? '/preview' : '/')}>
                  Voltar à página inicial
                </Button>
              </Stack>
            ) : (
              <Stack component="form" onSubmit={lookupMode ? lookupRequest : submit} spacing={2.5} noValidate>
                {!demoMode && (
                  <Button
                    type="button"
                    variant="text"
                    onClick={() => {
                      setLookupMode((current) => !current)
                      setLookupResult(null)
                      setLookupError('')
                    }}
                    sx={{ alignSelf: 'flex-start', px: 0 }}
                  >
                    {lookupMode ? '← Voltar ao pedido de atendimento' : 'Já enviei meu pedido: acompanhar'}
                  </Button>
                )}
                <Box>
                  <Typography variant="h4" component="h1" color="primary" fontWeight={800}>
                    {lookupMode ? 'Consultar pedido' : 'Solicitar atendimento'}
                  </Typography>
                  <Typography color="text.secondary" sx={{ mt: 1 }}>
                    {lookupMode
                      ? 'Informe o código de acompanhamento e o mesmo telefone ou e-mail fornecido no pedido. A consulta não revela dados pessoais ou observações internas.'
                      : 'O Mercado Solidário organiza o apoio a famílias com cuidado, respeito e transparência. Compartilhe apenas as informações necessárias para que a equipe possa analisar seu pedido.'}
                  </Typography>
                </Box>

                {demoMode && (
                  <Alert severity="warning">
                    MODO DEMONSTRAÇÃO — DADOS FICTÍCIOS. O formulário não envia nem grava
                    informações.
                  </Alert>
                )}
                {lookupMode ? (
                  <Stack spacing={2.5}>
                    <TextField
                      label="Código de acompanhamento"
                      value={lookupProtocol}
                      onChange={(event) => setLookupProtocol(event.target.value)}
                      required
                      fullWidth
                      autoComplete="off"
                    />
                    <TextField
                      label="Telefone ou e-mail informado no pedido"
                      value={lookupContact}
                      onChange={(event) => setLookupContact(event.target.value)}
                      required
                      fullWidth
                      autoComplete="off"
                    />
                    {lookupError && <Alert severity="error">{lookupError}</Alert>}
                    {lookupResult && (
                      <Alert severity="success">
                        Status: <strong>{lookupResult.status.replace(/_/g, ' ')}</strong>
                        <br />
                        Recebido: {new Date(lookupResult.createdAt).toLocaleString('pt-BR')}
                        <br />
                        Última atualização: {new Date(lookupResult.updatedAt).toLocaleString('pt-BR')}
                        <br />
                        Contato de retorno: {lookupResult.contactMasked}
                      </Alert>
                    )}
                    <Button type="submit" variant="contained" size="large" disabled={lookupLoading}>
                      {lookupLoading ? 'Consultando…' : 'Consultar pedido'}
                    </Button>
                  </Stack>
                ) : (
                  <>
                    <Alert severity="info">
                      O envio do pedido não representa aprovação automática de benefício. A equipe do
                      Mercado Solidário analisará as informações e poderá entrar em contato.
                    </Alert>
                    {errorMessage && <Alert severity="error">{errorMessage}</Alert>}

                <TextField
                  label="Nome ou identificação de contato"
                  value={form.fullName}
                  onChange={(event) => updateForm('fullName', event.target.value)}
                  error={Boolean(errors.fullName)}
                  helperText={errors.fullName}
                  required
                  autoComplete="name"
                  inputProps={{ maxLength: 255 }}
                  fullWidth
                />
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                  <FormControl fullWidth>
                    <InputLabel id="contact-preference-label">Preferência de contato</InputLabel>
                    <Select
                      labelId="contact-preference-label"
                      label="Preferência de contato"
                      value={form.contactPreference}
                      onChange={(event) =>
                        updateForm('contactPreference', event.target.value as ServiceRequestContactPreference)
                      }
                    >
                      <MenuItem value="PHONE">Telefone</MenuItem>
                      <MenuItem value="EMAIL">E-mail</MenuItem>
                    </Select>
                  </FormControl>
                  <TextField
                    label={form.contactPreference === 'EMAIL' ? 'E-mail' : 'Telefone com DDD'}
                    value={form.contactValue}
                    onChange={(event) => updateForm('contactValue', event.target.value)}
                    error={Boolean(errors.contactValue)}
                    helperText={errors.contactValue}
                    required
                    type={form.contactPreference === 'EMAIL' ? 'email' : 'tel'}
                    autoComplete={form.contactPreference === 'EMAIL' ? 'email' : 'tel'}
                    inputProps={{ maxLength: 254 }}
                    fullWidth
                  />
                </Stack>
                <TextField
                  label="Cidade ou bairro"
                  value={form.location}
                  onChange={(event) => updateForm('location', event.target.value)}
                  error={Boolean(errors.location)}
                  helperText={errors.location}
                  required
                  inputProps={{ maxLength: 255 }}
                  fullWidth
                />
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                  <TextField
                    label="Pessoas na residência"
                    type="number"
                    value={form.householdSize}
                    onChange={(event) => updateForm('householdSize', event.target.value)}
                    error={Boolean(errors.householdSize)}
                    helperText={errors.householdSize}
                    required
                    inputProps={{ min: 1, step: 1 }}
                    fullWidth
                  />
                  <FormControl fullWidth error={Boolean(errors.assistanceType)} required>
                    <InputLabel id="assistance-type-label">Tipo de ajuda</InputLabel>
                    <Select
                      labelId="assistance-type-label"
                      label="Tipo de ajuda"
                      value={form.assistanceType}
                      onChange={(event) => updateForm('assistanceType', event.target.value)}
                    >
                      <MenuItem value="FOOD">Alimentos</MenuItem>
                      <MenuItem value="HYGIENE">Itens de higiene</MenuItem>
                      <MenuItem value="OTHER">Outro tipo de apoio</MenuItem>
                    </Select>
                    {errors.assistanceType && <FormHelperText>{errors.assistanceType}</FormHelperText>}
                  </FormControl>
                </Stack>
                <TextField
                  label="Mensagem (opcional)"
                  value={form.message}
                  onChange={(event) => updateForm('message', event.target.value)}
                  error={Boolean(errors.message)}
                  helperText={errors.message || `${form.message.length}/1000 caracteres`}
                  multiline
                  minRows={3}
                  inputProps={{ maxLength: 1000 }}
                  fullWidth
                />
                <Box>
                  <Typography variant="subtitle1" fontWeight={700} gutterBottom>
                    Quer conversar com a nossa equipe? (opcional)
                  </Typography>
                  <RadioGroup
                    value={form.welcomeChoice}
                    onChange={(event) =>
                      updateForm('welcomeChoice', event.target.value as RequestForm['welcomeChoice'])
                    }
                  >
                    <FormControlLabel
                      value="NONE"
                      control={<Radio />}
                      label="Só registrar o pedido por enquanto"
                    />
                    {welcomeSlots.length > 0 && (
                      <FormControlLabel
                        value="VISIT"
                        control={<Radio />}
                        label="Quero conversar pessoalmente — escolher um dia"
                      />
                    )}
                    <FormControlLabel
                      value="PHONE"
                      control={<Radio />}
                      label="Prefiro que me liguem no telefone informado"
                    />
                  </RadioGroup>
                  {form.welcomeChoice === 'VISIT' && welcomeSlots.length > 0 && (
                    <Stack spacing={1.5} sx={{ mt: 1 }}>
                      <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap">
                        {welcomeSlots.map((slot) => (
                          <Chip
                            key={slot.id}
                            clickable
                            color={form.welcomeDate === slot.availableDate ? 'primary' : 'default'}
                            variant={form.welcomeDate === slot.availableDate ? 'filled' : 'outlined'}
                            onClick={() => updateForm('welcomeDate', slot.availableDate)}
                            label={
                              new Date(slot.availableDate + 'T12:00:00').toLocaleDateString('pt-BR', {
                                weekday: 'short', day: '2-digit', month: '2-digit',
                              }) + (slot.note ? ` — ${slot.note}` : '')
                            }
                          />
                        ))}
                      </Stack>
                      <TextField
                        label="Horário estimado (ex.: 14:30)"
                        type="time"
                        value={form.welcomeTime}
                        onChange={(event) => updateForm('welcomeTime', event.target.value)}
                        InputLabelProps={{ shrink: true }}
                        sx={{ maxWidth: 220 }}
                      />
                      {errors.welcomeDate && (
                        <FormHelperText error>{errors.welcomeDate}</FormHelperText>
                      )}
                    </Stack>
                  )}
                  {form.welcomeChoice === 'PHONE' && (
                    <FormHelperText sx={{ ml: 4 }}>
                      Vamos te ligar no telefone informado acima para conversar.
                    </FormHelperText>
                  )}
                </Box>
                <FormControl error={Boolean(errors.consentAccepted)}>
                  <FormControlLabel
                    control={
                      <input
                        type="checkbox"
                        checked={form.consentAccepted}
                        onChange={(event) => updateForm('consentAccepted', event.target.checked)}
                        aria-label="Aceito o uso destes dados para contato"
                        style={{ width: 18, height: 18, margin: '0 10px 0 8px', accentColor: '#183B56' }}
                      />
                    }
                    label="Aceito o uso destas informações para análise deste pedido e contato pela equipe."
                  />
                  {errors.consentAccepted && <FormHelperText>{errors.consentAccepted}</FormHelperText>}
                  <FormHelperText>
                    Privacidade: não informe senha, dados bancários, chave PIX, CPF ou dados
                    detalhados dos membros da família. Usaremos as informações somente para analisar
                    o pedido e possibilitar contato.
                  </FormHelperText>
                </FormControl>
                <Button type="submit" variant="contained" size="large" disabled={loading}>
                  {loading ? 'Enviando pedido…' : 'Enviar pedido de atendimento'}
                </Button>
                  </>
                )}
              </Stack>
            )}
          </Paper>
        </Container>
      </Box>
    </>
  )
}

export default RequestHelpPage

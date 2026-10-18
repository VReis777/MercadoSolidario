import { FormEvent, useEffect, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Container,
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  Select,
  TextField,
  Typography,
} from '@mui/material'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import InstagramIcon from '@mui/icons-material/Instagram'
import WhatsAppIcon from '@mui/icons-material/WhatsApp'
import VolunteerActivismIcon from '@mui/icons-material/VolunteerActivism'
import { Link as RouterLink } from 'react-router-dom'
import api from '../services/api'
import { getApiErrorMessage } from '../utils/apiError'

const interestOptions = [
  { value: 'PATROCINAR', label: 'Quero patrocinar uma ação' },
  { value: 'DOAR_MAIS', label: 'Quero doar valores ou produtos' },
  { value: 'SER_PARCEIRO', label: 'Quero ser parceiro do projeto' },
]

interface SiteInfo {
  instagramUrl?: string
  whatsappGroupUrl?: string
}

function CompanyPage() {
  const [siteInfo, setSiteInfo] = useState<SiteInfo>({})
  const [responsibleName, setResponsibleName] = useState('')
  const [companyName, setCompanyName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [whatsapp, setWhatsapp] = useState('')
  const [interest, setInterest] = useState('')
  const [message, setMessage] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [done, setDone] = useState(false)
  const [emailSent, setEmailSent] = useState(false)

  useEffect(() => {
    api.get<SiteInfo>('/public/site-info')
      .then((response) => setSiteInfo(response.data))
      .catch(() => undefined)
  }, [])

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      const response = await api.post('/public/companies', {
        responsibleName: responsibleName.trim(),
        companyName: companyName.trim(),
        email: email.trim(),
        phone: phone.trim() || undefined,
        whatsapp: whatsapp.trim() || undefined,
        interest,
        message: message.trim() || undefined,
      })
      setEmailSent(Boolean(response.data?.emailSent))
      setDone(true)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Não foi possível enviar. Confira os dados e tente novamente.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Container maxWidth="md" sx={{ py: { xs: 3, md: 6 } }}>
      <Card sx={{ mb: 3 }}>
        <CardContent sx={{ p: { xs: 2, sm: 4 } }}>
          <Typography variant="h4" component="h1" color="primary" fontWeight={800} gutterBottom>
            Sua empresa pode transformar o Natal de uma família
          </Typography>
          <Typography color="text.secondary" sx={{ mb: 2 }}>
            O Mercado Solidário é um projeto social que distribui alimentos e itens de cuidado com dignidade.
            Sua empresa pode participar de três formas:
          </Typography>
          <Grid container spacing={2} sx={{ mb: 3 }}>
            {interestOptions.map((option) => (
              <Grid item xs={12} sm={4} key={option.value}>
                <Card variant="outlined" sx={{ height: '100%' }}>
                  <CardContent>
                    <Typography fontWeight={700} variant="body1">{option.label}</Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 1 }}>
            <Button variant="contained" color="secondary" startIcon={<VolunteerActivismIcon />}
              component={RouterLink} to="/doacao">
              Doar agora pelo PIX
            </Button>
            <Button
              variant="outlined"
              startIcon={<WhatsAppIcon />}
              component="a"
              href={siteInfo.whatsappGroupUrl || undefined}
              target="_blank"
              rel="noopener noreferrer"
              disabled={!siteInfo.whatsappGroupUrl}
            >
              {siteInfo.whatsappGroupUrl ? 'Entrar no grupo do WhatsApp' : 'Grupo de WhatsApp em breve'}
            </Button>
            <Button
              variant="text"
              startIcon={<InstagramIcon />}
              component="a"
              href={siteInfo.instagramUrl || 'https://www.instagram.com/mercadosolidariopetropolis'}
              target="_blank"
              rel="noopener noreferrer"
            >
              Instagram
            </Button>
          </Box>
        </CardContent>
      </Card>

      <Card>
        <CardContent sx={{ p: { xs: 2, sm: 4 } }}>
          {done ? (
            <Box sx={{ textAlign: 'center', py: 4 }}>
              <CheckCircleIcon color="success" sx={{ fontSize: 56, mb: 2 }} />
              <Typography variant="h5" fontWeight={700} gutterBottom>Contato recebido!</Typography>
              <Typography color="text.secondary" sx={{ mb: 1 }}>
                Obrigado pelo interesse, {responsibleName.split(' ')[0]}. A {companyName} já está na nossa lista de empresas.
              </Typography>
              <Typography color="text.secondary" sx={{ mb: 3 }}>
                {emailSent
                  ? 'Nossa equipe foi avisada por e-mail e vai retornar o contato em breve.'
                  : 'Nossa equipe vai retornar o contato em breve.'}
              </Typography>
              <Button component={RouterLink} to="/" variant="contained">Voltar à página inicial</Button>
            </Box>
          ) : (
            <>
              <Typography variant="h5" color="primary" fontWeight={700} gutterBottom>
                Quero que a equipe entre em contato
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Deixe seus dados e a nossa equipe retorna pelo e-mail ou telefone informado.
              </Typography>
              {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
              <Box component="form" onSubmit={submit}>
                <Grid container spacing={2}>
                  <Grid item xs={12} sm={6}>
                    <TextField required fullWidth label="Nome da empresa" value={companyName}
                      onChange={(e) => setCompanyName(e.target.value)} inputProps={{ maxLength: 255 }} />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField required fullWidth label="Responsável" value={responsibleName}
                      onChange={(e) => setResponsibleName(e.target.value)} inputProps={{ maxLength: 255 }} />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField required fullWidth type="email" label="E-mail de contato" value={email}
                      onChange={(e) => setEmail(e.target.value)} inputProps={{ maxLength: 255 }} />
                  </Grid>
                  <Grid item xs={6} sm={3}>
                    <TextField fullWidth label="Telefone" value={phone}
                      onChange={(e) => setPhone(e.target.value)} inputProps={{ maxLength: 20 }} />
                  </Grid>
                  <Grid item xs={6} sm={3}>
                    <TextField fullWidth label="WhatsApp" value={whatsapp}
                      onChange={(e) => setWhatsapp(e.target.value)} inputProps={{ maxLength: 20 }} />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <FormControl fullWidth required>
                      <InputLabel>Como a empresa quer participar</InputLabel>
                      <Select value={interest} label="Como a empresa quer participar"
                        onChange={(e) => setInterest(e.target.value)}>
                        {interestOptions.map((option) => (
                          <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>
                        ))}
                      </Select>
                    </FormControl>
                  </Grid>
                  <Grid item xs={12}>
                    <TextField fullWidth multiline minRows={3} label="Mensagem (opcional)" value={message}
                      onChange={(e) => setMessage(e.target.value)} inputProps={{ maxLength: 2000 }} />
                  </Grid>
                  <Grid item xs={12}>
                    <Button type="submit" variant="contained" size="large" disabled={saving}>
                      {saving ? 'Enviando...' : 'Enviar contato para a equipe'}
                    </Button>
                  </Grid>
                </Grid>
              </Box>
            </>
          )}
          <Typography variant="body2" sx={{ mt: 3 }}>
            <RouterLink to="/">← Voltar à página inicial</RouterLink>
          </Typography>
        </CardContent>
      </Card>
    </Container>
  )
}

export default CompanyPage

import { FormEvent, useEffect, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Checkbox,
  Container,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import donationService, { PublicDonationResult } from '../services/donationService'
import { getApiErrorMessage } from '../utils/apiError'
import { newRequestReference } from '../utils/idempotency'

function PublicDonationPage() {
  const [pixKey, setPixKey] = useState('msolidario@hotmail.com')
  const [amount, setAmount] = useState('20.00')
  const [donorName, setDonorName] = useState('')
  const [anonymous, setAnonymous] = useState(false)
  const [requestReference, setRequestReference] = useState(() => newRequestReference())
  const [result, setResult] = useState<PublicDonationResult | null>(null)
  const [error, setError] = useState('')
  const [copied, setCopied] = useState<'key' | 'payload' | null>(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    donationService.getPublicPixInfo()
      .then((info) => {
        if (info.configured && info.pixKey) setPixKey(info.pixKey)
      })
      .catch(() => undefined)
  }, [])

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      const response = await donationService.createFinancial({
        amount: Number(amount),
        donorName: anonymous ? undefined : donorName.trim() || undefined,
        anonymous,
        externalReference: requestReference,
      })
      setResult(response)
      setCopied(null)
    } catch (submitError) {
      setError(getApiErrorMessage(submitError, 'Não foi possível registrar a doação.'))
    } finally {
      setSaving(false)
    }
  }

  const startNewDonation = () => {
    setAmount('20.00')
    setDonorName('')
    setAnonymous(false)
    setResult(null)
    setError('')
    setRequestReference(newRequestReference())
  }

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default', pb: 5 }}>
      <Container maxWidth="sm" sx={{ mt: { xs: 2, sm: 5 } }}>
        <Card>
          <CardContent sx={{ p: { xs: 2, sm: 4 } }}>
            <Typography variant="h4" component="h1" color="primary" fontWeight={800} gutterBottom>
              Faça uma doação
            </Typography>
            <Typography color="text.secondary" sx={{ mb: 3 }}>
              Sua contribuição apoia o trabalho do Mercado Solidário. Os dados desta página não dão acesso a informações de famílias ou beneficiários.
            </Typography>
            <Alert severity="info" sx={{ mb: 2 }}>
              Prefere doar direto pela chave? Nosso PIX é <strong>{pixKey}</strong> (e-mail).
            </Alert>
            <Button
              variant="outlined"
              size="small"
              sx={{ mb: 3 }}
              onClick={async () => {
                try {
                  await navigator.clipboard.writeText(pixKey)
                  setCopied('key')
                } catch {
                  setError(`Não foi possível copiar. Anote a chave: ${pixKey}`)
                }
              }}
            >
              {copied === 'key' ? 'Chave PIX copiada!' : 'Copiar chave PIX'}
            </Button>
            {!result ? (
              <Box component="form" onSubmit={submit}>
                {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
                <Stack spacing={2}>
                  <Stack direction="row" gap={1} flexWrap="wrap">
                    {[1, 2, 5, 10, 20, 50, 100].map((suggestion) => (
                      <Button
                        key={suggestion}
                        type="button"
                        variant={Number(amount) === suggestion ? 'contained' : 'outlined'}
                        size="small"
                        onClick={() => setAmount(suggestion.toFixed(2))}
                      >
                        R$ {suggestion.toLocaleString('pt-BR')},00
                      </Button>
                    ))}
                  </Stack>
                  <TextField
                    required
                    fullWidth
                    type="number"
                    label="Outro valor (R$)"
                    value={amount}
                    onChange={(event) => setAmount(event.target.value)}
                    inputProps={{ min: 0.01, step: 0.01 }}
                  />
                  <TextField
                    fullWidth
                    label="Nome (opcional)"
                    value={donorName}
                    disabled={anonymous}
                    onChange={(event) => setDonorName(event.target.value)}
                    inputProps={{ maxLength: 255 }}
                  />
                  <Box sx={{ display: 'flex', alignItems: 'center', ml: -1 }}>
                    <Checkbox checked={anonymous} onChange={(event) => setAnonymous(event.target.checked)} />
                    <Typography>Quero doar anonimamente</Typography>
                  </Box>
                  <Button type="submit" variant="contained" size="large" disabled={saving}>
                    {saving ? 'Registrando...' : 'Continuar'}
                  </Button>
                </Stack>
              </Box>
            ) : (
              <Stack spacing={2}>
                <Alert severity="info">
                  Doação #{result.id} — status: <strong>{result.status}</strong>. Não há confirmação de pagamento neste registro.
                </Alert>
                <Typography variant="h5" fontWeight={700}>Valor: R$ {result.amount.toFixed(2)}</Typography>
                <Alert severity={result.pixStatus === 'PIX_NAO_CONFIGURADO' ? 'warning' : 'info'}>
                  {result.message}
                </Alert>
                {result.pixPayload && (
                  <Box sx={{ textAlign: 'center' }}>
                    {result.qrCodeDataUri && (
                      <Box
                        component="img"
                        src={result.qrCodeDataUri}
                        alt="QR Code PIX. A geração não confirma o pagamento."
                        sx={{ width: 240, height: 240, maxWidth: '100%', border: '1px solid', borderColor: 'divider', p: 1, bgcolor: 'white' }}
                      />
                    )}
                  </Box>
                )}
                {result.pixPayload && (
                  <TextField
                    fullWidth
                    multiline
                    minRows={4}
                    label="PIX copia e cola"
                    value={result.pixPayload}
                    InputProps={{ readOnly: true }}
                    onFocus={(event) => event.target.select()}
                  />
                )}
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                  <Button variant="outlined" onClick={async () => {
                    try {
                      await navigator.clipboard.writeText(pixKey)
                      setCopied('key')
                    } catch {
                      setError(`Não foi possível copiar. Anote a chave: ${pixKey}`)
                    }
                  }}>
                    {copied === 'key' ? 'Chave PIX copiada!' : 'Copiar chave PIX (e-mail)'}
                  </Button>
                </Stack>
                {result.pixPayload && (
                  <Button onClick={async () => {
                    if (!navigator.clipboard) {
                      setError('Seu navegador não permite copiar automaticamente. Selecione e copie o código PIX manualmente.')
                      return
                    }
                    try {
                      await navigator.clipboard.writeText(result.pixPayload || '')
                      setCopied('payload')
                      setError('')
                    } catch (copyError) {
                      if (copyError instanceof DOMException) {
                        setError('Não foi possível copiar automaticamente. Selecione e copie o código PIX manualmente.')
                        return
                      }
                      throw copyError
                    }
                  }}>
                    {copied === 'payload' ? 'Código copiado!' : 'Copiar código PIX (copia e cola)'}
                  </Button>
                )}
                {error && <Alert severity="warning">{error}</Alert>}
                <Typography variant="body2" color="text.secondary">
                  Após realizar o pagamento, a confirmação poderá depender de validação da equipe responsável. A geração do código não significa que o pagamento foi recebido.
                </Typography>
                <Button variant="outlined" onClick={startNewDonation}>Registrar outra doação</Button>
              </Stack>
            )}
            <Box sx={{ mt: 3, textAlign: 'center' }}>
              <Button
                component="a"
                href="https://www.instagram.com/mercadosolidariopetropolis"
                target="_blank"
                rel="noopener noreferrer"
                variant="text"
              >
                Siga @mercadosolidariopetropolis no Instagram
              </Button>
            </Box>
          </CardContent>
        </Card>
      </Container>
    </Box>
  )
}

export default PublicDonationPage

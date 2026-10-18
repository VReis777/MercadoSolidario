import { useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  TextField,
  useMediaQuery,
} from '@mui/material'
import { FamilyMember, FamilyMemberRequest } from '../types/family'
import { formatCpfInput } from '../utils/cpf'

interface FamilyMemberFormProps {
  open: boolean
  member?: FamilyMember | null
  onClose: () => void
  onSubmit: (data: FamilyMemberRequest) => Promise<void>
  loading: boolean
  error: string
}

function FamilyMemberForm({ open, member, onClose, onSubmit, loading, error }: FamilyMemberFormProps) {
  const fullScreen = useMediaQuery('(max-width:599px)')
  const [name, setName] = useState('')
  const [cpf, setCpf] = useState('')
  const [birthDate, setBirthDate] = useState('')
  const [relationship, setRelationship] = useState('')
  const [observations, setObservations] = useState('')
  const [nameError, setNameError] = useState('')

  useEffect(() => {
    if (open) {
      setName(member?.name || '')
      setCpf(member?.cpf || '')
      setBirthDate(member?.birthDate || '')
      setRelationship(member?.relationship || '')
      setObservations(member?.observations || '')
      setNameError('')
    }
  }, [open, member])

  const handleSubmit = async () => {
    if (!name.trim()) {
      setNameError('Nome do membro é obrigatório')
      return
    }
    const payload: FamilyMemberRequest = {
      name: name.trim(),
      relationship: relationship.trim() || undefined,
      observations: observations.trim() || undefined,
      birthDate: birthDate || undefined,
      cpf: cpf.replace(/\D/g, '') || undefined,
    }
    try {
      await onSubmit(payload)
      setName('')
      setCpf('')
      setBirthDate('')
      setRelationship('')
      setObservations('')
      setNameError('')
    } catch {
      // The parent exposes the API error in this form.
    }
  }

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm" fullScreen={fullScreen}>
      <DialogTitle>{member ? 'Editar membro' : 'Adicionar membro'}</DialogTitle>
      <DialogContent>
        {error && <Alert severity="error" sx={{ mt: 1 }}>{error}</Alert>}
        <Grid container spacing={2} sx={{ mt: 0.5 }}>
          <Grid item xs={12}>
            <TextField
              fullWidth
              label="Nome *"
              value={name}
              onChange={(e) => setName(e.target.value)}
              error={!!nameError}
              helperText={nameError}
              disabled={loading}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              fullWidth
              label="CPF"
              value={cpf}
              onChange={(e) => setCpf(formatCpfInput(e.target.value))}
              disabled={loading}
              placeholder="000.000.000-00"
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              fullWidth
              label="Data de nascimento"
              type="date"
              value={birthDate}
              onChange={(e) => setBirthDate(e.target.value)}
              disabled={loading}
              InputLabelProps={{ shrink: true }}
            />
          </Grid>
          <Grid item xs={12}>
            <TextField
              fullWidth
              label="Parentesco"
              value={relationship}
              onChange={(e) => setRelationship(e.target.value)}
              disabled={loading}
            />
          </Grid>
          <Grid item xs={12}>
            <TextField
              fullWidth
              label="Observações"
              value={observations}
              onChange={(e) => setObservations(e.target.value)}
              disabled={loading}
              multiline
              rows={3}
            />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ p: 2, flexWrap: 'wrap', gap: 1 }}>
        <Button onClick={onClose} disabled={loading}>Cancelar</Button>
        <Button onClick={handleSubmit} variant="contained" disabled={loading}>
          {member ? 'Salvar' : 'Adicionar'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default FamilyMemberForm

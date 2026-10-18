import { useEffect, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControl,
  Grid,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Snackbar,
  Typography,
  useMediaQuery,
} from '@mui/material'
import CloseIcon from '@mui/icons-material/Close'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import AddIcon from '@mui/icons-material/Add'
import PersonIcon from '@mui/icons-material/Person'
import MoneyIcon from '@mui/icons-material/Money'
import HomeIcon from '@mui/icons-material/Home'
import DescriptionIcon from '@mui/icons-material/Description'
import { styled } from '@mui/material/styles'
import familyService from '../services/familyService'
import { Family, FamilyMember, FamilyMemberRequest } from '../types/family'
import { getApiErrorMessage } from '../utils/apiError'
import { maskCpf } from '../utils/cpf'
import FamilyMemberForm from './FamilyMemberForm'

const DetailRow = styled(Box)(({ theme }) => ({
  display: 'flex',
  justifyContent: 'space-between',
  gap: theme.spacing(2),
  padding: theme.spacing(1, 0),
  borderBottom: `1px solid ${theme.palette.divider}`,
  flexWrap: 'wrap',
}))

const DetailLabel = styled(Typography)(({ theme }) => ({
  fontWeight: 600,
  color: theme.palette.text.secondary,
}))

interface FamilyDetailsProps {
  open: boolean
  onClose: () => void
  family: Family | null
  onEdit: () => void
  onUpdated: () => void
  userRoles: string[]
}

function FamilyDetails({ open, onClose, family, onEdit, onUpdated, userRoles }: FamilyDetailsProps) {
  const fullScreen = useMediaQuery('(max-width:599px)')
  const [status, setStatus] = useState(family?.status || '')
  const [updatingStatus, setUpdatingStatus] = useState(false)
  const [statusError, setStatusError] = useState('')
  const [members, setMembers] = useState<FamilyMember[]>([])
  const [membersLoading, setMembersLoading] = useState(false)
  const [membersError, setMembersError] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [savingMember, setSavingMember] = useState(false)
  const [loadingMemberId, setLoadingMemberId] = useState<number | null>(null)
  const [deletingMember, setDeletingMember] = useState(false)
  const [memberFormError, setMemberFormError] = useState('')
  const [selectedMember, setSelectedMember] = useState<FamilyMember | null>(null)
  const [confirmDelete, setConfirmDelete] = useState<FamilyMember | null>(null)
  const [feedback, setFeedback] = useState('')

  const canEdit = userRoles.some((role) => ['ROLE_ADMIN', 'ROLE_RH_TESOURARIA'].includes(role))
  const canSeeFullCpf = userRoles.some((role) => ['ROLE_ADMIN', 'ROLE_RH_TESOURARIA'].includes(role))

  const statusColors: Record<string, string> = {
    CADASTRADA: '#2196F3',
    EM_ANALISE: '#FF9800',
    APROVADA: '#4CAF50',
    CONTEMPLADA: '#9C27B0',
    ATENDIDA: '#00BCD4',
    INATIVA: '#9E9E9E',
    CANCELADA: '#F44336',
  }

  const loadMembers = async () => {
    if (!family?.id) return
    setMembersLoading(true)
    setMembersError('')
    try {
      const data = await familyService.listMembers(family.id)
      setMembers(data)
    } catch (error) {
      setMembersError(getApiErrorMessage(error, 'Não foi possível carregar os membros.'))
    } finally {
      setMembersLoading(false)
    }
  }

  useEffect(() => {
    setStatus(family?.status || '')
    setStatusError('')
    if (open && family?.id) {
      loadMembers()
    }
  }, [open, family?.id, family?.status])

  const displayCpf = (cpf?: string) => {
    if (!cpf) return '-'
    return canSeeFullCpf ? cpf : maskCpf(cpf)
  }

  const formatDate = (dateString?: string): string => {
    if (!dateString) return '-'
    return new Date(dateString).toLocaleDateString('pt-BR')
  }

  const handleStatusChange = async () => {
    if (!canEdit || !family) return
    setUpdatingStatus(true)
    setStatusError('')
    try {
      await familyService.updateStatus(family.id, status)
      family.status = status
      setFeedback('Status atualizado.')
      onUpdated()
    } catch (error) {
      setStatusError(getApiErrorMessage(error, 'Erro ao alterar status. Verifique se a transição é válida.'))
    } finally {
      setUpdatingStatus(false)
    }
  }

  const handleSaveMember = async (data: FamilyMemberRequest) => {
    if (!family) return
    setSavingMember(true)
    setMemberFormError('')
    try {
      let savedMember: FamilyMember
      if (selectedMember) {
        savedMember = await familyService.updateMember(family.id, selectedMember.id, data)
        setMembers((current) => current.map((member) =>
          member.id === savedMember.id ? savedMember : member
        ))
        setFeedback('Membro atualizado.')
      } else {
        savedMember = await familyService.addMember(family.id, data)
        setMembers((current) => [savedMember, ...current])
        setFeedback('Membro adicionado.')
      }
      setFormOpen(false)
      setSelectedMember(null)
      onUpdated()
    } catch (error) {
      setMemberFormError(getApiErrorMessage(error, 'Não foi possível salvar o membro.'))
      throw error
    } finally {
      setSavingMember(false)
    }
  }

  const handleEditMember = async (member: FamilyMember) => {
    if (!family) return
    setLoadingMemberId(member.id)
    setMembersError('')
    try {
      const details = await familyService.getMember(family.id, member.id)
      setSelectedMember(details)
      setMemberFormError('')
      setFormOpen(true)
    } catch (error) {
      setMembersError(getApiErrorMessage(error, 'Não foi possível carregar os dados do membro.'))
    } finally {
      setLoadingMemberId(null)
    }
  }

  const handleDeleteMember = async () => {
    if (!family || !confirmDelete) return
    setDeletingMember(true)
    setMembersError('')
    try {
      await familyService.deleteMember(family.id, confirmDelete.id)
      setMembers((current) => current.filter((member) => member.id !== confirmDelete.id))
      setFeedback('Membro removido.')
      setConfirmDelete(null)
      onUpdated()
    } catch (error) {
      setMembersError(getApiErrorMessage(error, 'Não foi possível remover o membro.'))
      setConfirmDelete(null)
    } finally {
      setDeletingMember(false)
    }
  }

  return (
    <>
      <Dialog open={open} onClose={onClose} maxWidth="lg" fullWidth fullScreen={fullScreen}>
        <DialogTitle>
          <Box display="flex" alignItems="center" justifyContent="space-between">
            <Typography variant="h6">Detalhes da Família</Typography>
            <IconButton onClick={onClose} aria-label="Fechar">
              <CloseIcon />
            </IconButton>
          </Box>
        </DialogTitle>
        <DialogContent>
          {statusError && (
            <Alert severity="error" sx={{ mb: 2 }}>{statusError}</Alert>
          )}

          <Box sx={{ mb: 3 }}>
            <Chip
              label={status}
              sx={{
                backgroundColor: statusColors[status] || '#999',
                color: 'white',
                fontWeight: 600,
              }}
            />
          </Box>

          <Grid container spacing={3}>
            <Grid item xs={12} sm={6}>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                <PersonIcon fontSize="small" sx={{ verticalAlign: 'middle', mr: 1 }} />
                Responsável
              </Typography>
              <DetailRow>
                <DetailLabel>Nome:</DetailLabel>
                <Typography fontWeight={500}>{family?.responsibleName}</Typography>
              </DetailRow>
              <DetailRow>
                <DetailLabel>CPF:</DetailLabel>
                <Typography fontWeight={500}>{displayCpf(family?.cpf)}</Typography>
              </DetailRow>
              <DetailRow>
                <DetailLabel>Cônjuge:</DetailLabel>
                <Typography fontWeight={500}>{family?.spouseName || '-'}</Typography>
              </DetailRow>
              <DetailRow>
                <DetailLabel>Telefone:</DetailLabel>
                <Typography fontWeight={500}>{family?.phone || '-'}</Typography>
              </DetailRow>
              <DetailRow>
                <DetailLabel>Endereço:</DetailLabel>
                <Typography fontWeight={500}>{family?.address || '-'}</Typography>
              </DetailRow>
            </Grid>

            <Grid item xs={12} sm={6}>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                <HomeIcon fontSize="small" sx={{ verticalAlign: 'middle', mr: 1 }} />
                Composição
              </Typography>
              <DetailRow>
                <DetailLabel>Pessoas na residência:</DetailLabel>
                <Typography fontWeight={500}>{family?.householdSize}</Typography>
              </DetailRow>
              <DetailRow>
                <DetailLabel>Dependentes:</DetailLabel>
                <Typography fontWeight={500}>{family?.dependents || 0}</Typography>
              </DetailRow>
            </Grid>

            <Grid item xs={12} sm={6}>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                <MoneyIcon fontSize="small" sx={{ verticalAlign: 'middle', mr: 1 }} />
                Situação Financeira
              </Typography>
              <DetailRow>
                <DetailLabel>Renda familiar:</DetailLabel>
                <Typography fontWeight={500}>
                  R$ {family?.totalIncome?.toFixed(2) || '0,00'}
                </Typography>
              </DetailRow>
            </Grid>

            <Grid item xs={12} sm={6}>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                <DescriptionIcon fontSize="small" sx={{ verticalAlign: 'middle', mr: 1 }} />
                Solicitação
              </Typography>
              <DetailRow>
                <DetailLabel>Motivo:</DetailLabel>
                <Typography fontWeight={500}>{family?.requestReason?.replace(/_/g, ' ') || '-'}</Typography>
              </DetailRow>
              <DetailRow>
                <DetailLabel>Origem:</DetailLabel>
                <Typography fontWeight={500}>{family?.origin?.replace(/_/g, ' ') || '-'}</Typography>
              </DetailRow>
              <DetailRow>
                <DetailLabel>Indicado por:</DetailLabel>
                <Typography fontWeight={500}>{family?.referrerName || '-'}</Typography>
              </DetailRow>
            </Grid>

            <Grid item xs={12}>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>Observações</Typography>
              <Typography variant="body2" color="text.secondary">
                {family?.observations || 'Nenhuma observação'}
              </Typography>
            </Grid>

            {family?.previousBeneficiary && (
              <Grid item xs={12}>
                <Alert severity="info" sx={{ mt: 2 }}>
                  <Typography variant="subtitle2" fontWeight={600}>
                    Beneficiário em anos anteriores
                  </Typography>
                  {family.previousBeneficiaryYears && (
                    <Typography variant="body2">
                      Anos: {family.previousBeneficiaryYears}
                    </Typography>
                  )}
                  {family.previousBeneficiaryNotes && (
                    <Typography variant="body2" sx={{ mt: 1 }}>
                      {family.previousBeneficiaryNotes}
                    </Typography>
                  )}
                </Alert>
              </Grid>
            )}
          </Grid>

          <Divider sx={{ my: 3 }} />

          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 1, flexWrap: 'wrap', mb: 2 }}>
            <Typography variant="h6">Membros da Família</Typography>
            {canEdit && (
              <Button
                variant="contained"
                startIcon={<AddIcon />}
                onClick={() => {
                  setSelectedMember(null)
                  setMemberFormError('')
                  setFormOpen(true)
                }}
              >
                Adicionar membro
              </Button>
            )}
          </Box>

          {membersError && (
            <Alert severity="error" sx={{ mb: 2 }} onClose={() => setMembersError('')}>
              {membersError}
            </Alert>
          )}

          {membersLoading ? (
            <Box sx={{ textAlign: 'center', py: 3 }}><CircularProgress /></Box>
          ) : members.length === 0 ? (
            <Typography variant="body2" color="text.secondary">Nenhum membro cadastrado</Typography>
          ) : (
            <Grid container spacing={2}>
              {members.map((member) => (
                <Grid item xs={12} md={6} key={member.id}>
                  <Box sx={{ p: 2, border: '1px solid', borderColor: 'divider', borderRadius: 2 }}>
                    <Typography fontWeight={700}>{member.name}</Typography>
                    <Typography variant="body2">{member.relationship || 'Parentesco não informado'}</Typography>
                    <Typography variant="body2">Nascimento: {member.birthDate ? formatDate(member.birthDate) : '-'}</Typography>
                    <Typography variant="body2">CPF: {member.cpf ? maskCpf(member.cpf) : '-'}</Typography>
                    {canSeeFullCpf && member.observations && (
                      <Typography variant="body2" sx={{ mt: 1, overflowWrap: 'anywhere' }}>
                        <strong>Observações:</strong> {member.observations}
                      </Typography>
                    )}
                    {canEdit && (
                      <Box sx={{ mt: 1, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                        <Button
                          size="medium"
                          startIcon={<EditIcon />}
                          onClick={() => handleEditMember(member)}
                          disabled={loadingMemberId !== null || deletingMember}
                        >
                          {loadingMemberId === member.id ? 'Carregando...' : 'Editar'}
                        </Button>
                        <Button
                          size="medium"
                          color="error"
                          startIcon={<DeleteIcon />}
                          onClick={() => setConfirmDelete(member)}
                          disabled={loadingMemberId !== null || deletingMember}
                        >
                          Remover
                        </Button>
                      </Box>
                    )}
                  </Box>
                </Grid>
              ))}
            </Grid>
          )}

          {canEdit && (
            <Box sx={{ mt: 3 }}>
              <Divider sx={{ mb: 2 }} />
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Alterar Status
              </Typography>
              <FormControl fullWidth>
                <InputLabel>Status</InputLabel>
                <Select
                  value={status}
                  onChange={(e) => setStatus(e.target.value)}
                  label="Status"
                  disabled={updatingStatus}
                >
                  <MenuItem value="CADASTRADA">Cadastrada</MenuItem>
                  <MenuItem value="EM_ANALISE">Em Análise</MenuItem>
                  <MenuItem value="APROVADA">Aprovada</MenuItem>
                  <MenuItem value="CONTEMPLADA">Contemplada</MenuItem>
                  <MenuItem value="ATENDIDA">Atendida</MenuItem>
                  <MenuItem value="INATIVA">Inativa</MenuItem>
                  <MenuItem value="CANCELADA">Cancelada</MenuItem>
                </Select>
              </FormControl>
            </Box>
          )}
        </DialogContent>
        <DialogActions sx={{ p: 2, flexWrap: 'wrap', gap: 1 }}>
          {canEdit && (
            <Button onClick={onEdit} variant="outlined" startIcon={<EditIcon />}>
              Editar
            </Button>
          )}
          {canEdit && (
            <Button onClick={handleStatusChange} variant="contained" disabled={updatingStatus}>
              {updatingStatus ? 'Atualizando...' : 'Alterar Status'}
            </Button>
          )}
          <Button onClick={onClose}>Fechar</Button>
        </DialogActions>
      </Dialog>

      <FamilyMemberForm
        open={formOpen}
        member={selectedMember}
        onClose={() => setFormOpen(false)}
        onSubmit={handleSaveMember}
        loading={savingMember}
        error={memberFormError}
      />

      <Dialog open={Boolean(confirmDelete)} onClose={() => setConfirmDelete(null)} fullWidth maxWidth="xs">
        <DialogTitle>Remover membro</DialogTitle>
        <DialogContent>
          <Typography>
            Tem certeza que deseja remover este membro? Essa ação não poderá ser desfeita.
          </Typography>
          {membersError && (
            <Alert severity="error" sx={{ mt: 2 }}>{membersError}</Alert>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmDelete(null)} disabled={deletingMember}>Cancelar</Button>
          <Button color="error" variant="contained" onClick={handleDeleteMember} disabled={deletingMember}>
            {deletingMember ? 'Removendo...' : 'Remover'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={Boolean(feedback)}
        autoHideDuration={4000}
        onClose={() => setFeedback('')}
        message={feedback}
      />
    </>
  )
}

export default FamilyDetails

import { useNavigate, useParams } from 'react-router-dom'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Container,
  Typography,
} from '@mui/material'
import PreviewBanner from './PreviewBanner'
import { PREVIEW_FAMILIES } from './previewData'
import { maskCpf } from '../utils/cpf'

const statusColor: Record<string, string> = {
  CADASTRADA: '#2196F3',
  EM_ANALISE: '#FF9800',
  APROVADA: '#4CAF50',
  ATENDIDA: '#00BCD4',
}

function PreviewFamilyDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const family = PREVIEW_FAMILIES.find((item) => String(item.id) === id)

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default', overflowX: 'hidden' }}>
      <PreviewBanner />
      <Container maxWidth="md" sx={{ py: 3 }}>
        <Button onClick={() => navigate('/preview/admin')} sx={{ mb: 2 }}>
          Voltar ao painel
        </Button>
        {!family ? (
          <Alert severity="error">Família fictícia não encontrada nesta demonstração.</Alert>
        ) : (
          <>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 2, flexWrap: 'wrap', mb: 2 }}>
              <Typography variant="h4" color="primary">{family.responsibleName}</Typography>
              <Chip label={family.status.replace('_', ' ')} sx={{ bgcolor: statusColor[family.status], color: 'white' }} />
            </Box>
            <Card sx={{ mb: 2 }}>
              <CardContent>
                <Typography><strong>CPF de demonstração:</strong> {maskCpf(family.cpf)}</Typography>
                <Typography><strong>Cônjuge:</strong> {family.spouseName || '-'}</Typography>
                <Typography><strong>Pessoas na residência:</strong> {family.householdSize}</Typography>
                <Typography><strong>Renda fictícia:</strong> R$ {family.totalIncome.toFixed(0)}</Typography>
                <Typography><strong>Observações:</strong> {family.observations}</Typography>
              </CardContent>
            </Card>
            <Typography variant="h6" gutterBottom>Membros fictícios</Typography>
            {family.members.length === 0 ? (
              <Alert severity="info">Nenhum membro fictício cadastrado nesta família de demonstração.</Alert>
            ) : (
              family.members.map((member) => (
                <Card key={member.id} sx={{ mb: 1.5 }}>
                  <CardContent>
                    <Typography fontWeight={700}>{member.name}</Typography>
                    <Typography variant="body2">{member.relationship} · nascimento {member.birthDate}</Typography>
                    <Typography variant="body2">CPF demonstração: {maskCpf(member.cpf)}</Typography>
                  </CardContent>
                </Card>
              ))
            )}
          </>
        )}
      </Container>
    </Box>
  )
}

export default PreviewFamilyDetailPage

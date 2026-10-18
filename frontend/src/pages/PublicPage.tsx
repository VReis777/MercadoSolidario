import { Button } from '@mui/material'
import { useNavigate } from 'react-router-dom'
import PublicLanding from '../components/PublicLanding'
import { isVisualPreviewEnabled } from '../preview/previewEnabled'

function PublicPage() {
  const navigate = useNavigate()

  return (
    <PublicLanding
      onPrimaryAction={(action) => {
        if (action === 'donate') navigate('/doacao')
        if (action === 'company') navigate('/sou-empresa')
      }}
      onBenefitRequest={() => navigate('/cadastro-beneficiado')}
      extraActions={
        <>
          <Button
            variant="contained"
            onClick={() => navigate('/solicitar-atendimento')}
            sx={{ bgcolor: 'white', color: 'primary.main', mr: 1, mb: 1, '&:hover': { bgcolor: '#f3f3f3' } }}
          >
            Preciso de atendimento
          </Button>
          <Button
            variant="contained"
            onClick={() => navigate('/entrar')}
            sx={{ bgcolor: 'white', color: 'primary.main', mr: 1, mb: 1, '&:hover': { bgcolor: '#f3f3f3' } }}
          >
            Acesso administrativo
          </Button>
          {isVisualPreviewEnabled() && (
            <Button
              variant="outlined"
              onClick={() => navigate('/preview')}
              sx={{ color: 'white', borderColor: 'white', mb: 1 }}
            >
              Prévia visual
            </Button>
          )}
        </>
      }
    />
  )
}

export default PublicPage

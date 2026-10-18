import { Button } from '@mui/material'
import { useNavigate } from 'react-router-dom'
import PreviewBanner from './PreviewBanner'
import PublicLanding from '../components/PublicLanding'

function PreviewPublicPage() {
  const navigate = useNavigate()

  return (
    <>
      <PreviewBanner />
      <PublicLanding
        onBenefitRequest={() => navigate('/preview/request-help')}
        extraActions={
          <>
            <Button
              variant="contained"
              onClick={() => navigate('/preview/request-help')}
              sx={{ bgcolor: 'white', color: 'primary.main', mr: 1, mb: 1, '&:hover': { bgcolor: '#f3f3f3' } }}
            >
              Preciso de atendimento
            </Button>
            <Button
              variant="contained"
              onClick={() => navigate('/preview/admin')}
              sx={{ bgcolor: 'white', color: 'primary.main', '&:hover': { bgcolor: '#f3f3f3' } }}
            >
              Ver painel administrativo de demonstração
            </Button>
          </>
        }
      />
    </>
  )
}

export default PreviewPublicPage

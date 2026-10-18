import { Alert, Box } from '@mui/material'

function PreviewBanner() {
  return (
    <Box sx={{ position: 'sticky', top: 0, zIndex: 1300 }}>
      <Alert
        severity="warning"
        icon={false}
        sx={{
          justifyContent: 'center',
          fontWeight: 800,
          letterSpacing: 0.6,
          borderRadius: 0,
          py: 1,
          bgcolor: '#247BA0',
          color: '#fff',
          '& .MuiAlert-message': { width: '100%', textAlign: 'center' },
        }}
      >
        MODO DEMONSTRAÇÃO — DADOS FICTÍCIOS
      </Alert>
    </Box>
  )
}

export default PreviewBanner

import { useState } from 'react'
import {
  Box,
  Button,
  Card,
  CardContent,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
  Typography,
  Alert,
  CircularProgress,
  Chip,
  List,
  ListItem,
  ListItemText,
  IconButton,
} from '@mui/material'
import UploadIcon from '@mui/icons-material/Upload'
import CloseIcon from '@mui/icons-material/Close'
import { styled } from '@mui/material/styles'

const DropZone = styled(Box)(({ theme }) => ({
  border: `2px dashed ${theme.palette.divider}`,
  borderRadius: theme.shape.borderRadius,
  padding: theme.spacing(4),
  textAlign: 'center',
  cursor: 'pointer',
  transition: 'border-color 0.3s',
  '&:hover': {
    borderColor: theme.palette.primary.main,
  },
}))

interface ImportPreviousBeneficiariesProps {
  open: boolean
  onClose: () => void
  onImportComplete: () => void
}

function ImportPreviousBeneficiaries({ open, onClose, onImportComplete }: ImportPreviousBeneficiariesProps) {
  const [year, setYear] = useState('2024')
  const [file, setFile] = useState<File | null>(null)
  const [importing, setImporting] = useState(false)
  const [result, setResult] = useState<{
    imported: number
    updated: number
    errors: number
    errorMessages: string[]
  } | null>(null)
  const [error, setError] = useState('')

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = event.target.files?.[0]
    if (selectedFile) {
      setFile(selectedFile)
      setResult(null)
      setError('')
    }
  }

  const handleDrop = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault()
    const droppedFile = event.dataTransfer.files[0]
    if (droppedFile) {
      setFile(droppedFile)
      setResult(null)
      setError('')
    }
  }

  const handleDragOver = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault()
  }

  const handleImport = async () => {
    if (!file) {
      setError('Selecione um arquivo para importar')
      return
    }

    setImporting(true)
    setError('')
    setResult(null)

    try {
      const formData = new FormData()
      formData.append('file', file)

      const token = localStorage.getItem('token')
      const response = await fetch(
        `http://localhost:8080/api/import/previous-beneficiaries/${year}`,
        {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`,
          },
          body: formData,
        }
      )

      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.error || 'Erro ao importar arquivo')
      }

      const data = await response.json()
      setResult(data)
      onImportComplete()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao importar arquivo')
    } finally {
      setImporting(false)
    }
  }

  const handleClose = () => {
    setFile(null)
    setResult(null)
    setError('')
    onClose()
  }

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <Box display="flex" alignItems="center" justifyContent="space-between">
          <Typography variant="h6">Importar Beneficiários Anteriores</Typography>
          <IconButton onClick={handleClose} aria-label="Fechar">
            <CloseIcon />
          </IconButton>
        </Box>
      </DialogTitle>
      <DialogContent>
        <Box sx={{ mb: 3 }}>
          <TextField
            label="Ano de benefício"
            value={year}
            onChange={(e) => setYear(e.target.value)}
            fullWidth
            helperText="Ex: 2024, 2025"
          />
        </Box>

        <DropZone
          onDrop={handleDrop}
          onDragOver={handleDragOver}
          onClick={() => document.getElementById('file-input')?.click()}
        >
          <input
            id="file-input"
            type="file"
            accept=".csv"
            style={{ display: 'none' }}
            onChange={handleFileSelect}
          />
          <UploadIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
          <Typography variant="body1" gutterBottom>
            Arraste o arquivo CSV aqui ou clique para selecionar
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Formato CSV com colunas separadas por ponto e vírgula (;)
          </Typography>
          {file && (
            <Chip
              label={file.name}
              onDelete={() => setFile(null)}
              sx={{ mt: 2 }}
            />
          )}
        </DropZone>

        <Alert severity="info" sx={{ mt: 2 }}>
          <Typography variant="body2">
            <strong>Formato esperado do CSV:</strong><br />
            Nome; CPF; Cônjuge; Pessoas; Renda; Origem; Motivo; Indicado por; Observações
          </Typography>
        </Alert>

        {error && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {error}
          </Alert>
        )}

        {result && (
          <Card sx={{ mt: 2 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Resultado da Importação
              </Typography>
              <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 2 }}>
                <Chip label={`Importados: ${result.imported}`} color="success" />
                <Chip label={`Atualizados: ${result.updated}`} color="info" />
                <Chip label={`Erros: ${result.errors}`} color={result.errors > 0 ? 'error' : 'default'} />
              </Box>
              {result.errorMessages.length > 0 && (
                <List dense>
                  {result.errorMessages.map((msg, idx) => (
                    <ListItem key={idx}>
                      <ListItemText primary={msg} />
                    </ListItem>
                  ))}
                </List>
              )}
            </CardContent>
          </Card>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose} disabled={importing}>
          Fechar
        </Button>
        <Button
          onClick={handleImport}
          variant="contained"
          disabled={!file || importing}
          startIcon={importing ? <CircularProgress size={20} /> : <UploadIcon />}
        >
          {importing ? 'Importando...' : 'Importar'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default ImportPreviousBeneficiaries

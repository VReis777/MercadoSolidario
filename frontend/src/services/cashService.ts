import api from './api'

export interface CashItem {
  id: number
  type: 'ENTRADA' | 'SAIDA'
  amount: number
  description: string
  hasAttachment: boolean
  createdBy: string
  createdAt: string
}

export interface CashSummary {
  total: number
  donationsTotal: number
  movementsBalance: number
  movements: CashItem[]
}

const cashService = {
  summary: async (): Promise<CashSummary> => {
    const response = await api.get<CashSummary>('/cash')
    return response.data
  },

  create: async (type: 'ENTRADA' | 'SAIDA', amount: number, description: string, file?: File): Promise<CashItem> => {
    const form = new FormData()
    form.append('type', type)
    form.append('amount', String(amount))
    form.append('description', description)
    if (file) form.append('file', file)
    const response = await api.post<CashItem>('/cash', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return response.data
  },

  downloadAttachment: async (id: number): Promise<{ blob: Blob; filename: string }> => {
    const response = await api.get(`/cash/${id}/attachment`, { responseType: 'blob' })
    const disposition = response.headers['content-disposition'] || ''
    const match = disposition.match(/filename="?([^";]+)"?/)
    return { blob: response.data, filename: match ? match[1] : `comprovante-${id}` }
  },
}

export default cashService

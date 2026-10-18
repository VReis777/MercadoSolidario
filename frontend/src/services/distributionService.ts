import api from './api'

export interface DistributionItem {
  id: number
  productId: number
  productName: string
  quantity: number
  unit: string
  stockMovementId: number | null
}

export interface Distribution {
  id: number
  familyId: number
  familyResponsibleName: string
  status: 'CRIADA' | 'CONFIRMADA' | 'CANCELADA'
  createdBy: string
  createdAt: string
  completedAt: string | null
  observation: string | null
  reference: string | null
  cancellationReason: string | null
  items: DistributionItem[]
}

const distributionService = {
  list: async (): Promise<Distribution[]> => {
    const response = await api.get<Distribution[]>('/distributions')
    return response.data
  },

  create: async (request: {
    familyId: number
    items: Array<{ productId: number; quantity: number }>
    observation?: string
    reference?: string
    idempotencyKey: string
  }): Promise<Distribution> => {
    const response = await api.post<Distribution>('/distributions', request)
    return response.data
  },

  cancel: async (id: number, reason: string): Promise<Distribution> => {
    const response = await api.post<Distribution>(`/distributions/${id}/cancel`, { reason })
    return response.data
  },
}

export default distributionService

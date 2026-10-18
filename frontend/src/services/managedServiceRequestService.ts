import api from './api'

export type ServiceRequestStatus =
  | 'RECEBIDA'
  | 'EM_ANALISE'
  | 'AGUARDANDO_CONTATO'
  | 'ATENDIDA'
  | 'ENCAMINHADA'
  | 'CANCELADA'

export interface ManagedServiceRequest {
  id: number
  status: ServiceRequestStatus
  createdAt: string
  updatedAt: string
  assignedUsername: string | null
  contact: string
  location?: string
  householdSize?: number
  assistanceType: string
  welcomePreference?: 'VISIT' | 'PHONE'
  welcomeDate?: string
  welcomeTime?: string
}

export interface ManagedServiceRequestDetails extends ManagedServiceRequest {
  fullName?: string
  contactPreference?: 'PHONE' | 'EMAIL'
  message?: string
  assignedUserId?: number | null
}

export interface ServiceRequestHistoryEntry {
  id: number
  previousStatus: ServiceRequestStatus | null
  newStatus: ServiceRequestStatus | null
  changedBy: string
  changedAt: string
  observation: string | null
}

export interface ServiceRequestPage {
  content: ManagedServiceRequest[]
  totalPages: number
  totalElements: number
  number: number
  size: number
}

export interface ServiceRequestSearch {
  page: number
  size: number
  status?: ServiceRequestStatus
  fromDate?: string
  toDate?: string
  protocol?: string
}

const managedServiceRequestService = {
  search: async (params: ServiceRequestSearch): Promise<ServiceRequestPage> => {
    const response = await api.get<ServiceRequestPage>('/service-requests', { params })
    return response.data
  },

  getById: async (id: number): Promise<ManagedServiceRequestDetails> => {
    const response = await api.get<ManagedServiceRequestDetails>(`/service-requests/${id}`)
    return response.data
  },

  updateStatus: async (
    id: number,
    body: { newStatus: ServiceRequestStatus; observation?: string },
  ): Promise<void> => {
    await api.patch(`/service-requests/${id}/status`, body)
  },

  addNote: async (id: number, body: { observation: string }): Promise<void> => {
    await api.post(`/service-requests/${id}/notes`, body)
  },

  assign: async (id: number, body: { username: string }): Promise<void> => {
    await api.post(`/service-requests/${id}/assign`, body)
  },

  getHistory: async (id: number): Promise<ServiceRequestHistoryEntry[]> => {
    const response = await api.get<ServiceRequestHistoryEntry[]>(`/service-requests/${id}/history`)
    return response.data
  },
}

export default managedServiceRequestService

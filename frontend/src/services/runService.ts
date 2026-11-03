import axios from 'axios'
import api from './api'

export interface RunModalityInfo {
  modality: string
  label: string
  slots: number
  registered: number
  remaining: number
}

export interface RunInfo {
  amount: number
  cardEnabled: boolean
  pixAutomatic: boolean
  modalities: RunModalityInfo[]
}

export interface RunRegistration {
  id: number
  modality: string
  amount: number
  extraDonation: number | null
  totalCharged: number
  status: string
  paymentMethod: string
  pixCode: string | null
  pixQrUri: string | null
  checkoutUrl: string | null
  createdAt: string
}

export interface RunStatus {
  id: number
  fullName: string
  modality: string
  shirtSize: string | null
  status: string
  amount: number
  paidAt: string | null
  checkinCode: string | null
  kitDeliveredAt: string | null
}

export interface RunVerify {
  id: number
  fullName: string
  modality: string
  shirtSize: string | null
  status: string
  kitDeliveredAt: string | null
  kitDeliveredBy: string | null
  qrUri: string
}

export interface AdminRunRegistration {
  id: number
  fullName: string
  email: string | null
  phone: string | null
  modality: string
  shirtSize: string | null
  amount: number
  extraDonation: number | null
  totalCharged: number
  paymentMethod: string
  status: string
  paidAt: string | null
  createdAt: string
  kitDeliveredAt: string | null
  kitDeliveredBy: string | null
}

const publicApi = axios.create({ baseURL: import.meta.env.VITE_API_URL })

const runService = {
  info: async (): Promise<RunInfo> => {
    const { data } = await publicApi.get<RunInfo>('/api/public/rum')
    return data
  },
  register: async (request: {
    fullName: string
    email?: string
    phone?: string
    modality: string
    shirtSize?: string
    paymentMethod: string
    extraDonation?: number
  }): Promise<RunRegistration> => {
    const { data } = await publicApi.post<RunRegistration>('/api/public/rum/registrations', request)
    return data
  },
  status: async (id: number): Promise<RunStatus> => {
    const { data } = await publicApi.get<RunStatus>(`/api/public/rum/registrations/${id}`)
    return data
  },
  verify: async (code: string): Promise<RunVerify> => {
    const { data } = await publicApi.get<RunVerify>(`/api/public/rum/verify/${code}`)
    return data
  },
  kitDelivered: async (code: string): Promise<RunVerify> => {
    const { data } = await api.post<RunVerify>(`/rum/registrations/kit/${code}`)
    return data
  },
  listAdmin: async (): Promise<AdminRunRegistration[]> => {
    const { data } = await api.get<AdminRunRegistration[]>('/rum/registrations')
    return data
  },
  confirmPayment: async (id: number): Promise<RunStatus> => {
    const { data } = await api.post<RunStatus>(`/rum/registrations/${id}/confirm-payment`)
    return data
  },
  cancel: async (id: number): Promise<RunStatus> => {
    const { data } = await api.post<RunStatus>(`/rum/registrations/${id}/cancel`)
    return data
  },
}

export default runService

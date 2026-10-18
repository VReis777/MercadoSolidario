import api from './api'

export type SponsorType = 'PESSOA_FISICA' | 'EMPRESA' | 'INSTITUICAO' | 'ANONIMO'

export interface Sponsor {
  id: number
  name: string
  type: SponsorType
  companyName?: string
  phone?: string
  whatsapp?: string
  email?: string
  observations?: string
  active: boolean
  createdAt: string
}

export interface SponsorRequest {
  name: string
  type: SponsorType
  companyName?: string
  phone?: string
  whatsapp?: string
  email?: string
  observations?: string
  active?: boolean
}

const sponsorService = {
  list: async (params?: { search?: string; onlyActive?: boolean }): Promise<Sponsor[]> =>
    (await api.get<Sponsor[]>('/sponsors', { params })).data,

  create: async (data: SponsorRequest): Promise<Sponsor> =>
    (await api.post<Sponsor>('/sponsors', data)).data,

  update: async (id: number, data: SponsorRequest): Promise<Sponsor> =>
    (await api.put<Sponsor>(`/sponsors/${id}`, data)).data,

  setActive: async (id: number, active: boolean): Promise<Sponsor> =>
    (await api.patch<Sponsor>(`/sponsors/${id}/active`, { active })).data,
}

export default sponsorService

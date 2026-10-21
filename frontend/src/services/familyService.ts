import api from './api'

export interface Family {
  id: number
  responsibleName: string
  cpf: string
  spouseName?: string
  dependents: number
  householdSize: number
  totalIncome: number
  paysRent?: boolean
  rentValue?: number
  disabledCount?: number
  requestReason: string
  origin: string
  referrerName?: string
  observations?: string
  previousBeneficiary?: boolean
  previousBeneficiaryYears?: string
  previousBeneficiaryNotes?: string
  status: string
  createdAt: string
  updatedAt: string
  members: FamilyMember[]
}

export interface FamilyMember {
  id: number
  name: string
  cpf?: string
  birthDate?: string
  relationship?: string
  observations?: string
  createdAt: string
  updatedAt: string
}

export interface FamilyCreateRequest {
  responsibleName: string
  cpf: string
  spouseName?: string
  householdSize: number
  totalIncome: number
  paysRent?: boolean
  rentValue?: number
  disabledCount?: number
  requestReason: string
  origin: string
  referrerName?: string
  observations?: string
  members?: FamilyMemberRequest[]
}

export interface FamilyUpdateRequest {
  responsibleName: string
  spouseName?: string
  householdSize: number
  totalIncome: number
  paysRent?: boolean
  rentValue?: number
  disabledCount?: number
  requestReason: string
  origin: string
  referrerName?: string
  observations?: string
  members?: FamilyMemberRequest[]
}

export interface FamilyMemberRequest {
  name: string
  cpf?: string
  birthDate?: string
  relationship?: string
  observations?: string
}

export interface FamilySearchRequest {
  name?: string
  cpf?: string
  householdSize?: number
  minIncome?: number
  maxIncome?: number
  origin?: string
  requestReason?: string
  status?: string
}

export interface FamilyPageResponse {
  content: Family[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

const familyService = {
  create: async (data: FamilyCreateRequest): Promise<Family> => {
    const response = await api.post<Family>('/families', data)
    return response.data
  },

  search: async (params: FamilySearchRequest & { page?: number; size?: number; sortBy?: string; sortDir?: string }): Promise<FamilyPageResponse> => {
    const response = await api.get<FamilyPageResponse>('/families', { params })
    return response.data
  },

  getById: async (id: number): Promise<Family> => {
    const response = await api.get<Family>(`/families/${id}`)
    return response.data
  },

  update: async (id: number, data: FamilyUpdateRequest): Promise<Family> => {
    const response = await api.put<Family>(`/families/${id}`, data)
    return response.data
  },

  updateStatus: async (id: number, status: string): Promise<Family> => {
    const response = await api.patch<Family>(`/families/${id}/status`, null, { params: { status } })
    return response.data
  },

  listMembers: async (familyId: number): Promise<FamilyMember[]> => {
    const response = await api.get<FamilyMember[]>(`/families/${familyId}/members`)
    return response.data
  },

  getMember: async (familyId: number, memberId: number): Promise<FamilyMember> => {
    const response = await api.get<FamilyMember>(`/families/${familyId}/members/${memberId}`)
    return response.data
  },

  addMember: async (familyId: number, data: FamilyMemberRequest): Promise<FamilyMember> => {
    const response = await api.post<FamilyMember>(`/families/${familyId}/members`, data)
    return response.data
  },

  updateMember: async (familyId: number, memberId: number, data: FamilyMemberRequest): Promise<FamilyMember> => {
    const response = await api.put<FamilyMember>(`/families/${familyId}/members/${memberId}`, data)
    return response.data
  },

  deleteMember: async (familyId: number, memberId: number): Promise<void> => {
    await api.delete(`/families/${familyId}/members/${memberId}`)
  }
}

export default familyService

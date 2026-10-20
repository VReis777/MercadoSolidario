import api from './api'

export interface PriorityFactorConfiguration {
  factorCode: string
  label: string
  operator: string
  referenceValue: string
  weight: number
  enabled: boolean
  explanation: string
}

export interface PriorityConfiguration {
  id: number
  code: string
  name: string
  version: number
  explanation: string
  active: boolean
  createdBy: string
  updatedBy: string
  createdAt: string
  updatedAt: string
  factors: PriorityFactorConfiguration[]
}

export interface PriorityFactorResult {
  factorCode: string
  label: string
  inputValue: string
  contribution: number
  matched: boolean
  explanation: string
}

export interface FamilyPriorityResult {
  familyId: number
  score: number | null
  position: number | null
  calculatedAt: string | null
  calculationVersion: string | null
  status: string
  factors: PriorityFactorResult[]
  demonstration: boolean
}

export interface PriorityRanking {
  status: string
  calculationVersion: string | null
  calculatedAt: string | null
  families: FamilyPriorityResult[]
}

export interface PriorityConfigurationRequest {
  code: string
  name: string
  explanation: string
  factors: PriorityFactorConfiguration[]
}

const priorityService = {
  listConfigurations: async (): Promise<PriorityConfiguration[]> => {
    const response = await api.get<PriorityConfiguration[]>('/priority/configuration')
    return response.data
  },

  createConfiguration: async (data: PriorityConfigurationRequest): Promise<PriorityConfiguration> => {
    const response = await api.post<PriorityConfiguration>('/priority/configuration', data)
    return response.data
  },

  createVersion: async (id: number, data: PriorityConfigurationRequest): Promise<PriorityConfiguration> => {
    const response = await api.put<PriorityConfiguration>(`/priority/configuration/${id}`, data)
    return response.data
  },

  activateConfiguration: async (id: number): Promise<PriorityConfiguration> => {
    const response = await api.post<PriorityConfiguration>(`/priority/configuration/${id}/activate`)
    return response.data
  },

  deactivateConfiguration: async (id: number): Promise<PriorityConfiguration> => {
    const response = await api.post<PriorityConfiguration>(`/priority/configuration/${id}/deactivate`)
    return response.data
  },

  getRanking: async (): Promise<PriorityRanking> => {
    const response = await api.get<PriorityRanking>('/families/priority')
    return response.data
  },

  recalculateRanking: async (): Promise<PriorityRanking> => {
    const response = await api.post<PriorityRanking>('/families/priority/recalculate')
    return response.data
  },

  adjustPriority: async (familyId: number, adjustment: number, note: string): Promise<PriorityRanking> => {
    const response = await api.post<PriorityRanking>(`/families/${familyId}/priority/adjust`, { adjustment, note })
    return response.data
  },

  getFamilyPriority: async (familyId: number): Promise<FamilyPriorityResult> => {
    const response = await api.get<FamilyPriorityResult>(`/families/${familyId}/priority`)
    return response.data
  },

  getHistory: async (): Promise<FamilyPriorityResult[]> => {
    const response = await api.get<FamilyPriorityResult[]>('/priority/configuration/history')
    return response.data
  },
}

export default priorityService

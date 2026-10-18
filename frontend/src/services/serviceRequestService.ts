import api from './api'

export type ServiceRequestContactPreference = 'PHONE' | 'EMAIL'
export type ServiceRequestAssistanceType = 'FOOD' | 'HYGIENE' | 'OTHER'
export type ServiceRequestWelcomePreference = 'VISIT' | 'PHONE'

export interface WelcomeSlot {
  id: number
  availableDate: string
  note?: string
  active: boolean
}

export interface CreateServiceRequest {
  fullName: string
  contactValue: string
  contactPreference: ServiceRequestContactPreference
  location: string
  householdSize: number
  assistanceType: ServiceRequestAssistanceType
  message: string
  consentAccepted: boolean
  welcomePreference?: ServiceRequestWelcomePreference
  welcomeDate?: string
  welcomeTime?: string
}

export interface CreatedServiceRequest {
  protocol: string
}

const serviceRequestService = {
  create: async (request: CreateServiceRequest): Promise<CreatedServiceRequest> => {
    const response = await api.post<CreatedServiceRequest>('/public/service-requests', request)
    return response.data
  },

  welcomeSlots: async (): Promise<WelcomeSlot[]> => {
    const response = await api.get<WelcomeSlot[]>('/public/welcome-slots')
    return response.data
  },
}

export default serviceRequestService

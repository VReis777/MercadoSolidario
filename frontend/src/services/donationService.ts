import api from './api'

export type DonationStatus =
  | 'CRIADA'
  | 'AGUARDANDO_PAGAMENTO'
  | 'PAGA'
  | 'CANCELADA'
  | 'EXPIRADA'
  | 'CONFIRMADA_MANUALMENTE'

export interface Donation {
  id: number
  type: 'FINANCEIRA' | 'PRODUTO'
  donorName: string | null
  anonymous: boolean
  amount: number | null
  status: DonationStatus
  productId: number | null
  productName: string | null
  quantity: number | null
  unit: string | null
  observation: string | null
  createdAt: string
  updatedAt: string
  confirmedAt: string | null
  confirmedBy: string | null
}

export interface PublicDonationResult {
  id: number
  amount: number
  status: DonationStatus
  pixStatus: 'PIX_NAO_CONFIGURADO' | 'QR_CODE_GERADO_NAO_CONFIRMADO'
  pixPayload: string | null
  qrCodeDataUri: string | null
  message: string
}

export interface PixConfiguration {
  configured: boolean
  maskedPixKey: string | null
  merchantName: string
  merchantCity: string
}

const donationService = {
  createFinancial: async (request: {
    amount: number
    donorName?: string
    anonymous: boolean
    externalReference: string
  }): Promise<PublicDonationResult> => {
    const response = await api.post<PublicDonationResult>('/donations', request)
    return response.data
  },

  list: async (): Promise<Donation[]> => {
    const response = await api.get<Donation[]>('/donations')
    return response.data
  },

  createProductDonation: async (request: {
    productId: number
    quantity: number
    donorName?: string
    anonymous: boolean
    observation?: string
    externalReference: string
  }): Promise<Donation> => {
    const response = await api.post<Donation>('/donations/products', request)
    return response.data
  },

  confirmManually: async (id: number): Promise<Donation> => {
    const response = await api.post<Donation>(`/donations/${id}/confirm-manually`)
    return response.data
  },

  cancel: async (id: number): Promise<Donation> => {
    const response = await api.post<Donation>(`/donations/${id}/cancel`)
    return response.data
  },

  expire: async (id: number): Promise<Donation> => {
    const response = await api.post<Donation>(`/donations/${id}/expire`)
    return response.data
  },

  getPixConfiguration: async (): Promise<PixConfiguration> => {
    const response = await api.get<PixConfiguration>('/donations/configuration')
    return response.data
  },

  updatePixConfiguration: async (request: {
    pixKey?: string
    merchantName?: string
    merchantCity?: string
    clearPixKey: boolean
  }): Promise<PixConfiguration> => {
    const response = await api.put<PixConfiguration>('/donations/configuration', request)
    return response.data
  },

  getPublicPixInfo: async (): Promise<{ configured: boolean; pixKey?: string; merchantName?: string; merchantCity?: string }> => {
    const response = await api.get('/public/donations/pix-info')
    return response.data
  },
}

export default donationService

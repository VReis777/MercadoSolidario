import axios from 'axios'

const api = axios.create({ baseURL: import.meta.env.VITE_API_URL })

export interface DonationGoalItem {
  id: number
  name: string
  unit: string
  perFamilyQuantity: number
  packageSize: number
  neededQuantity: number
  pledgedQuantity: number
  receivedQuantity: number
  remainingQuantity: number
  remainingPackages: number
  productId: number | null
  stockPackages: number | null
}

export interface DonationPledgeRequest {
  goalItemId: number
  packages: number
  donorName?: string
  donorContact?: string
  anonymous: boolean
  selfDelivery: boolean
}

const donationGoalService = {
  async list(): Promise<DonationGoalItem[]> {
    const { data } = await api.get<DonationGoalItem[]>('/api/public/donation-goals')
    return data
  },

  async donors(): Promise<string[]> {
    const { data } = await api.get<string[]>('/api/public/donation-donors')
    return data
  },

  async pledge(request: DonationPledgeRequest): Promise<void> {
    await api.post('/api/public/donation-pledges', request)
  },
}

export default donationGoalService

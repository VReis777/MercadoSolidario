import api from './api'

export interface DashboardSummary {
  from: string
  to: string
  families: {
    total: number
    byStatus: Record<string, number>
    createdInPeriod: number
    inAnalysis: number
  }
  stock: {
    activeProducts: number
    inactiveProducts: number
    lowStockProducts: number
    movementsByType: Record<string, number>
    lowStock: Array<{
      productId: number
      name: string
      quantity: number
      unit: string
      minimum: number
    }>
  }
  donations: null | {
    created: number
    awaitingPayment: number
    manuallyConfirmed: number
    cancelled: number
    expired: number
    confirmedFinancialAmount: number
    productDonations: number
    donatedProductEntries: number
  }
  distributions: {
    confirmed: number
    cancelled: number
    distinctFamiliesServed: number
    itemsDistributed: number
    topProducts: Array<{
      productId: number
      productName: string
      quantity: number
      unit: string
    }>
  }
  priorityConfigured: boolean
  alerts: Array<{
    code: string
    source: string
    message: string
    occurredAt: string | null
    actionLabel: string | null
    actionUrl: string | null
    requiredRole: string
  }>
  recentStockMovements: Array<{
    id: number
    productId: number
    productName: string
    type: string
    quantity: number
    newBalance: number | null
    unit: string
    occurredAt: string
  }>
  recentDistributions: Array<{
    id: number
    itemCount: number
    occurredAt: string
  }>
}

export const dashboardService = {
  async summary(from: string, to: string): Promise<DashboardSummary> {
    const response = await api.get<DashboardSummary>('/dashboard/summary', {
      params: { from, to },
    })
    return response.data
  },
}

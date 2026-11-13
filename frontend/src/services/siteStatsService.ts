import api from './api'

export interface SiteStatsSummary {
  total: number
  pages: { path: string; views: number }[]
}

const siteStatsService = {
  summary: async (): Promise<SiteStatsSummary> => {
    const response = await api.get<SiteStatsSummary>('/admin/site-stats')
    return response.data
  },
}

export default siteStatsService

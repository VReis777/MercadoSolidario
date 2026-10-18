import api from './api'

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface HistoricalBeneficiary {
  id: number
  campaignYear: number
  name: string
  cpfMasked?: string
  phoneMasked?: string
  householdSize?: number
  origin?: string
  sourceFile: string
  sourceSheet?: string
  sourceRow?: number
  registeredThisCampaign: boolean
}

export type HistoryMatch = 'NENHUM' | 'CPF' | 'NOME'

export interface CampaignRegistration {
  id: number
  responsibleName: string
  cpfMasked?: string
  householdSize?: number
  totalIncome?: number
  origin?: string
  referrerName?: string
  status?: string
  createdAt: string
  historyMatch: HistoryMatch
  historyYears: number[]
}

export interface CampaignOverview {
  campaignYear: number
  historicalByYear: Record<string, number>
  historicalWithoutCpfByYear: Record<string, number>
  peopleInMoreThanOneYear: number
  registeredThisCampaign: number
  registeredWithHistoryByCpf: number
  registeredWithHistoryByName: number
  peopleInRegisteredFamilies: number
  activeProducts: number
  productsWithoutStock: number
  origins: string[]
  alerts: string[]
}

export type ImportRowStatus = 'NOVO' | 'DUPLICADO_NO_ARQUIVO' | 'JA_IMPORTADO' | 'INVALIDO'

export interface HistoricalImportPreview {
  fileName: string
  campaignYear: number
  totalRows: number
  newRows: number
  duplicateInFile: number
  alreadyImported: number
  invalidRows: number
  withoutCpf: number
  alsoInOtherYears: number
  matchingCurrentFamilies: number
  sheetWarnings: string[]
  rows: {
    sheet: string
    rowNumber: number
    name: string
    cpfMasked?: string
    phoneMasked?: string
    householdSize?: number
    origin?: string
    status: ImportRowStatus
    messages: string[]
  }[]
}

export interface HistoricalImportResult {
  fileName: string
  campaignYear: number
  imported: number
  skippedDuplicates: number
  skippedAlreadyImported: number
  skippedInvalid: number
}

export interface BasketImportPreview {
  fileName: string
  totalItems: number
  newItems: number
  existingItems: number
  duplicateItems: number
  items: { name: string; status: 'NOVO' | 'JA_CADASTRADO' | 'DUPLICADO_NO_ARQUIVO'; preferredBrand?: string }[]
  unmatchedBrands: string[]
  warnings: string[]
}

export interface BasketImportResult {
  fileName: string
  created: number
  skippedExisting: number
  skippedDuplicates: number
}

export type RegistrationRowStatus = 'NOVO' | 'DUPLICADO_NO_ARQUIVO' | 'JA_CADASTRADO' | 'INVALIDO'

export interface RegistrationImportPreview {
  fileName: string
  campaignYear: number
  totalRows: number
  newRows: number
  duplicateInFile: number
  alreadyRegistered: number
  invalidRows: number
  withPreviousHistory: number
  warnings: string[]
  rows: {
    sheet: string
    rowNumber: number
    name: string
    cpfMasked?: string
    phoneMasked?: string
    spouseName?: string
    householdSize?: number
    totalIncome?: number
    referrerName?: string
    status: RegistrationRowStatus
    messages: string[]
  }[]
}

export interface RegistrationImportResult {
  fileName: string
  campaignYear: number
  imported: number
  skippedDuplicates: number
  skippedAlreadyRegistered: number
  skippedInvalid: number
}

const upload = (file: File, extra: Record<string, string> = {}) => {
  const data = new FormData()
  data.append('file', file)
  Object.entries(extra).forEach(([key, value]) => data.append(key, value))
  return data
}

const multipart = { headers: { 'Content-Type': 'multipart/form-data' } }

const campaignService = {
  overview: async (year?: number): Promise<CampaignOverview> =>
    (await api.get<CampaignOverview>('/campaign/overview', { params: { year } })).data,

  registrations: async (params: {
    year?: number; search?: string; status?: string; onlyWithHistory?: boolean; page: number; size: number
  }): Promise<Page<CampaignRegistration>> =>
    (await api.get<Page<CampaignRegistration>>('/campaign/registrations', { params })).data,

  history: async (params: {
    year?: number; search?: string; origin?: string; page: number; size: number
  }): Promise<Page<HistoricalBeneficiary>> =>
    (await api.get<Page<HistoricalBeneficiary>>('/historical-beneficiaries', { params })).data,

  previewHistoryImport: async (file: File, year: number): Promise<HistoricalImportPreview> =>
    (await api.post<HistoricalImportPreview>('/historical-beneficiaries/import/preview',
      upload(file, { year: String(year) }), multipart)).data,

  confirmHistoryImport: async (file: File, year: number): Promise<HistoricalImportResult> =>
    (await api.post<HistoricalImportResult>('/historical-beneficiaries/import',
      upload(file, { year: String(year) }), multipart)).data,

  previewBasketImport: async (file: File): Promise<BasketImportPreview> =>
    (await api.post<BasketImportPreview>('/products/import/preview', upload(file), multipart)).data,

  confirmBasketImport: async (file: File): Promise<BasketImportResult> =>
    (await api.post<BasketImportResult>('/products/import', upload(file), multipart)).data,

  downloadRegistrationTemplate: async (): Promise<void> => {
    const response = await api.get('/campaign/registrations/template', { responseType: 'blob' })
    const url = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.download = `modelo-cadastros-${new Date().getFullYear()}.xlsx`
    link.click()
    URL.revokeObjectURL(url)
  },

  previewRegistrationImport: async (file: File): Promise<RegistrationImportPreview> =>
    (await api.post<RegistrationImportPreview>('/campaign/registrations/import/preview', upload(file), multipart)).data,

  confirmRegistrationImport: async (file: File): Promise<RegistrationImportResult> =>
    (await api.post<RegistrationImportResult>('/campaign/registrations/import', upload(file), multipart)).data,
}

export default campaignService

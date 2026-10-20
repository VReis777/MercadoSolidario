import api from './api'

export interface Product {
  id: number
  name: string
  category?: string
  unit: string
  currentQuantity: number
  minimumQuantity: number
  basketQuantity?: number
  active: boolean
  lowStock: boolean
  createdAt: string
  updatedAt: string
  notes?: string
}

export type MovementType =
  | 'ENTRADA_DOACAO'
  | 'ENTRADA_COMPRA'
  | 'AJUSTE'
  | 'SAIDA_DISTRIBUICAO'
  | 'PERDA'
  | 'DEVOLUCAO'

export interface StockMovement {
  id: number
  productId: number
  productName: string
  type: MovementType
  quantity: number
  previousBalance?: number
  newBalance?: number
  username?: string
  reason: string
  observation?: string
  createdAt: string
}

export interface StockSummary {
  activeProducts: number
  lowStockProducts: number
  products: Product[]
}

export interface ProductRequest {
  name: string
  category?: string
  unit: string
  minimumQuantity: number
  basketQuantity?: number
  notes?: string
}

export interface StockImportChange {
  productId: number
  name: string
  unit: string
  currentQuantity: number
  newQuantity: number
  delta: number
}

export interface MovementRequest {
  productId: number
  type: MovementType
  quantity: number
  adjustmentDirection?: 'ENTRADA' | 'SAIDA'
  reason: string
  observation?: string
}

const stockService = {
  getSummary: async (): Promise<StockSummary> => {
    const response = await api.get<StockSummary>('/stock')
    return response.data
  },

  listProducts: async (): Promise<Product[]> => {
    const response = await api.get<Product[]>('/products')
    return response.data
  },

  listLowStock: async (): Promise<Product[]> => {
    const response = await api.get<Product[]>('/stock/low')
    return response.data
  },

  createProduct: async (request: ProductRequest): Promise<Product> => {
    const response = await api.post<Product>('/products', request)
    return response.data
  },

  updateProduct: async (id: number, request: ProductRequest): Promise<Product> => {
    const response = await api.put<Product>(`/products/${id}`, request)
    return response.data
  },

  updateProductStatus: async (id: number, active: boolean): Promise<Product> => {
    const response = await api.patch<Product>(`/products/${id}/status`, { active })
    return response.data
  },

  listMovements: async (): Promise<StockMovement[]> => {
    const response = await api.get<StockMovement[]>('/stock/movements')
    return response.data
  },

  listProductMovements: async (productId: number): Promise<StockMovement[]> => {
    const response = await api.get<StockMovement[]>(`/products/${productId}/movements`)
    return response.data
  },

  registerMovement: async (request: MovementRequest): Promise<StockMovement> => {
    const response = await api.post<StockMovement>('/stock/movements', request)
    return response.data
  },
  exportWorkbook: async (): Promise<Blob> => {
    const response = await api.get('/stock/export', { responseType: 'blob' })
    return response.data
  },
  importPreview: async (file: File): Promise<StockImportChange[]> => {
    const form = new FormData()
    form.append('file', file)
    const response = await api.post<StockImportChange[]>('/stock/import/preview', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return response.data
  },
  importConfirm: async (file: File): Promise<{ changed: number; warnings: string[] }> => {
    const form = new FormData()
    form.append('file', file)
    const response = await api.post('/stock/import/confirm', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return response.data
    return response.data
  },
}

export default stockService

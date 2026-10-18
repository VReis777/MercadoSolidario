export interface Family {
  id: number
  responsibleName: string
  cpf: string
  spouseName?: string
  phone?: string
  address?: string
  dependents: number
  householdSize: number
  totalIncome: number
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
  phone?: string
  address?: string
  householdSize: number
  totalIncome: number
  requestReason: string
  origin: string
  referrerName?: string
  observations?: string
  members?: FamilyMemberRequest[]
}

export interface FamilyUpdateRequest {
  responsibleName: string
  spouseName?: string
  phone?: string
  address?: string
  householdSize: number
  totalIncome: number
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

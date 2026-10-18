export interface PreviewMember {
  id: number
  name: string
  cpf: string
  birthDate: string
  relationship: string
  observations: string
}

export interface PreviewFamily {
  id: number
  responsibleName: string
  cpf: string
  spouseName: string
  householdSize: number
  dependents: number
  totalIncome: number
  requestReason: string
  origin: string
  referrerName: string
  observations: string
  status: 'CADASTRADA' | 'EM_ANALISE' | 'APROVADA' | 'ATENDIDA'
  members: PreviewMember[]
}

export const PREVIEW_FAMILIES: PreviewFamily[] = [
  {
    id: 9001,
    responsibleName: 'Família Oliveira',
    cpf: '000.000.000-00',
    spouseName: 'Cônjuge Oliveira (fictício)',
    householdSize: 4,
    dependents: 2,
    totalIncome: 1200,
    requestReason: 'BAIXA_RENDA',
    origin: 'CADASTRO_PROPRIO',
    referrerName: 'Indicação demonstrativa',
    observations: 'Registro fictício apenas para demonstração visual.',
    status: 'CADASTRADA',
    members: [
      {
        id: 1,
        name: 'Membro Oliveira A',
        cpf: '000.000.000-01',
        birthDate: '2012-03-10',
        relationship: 'Filho',
        observations: 'Dado fictício',
      },
      {
        id: 2,
        name: 'Membro Oliveira B',
        cpf: '000.000.000-02',
        birthDate: '2015-08-22',
        relationship: 'Filha',
        observations: 'Dado fictício',
      },
    ],
  },
  {
    id: 9002,
    responsibleName: 'Família Santos',
    cpf: '111.111.111-11',
    spouseName: '',
    householdSize: 3,
    dependents: 1,
    totalIncome: 980,
    requestReason: 'DESEMPREGO',
    origin: 'INDICACAO',
    referrerName: 'Parceiro demonstrativo',
    observations: 'Registro fictício em análise.',
    status: 'EM_ANALISE',
    members: [
      {
        id: 3,
        name: 'Membro Santos A',
        cpf: '111.111.111-10',
        birthDate: '2018-01-05',
        relationship: 'Filho',
        observations: 'Dado fictício',
      },
    ],
  },
  {
    id: 9003,
    responsibleName: 'Família Esperança',
    cpf: '222.222.222-22',
    spouseName: 'Cônjuge Esperança (fictício)',
    householdSize: 5,
    dependents: 3,
    totalIncome: 1500,
    requestReason: 'FAMILIA_NUMEROSA',
    origin: 'IGREJA',
    referrerName: '',
    observations: 'Registro fictício aprovado para demonstração.',
    status: 'APROVADA',
    members: [
      {
        id: 4,
        name: 'Membro Esperança A',
        cpf: '222.222.222-20',
        birthDate: '2008-11-14',
        relationship: 'Filha',
        observations: 'Dado fictício',
      },
      {
        id: 5,
        name: 'Membro Esperança B',
        cpf: '222.222.222-21',
        birthDate: '2011-06-30',
        relationship: 'Filho',
        observations: 'Dado fictício',
      },
      {
        id: 6,
        name: 'Membro Esperança C',
        cpf: '222.222.222-23',
        birthDate: '2019-09-02',
        relationship: 'Filho',
        observations: 'Dado fictício',
      },
    ],
  },
  {
    id: 9004,
    responsibleName: 'Família Luz',
    cpf: '333.333.333-33',
    spouseName: '',
    householdSize: 2,
    dependents: 0,
    totalIncome: 1100,
    requestReason: 'PERDA_RENDA',
    origin: 'DIACONO',
    referrerName: 'Diácono demonstrativo',
    observations: 'Registro fictício já atendido.',
    status: 'ATENDIDA',
    members: [],
  },
]

export const PREVIEW_SUMMARY = {
  cadastradas: 12,
  emAnalise: 5,
  aprovadas: 8,
  atendidas: 4,
}

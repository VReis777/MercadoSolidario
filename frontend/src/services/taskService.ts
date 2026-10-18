import api from './api'

export interface AssignableUser {
  id: number
  fullName: string
  email: string
  roles: string[]
}

export interface StaffTask {
  id: number
  title: string
  description?: string
  assigneeId: number
  assigneeName: string
  assigneeEmail: string
  createdByName: string
  status: 'ABERTA' | 'CONCLUIDA'
  createdAt: string
  completedAt?: string
  emailSent: boolean
}

const taskService = {
  async listMine(): Promise<StaffTask[]> {
    const response = await api.get<StaffTask[]>('/tasks')
    return response.data
  },

  async listAssignableUsers(): Promise<AssignableUser[]> {
    const response = await api.get<AssignableUser[]>('/tasks/assignable-users')
    return response.data
  },

  async create(payload: { assigneeId: number; title: string; description?: string }): Promise<StaffTask> {
    const response = await api.post<StaffTask>('/tasks', payload)
    return response.data
  },

  async complete(id: number): Promise<StaffTask> {
    const response = await api.patch<StaffTask>(`/tasks/${id}/complete`)
    return response.data
  },
}

export default taskService

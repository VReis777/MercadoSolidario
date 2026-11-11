import api from './api';

export type AssignableRole = 'RH_TESOURARIA' | 'DIACONO';

export interface UserInvitationRequest {
  fullName: string;
  email: string;
  role: AssignableRole;
}

export interface UserInvitationResponse {
  id: number;
  fullName: string;
  email: string;
  role: AssignableRole;
}

export const userAccountService = {
  async invite(request: UserInvitationRequest): Promise<UserInvitationResponse> {
    const response = await api.post<UserInvitationResponse>('/admin/users', request);
    return response.data;
  },

  async updateRole(userId: number, role: 'ADMIN' | 'RH_TESOURARIA' | 'DIACONO'): Promise<void> {
    await api.patch(`/admin/users/${userId}/role`, { role });
  },

  async setPassword(token: string, newPassword: string): Promise<void> {
    await api.post('/auth/set-password', { token, newPassword });
  },
};

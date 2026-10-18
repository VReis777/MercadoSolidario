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

  async setPassword(token: string, newPassword: string): Promise<void> {
    await api.post('/auth/set-password', { token, newPassword });
  },
};

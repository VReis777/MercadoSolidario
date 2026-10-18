import api from './api';
import type { LoginRequest, LoginResponse, User, ChangePasswordRequest } from '../types/auth';

export const authService = {
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const response = await api.post<LoginResponse>('/auth/login', credentials);
    return response.data;
  },

  async logout(): Promise<void> {
    await api.post('/auth/logout');
  },

  async getCurrentUser(): Promise<User> {
    const response = await api.get<User>('/auth/me');
    return response.data;
  },

  async changePassword(request: ChangePasswordRequest): Promise<void> {
    await api.put('/auth/change-password', request);
  },

  saveSession(token: string, user: User, expiresInSeconds?: number): void {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(user));
    if (expiresInSeconds) {
      localStorage.setItem('expiresAt', String(Date.now() + expiresInSeconds * 1000));
    } else {
      localStorage.removeItem('expiresAt');
    }
  },

  clearSession(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    localStorage.removeItem('expiresAt');
  },

  isSessionExpired(): boolean {
    const expiresAt = Number(localStorage.getItem('expiresAt'));
    return !!expiresAt && Date.now() >= expiresAt;
  },

  markSessionExpired(): void {
    sessionStorage.setItem('sessionNotice', 'Sua sessão expirou. Após 120 horas é necessário entrar novamente.');
  },

  consumeSessionNotice(): string | null {
    const notice = sessionStorage.getItem('sessionNotice');
    sessionStorage.removeItem('sessionNotice');
    return notice;
  },

  getToken(): string | null {
    return localStorage.getItem('token');
  },

  getUser(): User | null {
    const userStr = localStorage.getItem('user');
    if (userStr) {
      try {
        return JSON.parse(userStr);
      } catch {
        return null;
      }
    }
    return null;
  },

  isAuthenticated(): boolean {
    return !!this.getToken();
  },

  hasRole(role: string): boolean {
    const user = this.getUser();
    return user?.roles.includes(role) || false;
  },

  hasAnyRole(roles: string[]): boolean {
    const user = this.getUser();
    if (!user) return false;
    return roles.some(role => user.roles.includes(role));
  },
};

import axios, { AxiosError, AxiosResponse } from 'axios';
import { authService } from './authService';

const API_URL = import.meta.env.VITE_API_URL || 'http://127.0.0.1:8080';

const api = axios.create({
  baseURL: `${API_URL}/api`,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - add JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor - handle 401 and 403
api.interceptors.response.use(
  (response: AxiosResponse) => {
    return response;
  },
  (error: AxiosError) => {
    if (error.response) {
      const status = error.response.status;

      // 401 Unauthorized - clear session and redirect to login (except wrong credentials on the login form)
      if (status === 401 && !error.config?.url?.includes('/auth/login')) {
        authService.markSessionExpired();
        authService.clearSession();
        window.location.href = '/login';
      }

      // 403 Forbidden - don't clear session, just show error
      if (status === 403) {
        error.message = 'Você não possui permissão para acessar este recurso.';
      }
    } else if (error.request) {
      // Network error
      error.message = 'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.';
    } else {
      // Other errors
      error.message = 'Ocorreu um erro inesperado. Tente novamente.';
    }

    return Promise.reject(error);
  }
);

export default api;

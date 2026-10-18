export interface User {
  id: number;
  username: string;
  fullName: string;
  roles: string[];
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token?: string;
  type?: string;
  expiresIn?: number;
  user?: User;
  /** Primeiro acesso: sem token até a senha temporária ser trocada pelo link enviado por e-mail. */
  passwordChangeRequired?: boolean;
  emailSent?: boolean;
  maskedEmail?: string;
  message?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import axios from 'axios';
import type { User, LoginRequest, LoginResponse } from '../types/auth';
import { authService } from '../services/authService';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<LoginResponse>;
  logout: () => Promise<void>;
  hasRole: (role: string) => boolean;
  hasAnyRole: (roles: string[]) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let active = true;
    const token = authService.getToken();
    const savedUser = authService.getUser();

    if (!token || !savedUser) {
      authService.clearSession();
      setIsLoading(false);
      return () => {
        active = false;
      };
    }

    if (authService.isSessionExpired()) {
      authService.markSessionExpired();
      authService.clearSession();
      setIsLoading(false);
      return () => {
        active = false;
      };
    }

    authService.getCurrentUser()
      .then((currentUser) => {
        if (active) {
          authService.saveSession(token, currentUser);
          setUser(currentUser);
        }
      })
      .catch((error: unknown) => {
        if (!active) return;
        if (axios.isAxiosError(error) && error.response?.status === 401) {
          authService.clearSession();
          setUser(null);
          return;
        }
        setUser(savedUser);
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const login = async (credentials: LoginRequest): Promise<LoginResponse> => {
    const response = await authService.login(credentials);
    if (response.token && response.user && !response.passwordChangeRequired) {
      authService.saveSession(response.token, response.user, response.expiresIn ?? undefined);
      setUser(response.user);
    }
    return response;
  };

  const logout = async (): Promise<void> => {
    try {
      await authService.logout();
    } catch {
      // Continue with logout even if API call fails
    } finally {
      authService.clearSession();
      setUser(null);
    }
  };

  const hasRole = (role: string): boolean => {
    return authService.hasRole(role);
  };

  const hasAnyRole = (roles: string[]): boolean => {
    return authService.hasAnyRole(roles);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        logout,
        hasRole,
        hasAnyRole,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

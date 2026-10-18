import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from './AuthContext';

interface RoleGuardProps {
  children: React.ReactNode;
  allowedRoles: string[];
}

export const RoleGuard: React.FC<RoleGuardProps> = ({ children, allowedRoles }) => {
  const { isAuthenticated, hasAnyRole, isLoading } = useAuth();

  if (isLoading) {
    return <div>Carregando...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/entrar" replace />;
  }

  if (!hasAnyRole(allowedRoles)) {
    return (
      <div style={{ padding: '2rem', textAlign: 'center' }}>
        <h2>Acesso Negado</h2>
        <p>Você não possui permissão para acessar este recurso.</p>
      </div>
    );
  }

  return <>{children}</>;
};

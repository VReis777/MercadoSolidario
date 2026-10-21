import type { ReactNode } from 'react'
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom'
import { ThemeProvider } from '@mui/material/styles'
import CssBaseline from '@mui/material/CssBaseline'
import { AuthProvider, useAuth } from './auth/AuthContext'
import { ThemeProvider as CustomThemeProvider, useTheme } from './contexts/ThemeContext'
import { ProtectedRoute } from './auth/ProtectedRoute'
import { RoleGuard } from './auth/RoleGuard'
import PublicPage from './pages/PublicPage'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import FamiliesPage from './pages/FamiliesPage'
import PriorityPage from './pages/PriorityPage'
import StockPage from './pages/StockPage'
import DonationsPage from './pages/DonationsPage'
import PublicDonationPage from './pages/PublicDonationPage'
import DistributionsPage from './pages/DistributionsPage'
// Reabre em dezembro: import RequestHelpPage from './pages/RequestHelpPage'
import ServiceRequestsPage from './pages/ServiceRequestsPage'
import UserManagementPage from './pages/UserManagementPage'
import SetPasswordPage from './pages/SetPasswordPage'
import AuditPage from './pages/AuditPage'
import SponsorsPage from './pages/SponsorsPage'
import ReportsPage from './pages/ReportsPage'
import SettingsPage from './pages/SettingsPage'
// Reabre em dezembro: import BeneficiaryRegistrationPage from './pages/BeneficiaryRegistrationPage'
import CompanyPage from './pages/CompanyPage'
import AppNavbar from './components/AppNavbar'

function LegacyRedirect({ to }: { to: string }) {
  const { search } = useLocation()
  return <Navigate to={to + search} replace />
}

function AppContent() {
  const { isAuthenticated } = useAuth();
  const { pathname } = useLocation()
  const publicPaths = [
    '/',
    '/donate', '/doacao',
    '/request-help', '/solicitar-atendimento',
    '/cadastro-beneficiado', '/sou-empresa',
    '/login', '/entrar',
    '/set-password', '/definir-senha',
  ]
  const showPublicNavbar = publicPaths.includes(pathname)

  const protectedRoutes: { legacy: string; path: string; element: ReactNode; roles?: string[] }[] = [
    { legacy: '/audit', path: '/painel', element: <AuditPage />, roles: ['ROLE_ADMIN', 'ROLE_RH_TESOURARIA', 'ROLE_DIACONO'] },
    { legacy: '/dashboard', path: '/modulos', element: <DashboardPage /> },
    { legacy: '/families', path: '/familias', element: <FamiliesPage /> },
    { legacy: '/priority', path: '/prioridades', element: <PriorityPage />, roles: ['ROLE_ADMIN', 'ROLE_RH_TESOURARIA'] },
    { legacy: '/stock', path: '/estoque', element: <StockPage />, roles: ['ROLE_ADMIN', 'ROLE_RH_TESOURARIA', 'ROLE_DIACONO'] },
    { legacy: '/donations', path: '/doacoes', element: <DonationsPage />, roles: ['ROLE_ADMIN', 'ROLE_RH_TESOURARIA'] },
    { legacy: '/distributions', path: '/distribuicoes', element: <DistributionsPage />, roles: ['ROLE_ADMIN', 'ROLE_RH_TESOURARIA', 'ROLE_DIACONO'] },
    { legacy: '/service-requests', path: '/solicitacoes', element: <ServiceRequestsPage />, roles: ['ROLE_ADMIN', 'ROLE_RH_TESOURARIA', 'ROLE_DIACONO'] },
    { legacy: '/sponsors', path: '/patrocinadores', element: <SponsorsPage />, roles: ['ROLE_ADMIN', 'ROLE_RH_TESOURARIA'] },
    { legacy: '/reports', path: '/relatorios', element: <ReportsPage />, roles: ['ROLE_ADMIN', 'ROLE_RH_TESOURARIA'] },
    { legacy: '/settings', path: '/configuracoes', element: <SettingsPage />, roles: ['ROLE_ADMIN'] },
    { legacy: '/admin/users', path: '/admin/usuarios', element: <UserManagementPage />, roles: ['ROLE_ADMIN'] },
  ]

  return (
    <>
      {showPublicNavbar && <AppNavbar />}
      <Routes>
        <Route path="/" element={<PublicPage />} />
        <Route path="/donate" element={<Navigate to="/doacao" replace />} />
        <Route path="/doacao" element={<PublicDonationPage />} />
        {/* Reabre em dezembro: fluxos públicos de pedido de atendimento e cadastro
        <Route path="/request-help" element={<Navigate to="/solicitar-atendimento" replace />} />
        <Route path="/solicitar-atendimento" element={<RequestHelpPage />} />
        <Route path="/cadastro-beneficiado" element={<BeneficiaryRegistrationPage />} /> */}
        <Route path="/request-help" element={<Navigate to="/" replace />} />
        <Route path="/solicitar-atendimento" element={<Navigate to="/" replace />} />
        <Route path="/cadastro-beneficiado" element={<Navigate to="/" replace />} />
        <Route path="/sou-empresa" element={<CompanyPage />} />
        <Route path="/set-password" element={<LegacyRedirect to="/definir-senha" />} />
        <Route path="/definir-senha" element={<SetPasswordPage />} />
        <Route path="/entrar" element={isAuthenticated ? <Navigate to="/painel" replace /> : <LoginPage />} />
        <Route path="/preview" element={<Navigate to="/" replace />} />
        <Route path="/login" element={<Navigate to="/entrar" replace />} />
        {protectedRoutes.flatMap(({ legacy, path, element, roles }) => [
          <Route key={legacy} path={legacy} element={<Navigate to={path} replace />} />,
          <Route
            key={path}
            path={path}
            element={
              <ProtectedRoute>
                {roles ? <RoleGuard allowedRoles={roles}>{element}</RoleGuard> : element}
              </ProtectedRoute>
            }
          />,
        ])}
      </Routes>
    </>
  );
}

function App() {
  return (
    <CustomThemeProvider>
      <AppWithTheme />
    </CustomThemeProvider>
  )
}

function AppWithTheme() {
  const { theme } = useTheme();
  
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <AuthProvider>
        <Router>
          <AppContent />
        </Router>
      </AuthProvider>
    </ThemeProvider>
  )
}

export default App

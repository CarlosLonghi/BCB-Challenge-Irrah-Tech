import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from './useAuth';

/** Só renderiza as rotas filhas com sessão; sem ela, manda para o login. */
export function ProtectedRoute() {
  const { session } = useAuth();
  return session ? <Outlet /> : <Navigate to="/login" replace />;
}

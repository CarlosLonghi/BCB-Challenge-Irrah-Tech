import { useContext } from 'react';
import type { Session } from '@/shared/api/session';
import { AuthContext } from './authContext';

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth precisa estar dentro de <AuthProvider>');
  return value;
}

/**
 * Sessão garantida (não-nula) para telas dentro do <ProtectedRoute>.
 * Evita espalhar `session!` pelos componentes.
 */
export function useSession(): Session {
  const { session } = useAuth();
  if (!session) throw new Error('useSession só pode ser usado dentro de <ProtectedRoute>');
  return session;
}

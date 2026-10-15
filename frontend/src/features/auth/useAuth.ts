import { useShallow } from 'zustand/react/shallow';
import type { Session } from '@/shared/api/session';
import { useAuthStore } from './authStore';

/** Sessão e ações de login/logout; re-renderiza só quando um desses campos muda. */
export function useAuth() {
  return useAuthStore(
    useShallow(({ session, expired, signIn, signOut }) => ({ session, expired, signIn, signOut })),
  );
}

/**
 * Sessão garantida (não-nula) para telas dentro do <ProtectedRoute>.
 * Evita espalhar `session!` pelos componentes.
 */
export function useSession(): Session {
  const session = useAuthStore((state) => state.session);
  if (!session) throw new Error('useSession só pode ser usado dentro de <ProtectedRoute>');
  return session;
}

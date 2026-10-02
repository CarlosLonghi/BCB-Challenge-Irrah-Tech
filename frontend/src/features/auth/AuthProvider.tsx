import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { setUnauthorizedHandler } from '@/shared/api/http';
import { clearSession, loadSession, saveSession, type Session } from '@/shared/api/session';
import { AuthContext } from './authContext';

/** Guarda quem está logado e expõe login/logout para a árvore de componentes. */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(loadSession);
  const [expired, setExpired] = useState(false);

  const signIn = useCallback((newSession: Session) => {
    saveSession(newSession);
    setSession(newSession);
    setExpired(false);
  }, []);

  const signOut = useCallback(() => {
    clearSession();
    setSession(null);
  }, []);

  // Qualquer 401 em rota autenticada derruba a sessão; o ProtectedRoute leva ao login.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      signOut();
      setExpired(true);
    });
    return () => setUnauthorizedHandler(null);
  }, [signOut]);

  // Objeto estável: quem usa useAuth() só re-renderiza quando algo aqui muda de fato.
  const value = useMemo(() => ({ session, expired, signIn, signOut }), [session, expired, signIn, signOut]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

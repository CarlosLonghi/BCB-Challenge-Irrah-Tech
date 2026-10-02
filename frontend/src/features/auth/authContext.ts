import { createContext } from 'react';
import type { Session } from '@/shared/api/session';

export interface AuthContextValue {
  session: Session | null;
  /** true quando a sessão caiu por 401 (ex.: backend reiniciou); o login avisa o usuário. */
  expired: boolean;
  signIn: (session: Session) => void;
  signOut: () => void;
}

// Arquivo separado do provider: o lint (react/only-export-components) pede que
// arquivos de componente exportem só componentes.
export const AuthContext = createContext<AuthContextValue | null>(null);

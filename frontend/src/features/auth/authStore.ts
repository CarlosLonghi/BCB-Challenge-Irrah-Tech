import { create } from 'zustand';
import { useAppStore } from '@/app/appStore';
import { setUnauthorizedHandler } from '@/shared/api/http';
import { clearSession, loadSession, saveSession, type Session } from '@/shared/api/session';

interface AuthState {
  session: Session | null;
  /** true quando a sessão caiu por 401 (ex.: backend reiniciou); o login avisa o usuário. */
  expired: boolean;
  signIn: (session: Session) => void;
  signOut: () => void;
  /** Sessão encerrada por 401. */
  expire: () => void;
}

/** Guarda quem está logado. Ao sair, limpa também os dados da tela logada. */
export const useAuthStore = create<AuthState>()((set) => ({
  session: loadSession(),
  expired: false,

  signIn: (session) => {
    saveSession(session);
    set({ session, expired: false });
  },

  signOut: () => {
    clearSession();
    useAppStore.getState().reset();
    set({ session: null });
  },

  expire: () => {
    useAuthStore.getState().signOut();
    set({ expired: true });
  },
}));

// Qualquer 401 em rota autenticada derruba a sessão; o ProtectedRoute leva ao login.
setUnauthorizedHandler(() => useAuthStore.getState().expire());

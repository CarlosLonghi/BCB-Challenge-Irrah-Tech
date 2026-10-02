import { useCallback, type ReactNode } from 'react';
import { getBalance } from '@/features/account/api';
import { listConversations } from '@/features/conversations/api';
import { useFetch } from '@/shared/hooks/useFetch';
import { AppDataContext } from './appDataContext';

/**
 * Busca saldo e conversas uma vez e compartilha com a tela logada.
 * Fica dentro do AppLayout: ao sair, o layout desmonta e estes dados somem junto,
 * então o próximo cliente a entrar nunca vê dados do anterior.
 */
export function AppDataProvider({ clientId, children }: { clientId: number; children: ReactNode }) {
  const balance = useFetch(useCallback(() => getBalance(clientId), [clientId]));
  const conversations = useFetch(listConversations);

  return <AppDataContext.Provider value={{ balance, conversations }}>{children}</AppDataContext.Provider>;
}

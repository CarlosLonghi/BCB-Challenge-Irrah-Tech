import { useCallback, useEffect, type ReactNode } from 'react';
import { getBalance } from '@/features/account/api';
import { useConversationPages } from '@/features/conversations/useConversationPages';
import { useEventStream } from '@/shared/hooks/useEventStream';
import { useFetch } from '@/shared/hooks/useFetch';
import { AppDataContext } from './appDataContext';

/**
 * Busca saldo e conversas uma vez, abre o stream de eventos e compartilha tudo com a tela logada.
 * Fica dentro do AppLayout: ao sair, o layout desmonta e estes dados (e a conexão) somem junto,
 * então o próximo cliente a entrar nunca vê dados do anterior.
 */
export function AppDataProvider({ clientId, children }: { clientId: number; children: ReactNode }) {
  const balance = useFetch(useCallback(() => getBalance(clientId), [clientId]));
  const conversations = useConversationPages();
  const events = useEventStream();

  // FAILED = estorno: o saldo/limite voltou. Numa reconexão, um FAILED pode ter se perdido.
  const { reload: reloadBalance } = balance;
  const { subscribe } = events;
  useEffect(
    () =>
      subscribe((event) => {
        if (event.type === 'ready' ? event.reconnected : event.data.status === 'FAILED') reloadBalance();
      }),
    [subscribe, reloadBalance],
  );

  return <AppDataContext.Provider value={{ balance, conversations, events }}>{children}</AppDataContext.Provider>;
}

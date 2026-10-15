import { useEffect } from 'react';
import { connectEvents } from '@/shared/api/events';
import { emit, subscribe, useAppStore } from './appStore';

/**
 * Enquanto a tela logada estiver montada: busca saldo e conversas, mantém a conexão SSE aberta e
 * recarrega o saldo quando ele pode ter mudado. Ao sair, fecha a conexão e limpa o store.
 */
export function useAppSession(clientId: number) {
  useEffect(() => {
    const { loadBalance, reloadConversations, setConnected, reset } = useAppStore.getState();
    void loadBalance(clientId);
    void reloadConversations();

    // FAILED = estorno: o saldo/limite voltou. Numa reconexão, um FAILED pode ter se perdido.
    const unsubscribe = subscribe((event) => {
      if (event.type === 'ready' ? event.reconnected : event.data.status === 'FAILED') void loadBalance(clientId);
    });
    const disconnect = connectEvents({ onEvent: emit, onConnectionChange: setConnected });

    return () => {
      disconnect();
      unsubscribe();
      reset();
    };
  }, [clientId]);
}

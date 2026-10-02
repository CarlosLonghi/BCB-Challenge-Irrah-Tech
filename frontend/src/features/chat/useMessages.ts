import { useCallback, useEffect } from 'react';
import { useFetch } from '@/shared/hooks/useFetch';
import { listMessages } from './api';
import { hasPendingMessages, POLLING_INTERVAL_MS } from './messageRules';

/**
 * Histórico da conversa com polling: o backend não tem WebSocket, então rebusca a cada 3 s
 * enquanto alguma mensagem estiver QUEUED/PROCESSING/SENT e para quando todas chegam ao estado final.
 */
export function useMessages(conversationId: number) {
  const messages = useFetch(useCallback(() => listMessages(conversationId), [conversationId]));
  const { data, reload } = messages;

  // Cada resposta nova roda o efeito de novo: se ainda há pendentes, agenda a próxima busca.
  // O cleanup cancela o timer se a tela fechar ou se os dados mudarem antes dos 3 s.
  useEffect(() => {
    if (!hasPendingMessages(data)) return;
    const timer = setTimeout(reload, POLLING_INTERVAL_MS);
    return () => clearTimeout(timer);
  }, [data, reload]);

  return messages;
}

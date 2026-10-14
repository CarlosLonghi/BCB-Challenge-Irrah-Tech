import { useCallback, useEffect, useState } from 'react';
import type { Message } from '@/shared/api/types';
import { isFullPage, mergeById } from '@/shared/api/pagination';
import { useAction } from '@/shared/hooks/useAction';
import { useFetch } from '@/shared/hooks/useFetch';
import { listMessages } from './api';
import { POLLING_INTERVAL_MS, hasPendingMessages } from './messageRules';

/**
 * Histórico da conversa, paginado por cursor:
 * - a página recente (as últimas mensagens) é buscada ao abrir e rebuscada a cada 3 s enquanto alguma
 *   ainda vai mudar de status (o backend não tem WebSocket);
 * - "carregar anteriores" busca as mensagens antes da mais antiga já carregada e as junta no topo.
 * O ChatPage monta um Chat novo por conversa, então o estado das páginas antigas não vaza entre conversas.
 */
export function useMessages(conversationId: number) {
  const recent = useFetch(useCallback(() => listMessages(conversationId), [conversationId]));
  const { data, reload } = recent;
  const [older, setOlder] = useState<Message[]>([]);
  // undefined = ainda não buscou páginas antigas; vale o que a página recente indica
  const [olderHasMore, setOlderHasMore] = useState<boolean | undefined>(undefined);

  // Cada resposta nova roda o efeito de novo: se ainda há pendentes, agenda a próxima busca.
  // O cleanup cancela o timer se a tela fechar ou se os dados mudarem antes dos 3 s.
  useEffect(() => {
    if (!hasPendingMessages(data)) return;
    const timer = setTimeout(reload, POLLING_INTERVAL_MS);
    return () => clearTimeout(timer);
  }, [data, reload]);

  // A página recente é a versão mais nova de cada mensagem (status atualizado pelo polling).
  const messages = data && [...mergeById(data, older)].sort((a, b) => a.id - b.id);

  const loadOlder = useAction(async (before: number) => {
    const page = await listMessages(conversationId, before);
    setOlder((current) => mergeById(page, current));
    setOlderHasMore(isFullPage(page));
  });

  return {
    ...recent,
    data: messages,
    hasOlder: olderHasMore ?? isFullPage(data),
    // O erro fica em loadOlder.error para a tela mostrar; o catch só evita o aviso de promise rejeitada.
    loadOlder: () => {
      if (messages?.length) loadOlder.run(messages[0].id).catch(() => {});
    },
    isLoadingOlder: loadOlder.isPending,
    olderError: loadOlder.error,
  };
}

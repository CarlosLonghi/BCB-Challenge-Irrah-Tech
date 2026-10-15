import { useCallback, useEffect, useState } from 'react';
import type { EventStream } from '@/shared/api/events';
import type { Message } from '@/shared/api/types';
import { isFullPage, mergeById } from '@/shared/api/pagination';
import { useAction } from '@/shared/hooks/useAction';
import { useFetch } from '@/shared/hooks/useFetch';
import { listMessages } from './api';
import { POLLING_INTERVAL_MS, hasPendingMessages } from './messageRules';

/**
 * Histórico da conversa, paginado por cursor:
 * - a página recente (as últimas mensagens) é buscada ao abrir;
 * - o status muda pelo stream de eventos (SSE): a mensagem já carregada é atualizada na hora, e uma
 *   mensagem nova (ex.: enviada em outra aba) faz buscar a página recente de novo;
 * - com o stream fora do ar, volta ao polling de 3 s enquanto alguma mensagem ainda vai mudar de status;
 * - "carregar anteriores" busca as mensagens antes da mais antiga já carregada e as junta no topo.
 * O ChatPage monta um Chat novo por conversa, então o estado das páginas antigas não vaza entre conversas.
 */
export function useMessages(conversationId: number, events: EventStream) {
  const recent = useFetch(useCallback(() => listMessages(conversationId), [conversationId]));
  const { data, reload, setData } = recent;
  const [older, setOlder] = useState<Message[]>([]);
  // undefined = ainda não buscou páginas antigas; vale o que a página recente indica
  const [olderHasMore, setOlderHasMore] = useState<boolean | undefined>(undefined);
  const { connected, subscribe } = events;

  useEffect(
    () =>
      subscribe((event) => {
        // Reconectou: algum status pode ter mudado enquanto o stream estava fora
        if (event.type === 'ready') {
          if (event.reconnected) reload();
          return;
        }
        const { messageId, conversationId: eventConversationId, status } = event.data;
        if (eventConversationId !== conversationId) return;

        // Mais nova que a página carregada (ou página ainda carregando): busca de novo
        const newestId = data?.at(-1)?.id;
        if (newestId === undefined || messageId > newestId) {
          reload();
          return;
        }
        const withStatus = (list: Message[]) =>
          list.map((message) => (message.id === messageId ? { ...message, status } : message));
        setData((current) => current && withStatus(current));
        setOlder(withStatus);
      }),
    [subscribe, conversationId, data, reload, setData],
  );

  // Plano B sem o stream: cada resposta roda o efeito de novo e, se ainda há pendentes, agenda a próxima busca.
  // O cleanup cancela o timer se a tela fechar, se o stream voltar ou se os dados mudarem antes dos 3 s.
  useEffect(() => {
    if (connected || !hasPendingMessages(data)) return;
    const timer = setTimeout(reload, POLLING_INTERVAL_MS);
    return () => clearTimeout(timer);
  }, [connected, data, reload]);

  // A página recente é a versão mais nova de cada mensagem.
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

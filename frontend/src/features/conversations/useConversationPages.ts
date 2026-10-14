import { useCallback, useState } from 'react';
import type { Conversation } from '@/shared/api/types';
import { isFullPage, mergeById } from '@/shared/api/pagination';
import { useAction } from '@/shared/hooks/useAction';
import { useFetch, type FetchState } from '@/shared/hooks/useFetch';
import { listConversations } from './api';

export interface ConversationPages extends FetchState<Conversation[]> {
  hasMore: boolean;
  loadMore: () => void;
  isLoadingMore: boolean;
  loadMoreError: Error | null;
}

/**
 * Conversas paginadas por page/size (a ordem muda a cada envio, então não há cursor estável).
 * `reload` volta para a primeira página: depois de um envio a conversa sobe para o topo e as
 * páginas seguintes mudariam de posição.
 */
export function useConversationPages(): ConversationPages {
  const first = useFetch(useCallback(() => listConversations(0), []));
  const [nextPages, setNextPages] = useState<Conversation[][]>([]);

  const loadMore = useAction(async (page: number) => {
    const conversations = await listConversations(page);
    setNextPages((current) => [...current, conversations]);
  });

  const { reload: reloadFirst } = first;
  const reload = useCallback(() => {
    setNextPages([]);
    reloadFirst();
  }, [reloadFirst]);

  const lastPage = nextPages.length > 0 ? nextPages[nextPages.length - 1] : first.data;

  return {
    ...first,
    data: first.data && mergeById(first.data, nextPages.flat()),
    reload,
    hasMore: isFullPage(lastPage),
    // O erro fica em loadMoreError para a tela mostrar; o catch só evita o aviso de promise rejeitada.
    loadMore: () => {
      loadMore.run(nextPages.length + 1).catch(() => {});
    },
    isLoadingMore: loadMore.isPending,
    loadMoreError: loadMore.error,
  };
}

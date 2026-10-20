import { useMemo } from 'react';
import { create } from 'zustand';
import { useShallow } from 'zustand/react/shallow';
import { getBalance } from '@/features/account/api';
import { listConversations } from '@/features/conversations/api';
import type { EventStream, StreamEvent, StreamListener } from '@/shared/api/events';
import { isFullPage, mergeById } from '@/shared/api/pagination';
import type { Balance, Conversation } from '@/shared/api/types';

/** Resultado de uma busca do store: `error` não apaga `data`, então um erro ao recarregar mantém o que já estava na tela. */
interface Loadable<T> {
  data: T | undefined;
  error: Error | null;
}

interface AppState {
  balance: Loadable<Balance>;
  /** Primeira página das conversas. */
  conversations: Loadable<Conversation[]>;
  /** Páginas seguintes, na ordem em que o "Carregar mais" as trouxe. */
  nextPages: Conversation[][];
  isLoadingMore: boolean;
  loadMoreError: Error | null;
  connected: boolean;

  loadBalance: (clientId: number) => Promise<void>;
  /** Troca o saldo sem ir ao backend (ex.: saldo que já veio na resposta do envio). */
  setBalance: (update: (current: Balance | undefined) => Balance | undefined) => void;
  /** Volta para a primeira página: depois de um envio a conversa sobe e as páginas seguintes mudariam. */
  reloadConversations: () => Promise<void>;
  loadMoreConversations: () => Promise<void>;
  setConnected: (connected: boolean) => void;
  /** Volta ao estado inicial e descarta as buscas em andamento (logout, saída do layout). */
  reset: () => void;
}

const EMPTY = {
  balance: { data: undefined, error: null },
  conversations: { data: undefined, error: null },
  nextPages: [],
  isLoadingMore: false,
  loadMoreError: null,
  connected: false,
} satisfies Partial<AppState>;

// Cada busca recebe um número; só a mais recente de cada tipo grava no store. Uma resposta que chega
// depois de outra busca do mesmo tipo, ou depois do reset (logout), é descartada: é o que a flag
// `ignore` do useFetch faz para o estado local.
type RequestKind = 'balance' | 'conversations' | 'loadMore';
let lastRequestId = 0;
const latestRequest = new Map<RequestKind, number>();

function startRequest(kind: RequestKind) {
  const id = ++lastRequestId;
  latestRequest.set(kind, id);
  return () => latestRequest.get(kind) === id;
}

function toError(err: unknown) {
  return err instanceof Error ? err : new Error('Algo deu errado.');
}

/**
 * Dados usados por mais de uma parte da tela logada: o cabeçalho mostra o saldo, a coluna da
 * esquerda mostra as conversas, e o envio de mensagem (no chat) precisa atualizar os dois.
 * `connected` é o estado da conexão SSE única da tela (status das mensagens em tempo real).
 *
 * O store é global e sobrevive ao unmount: quem limpa é o `reset()`, chamado pelo `useAppSession`
 * ao sair do layout e pelo `signOut`. Assim o próximo cliente a entrar nunca vê dados do anterior.
 * Estado de uma tela só (mensagens, conversa aberta) continua no `useFetch`, não aqui.
 */
export const useAppStore = create<AppState>()((set, get) => ({
  ...EMPTY,

  loadBalance: async (clientId) => {
    const isCurrent = startRequest('balance');
    try {
      const data = await getBalance(clientId);
      if (isCurrent()) set({ balance: { data, error: null } });
    } catch (err) {
      if (isCurrent()) set((state) => ({ balance: { ...state.balance, error: toError(err) } }));
    }
  },

  setBalance: (update) => set((state) => ({ balance: { ...state.balance, data: update(state.balance.data) } })),

  reloadConversations: async () => {
    const isCurrent = startRequest('conversations');
    // Um "Carregar mais" em andamento traria uma página da ordem antiga.
    latestRequest.delete('loadMore');
    set({ nextPages: [], isLoadingMore: false, loadMoreError: null });
    try {
      const data = await listConversations(0);
      if (isCurrent()) set({ conversations: { data, error: null } });
    } catch (err) {
      if (isCurrent()) set((state) => ({ conversations: { ...state.conversations, error: toError(err) } }));
    }
  },

  loadMoreConversations: async () => {
    const isCurrent = startRequest('loadMore');
    const page = get().nextPages.length + 1;
    set({ isLoadingMore: true, loadMoreError: null });
    try {
      const conversations = await listConversations(page);
      if (isCurrent()) set((state) => ({ nextPages: [...state.nextPages, conversations], isLoadingMore: false }));
    } catch (err) {
      if (isCurrent()) set({ loadMoreError: toError(err), isLoadingMore: false });
    }
  },

  setConnected: (connected) => set({ connected }),

  reset: () => {
    latestRequest.clear();
    set(EMPTY);
  },
}));

// Assinantes do stream: ficam fora do estado porque não mudam a tela, só recebem eventos.
const listeners = new Set<StreamListener>();

/** Recebe cada evento do stream até chamar a função devolvida. */
export function subscribe(listener: StreamListener) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

/** Repassa um evento do stream a todos os assinantes (chamado pelo `useAppSession`). */
export function emit(event: StreamEvent) {
  listeners.forEach((listener) => listener(event));
}

// Hooks de leitura: cada componente re-renderiza só quando o pedaço que ele usa muda.

/** Saldo e limite do cliente logado; o cabeçalho escolhe qual mostrar pelo plano. */
export function useBalance() {
  return useAppStore((state) => state.balance);
}

export interface ConversationPages {
  data: Conversation[] | undefined;
  error: Error | null;
  /** true só na primeira carga: ao recarregar, os dados antigos continuam na tela. */
  isPending: boolean;
  reload: () => void;
  hasMore: boolean;
  loadMore: () => void;
  isLoadingMore: boolean;
  loadMoreError: Error | null;
}

/** Conversas paginadas por page/size (a ordem muda a cada envio, então não há cursor estável). */
export function useConversations(): ConversationPages {
  const { conversations, nextPages, isLoadingMore, loadMoreError, reloadConversations, loadMoreConversations } =
    useAppStore(
      useShallow((state) => ({
        conversations: state.conversations,
        nextPages: state.nextPages,
        isLoadingMore: state.isLoadingMore,
        loadMoreError: state.loadMoreError,
        reloadConversations: state.reloadConversations,
        loadMoreConversations: state.loadMoreConversations,
      })),
    );
  const { data, error } = conversations;
  const lastPage = nextPages.length > 0 ? nextPages[nextPages.length - 1] : data;

  return {
    data: data && mergeById(data, nextPages.flat()),
    error,
    isPending: data === undefined && error === null,
    reload: () => void reloadConversations(),
    hasMore: isFullPage(lastPage),
    loadMore: () => void loadMoreConversations(),
    isLoadingMore,
    loadMoreError,
  };
}

/** Uma conexão SSE para a tela logada inteira; cada parte da tela assina os eventos que lhe interessam. */
export function useEvents(): EventStream {
  const connected = useAppStore((state) => state.connected);
  return useMemo(() => ({ connected, subscribe }), [connected]);
}

import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { getBalance } from '@/features/account/api';
import { listConversations } from '@/features/conversations/api';
import { connectEvents } from '@/shared/api/events';
import { PAGE_SIZE } from '@/shared/api/pagination';
import type { Balance, Conversation } from '@/shared/api/types';
import { emit, useAppStore, useConversations } from './appStore';
import { useAppSession } from './useAppSession';

vi.mock('@/features/account/api', () => ({ getBalance: vi.fn() }));
vi.mock('@/features/conversations/api', () => ({ listConversations: vi.fn() }));
vi.mock('@/shared/api/events', () => ({ connectEvents: vi.fn() }));

const BALANCE: Balance = { balance: 10, limit: 0 };

function conversation(id: number): Conversation {
  return {
    id, clientId: 1, recipientId: 100 + id, recipientName: `Destinatário ${id}`,
    lastMessageContent: 'Olá', lastMessageTime: '2026-10-14T12:00:00Z', unreadCount: 0,
  };
}

function page(firstId: number, size: number) {
  return Array.from({ length: size }, (_, index) => conversation(firstId + index));
}

/** Promise que o teste resolve na hora que quiser (resposta atrasada). */
function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((res) => {
    resolve = res;
  });
  return { promise, resolve };
}

describe('appStore', () => {
  beforeEach(() => {
    vi.mocked(getBalance).mockReset().mockResolvedValue(BALANCE);
    vi.mocked(listConversations).mockReset();
    vi.mocked(connectEvents).mockReset().mockReturnValue(() => {});
  });

  afterEach(() => {
    useAppStore.getState().reset();
  });

  it('reset descarta a resposta que chega depois (logout no meio da busca)', async () => {
    const response = deferred<Balance>();
    vi.mocked(getBalance).mockReturnValue(response.promise);

    const loading = useAppStore.getState().loadBalance(1);
    useAppStore.getState().reset();
    response.resolve(BALANCE);
    await loading;

    expect(useAppStore.getState().balance.data).toBeUndefined();
  });

  it('só a busca mais recente grava, mesmo que a anterior responda por último', async () => {
    const slow = deferred<Conversation[]>();
    vi.mocked(listConversations).mockReturnValueOnce(slow.promise).mockResolvedValueOnce([conversation(2)]);

    const first = useAppStore.getState().reloadConversations();
    await useAppStore.getState().reloadConversations();
    slow.resolve([conversation(1)]);
    await first;

    expect(useAppStore.getState().conversations.data).toEqual([conversation(2)]);
  });

  it('"Carregar mais" junta a próxima página; recarregar volta para a primeira', async () => {
    vi.mocked(listConversations).mockResolvedValueOnce(page(1, PAGE_SIZE)).mockResolvedValueOnce(page(51, 3));
    const { result } = renderHook(() => useConversations());

    await act(() => useAppStore.getState().reloadConversations());
    expect(result.current.hasMore).toBe(true);

    await act(() => useAppStore.getState().loadMoreConversations());
    expect(listConversations).toHaveBeenLastCalledWith(1);
    expect(result.current.data).toHaveLength(PAGE_SIZE + 3);
    expect(result.current.hasMore).toBe(false);

    vi.mocked(listConversations).mockResolvedValueOnce(page(1, 2));
    await act(() => useAppStore.getState().reloadConversations());
    expect(listConversations).toHaveBeenLastCalledWith(0);
    expect(result.current.data).toHaveLength(2);
  });

  it('useAppSession busca os dados, recarrega o saldo num FAILED e limpa tudo ao desmontar', async () => {
    vi.mocked(listConversations).mockResolvedValue([conversation(1)]);
    const disconnect = vi.fn();
    vi.mocked(connectEvents).mockReturnValue(disconnect);

    const { unmount } = renderHook(() => useAppSession(1));
    await act(async () => {});
    expect(getBalance).toHaveBeenCalledOnce();
    expect(useAppStore.getState().conversations.data).toHaveLength(1);

    await act(async () => emit({ type: 'message-status', data: { messageId: 3, conversationId: 7, status: 'DELIVERED' } }));
    expect(getBalance).toHaveBeenCalledOnce();
    await act(async () => emit({ type: 'message-status', data: { messageId: 3, conversationId: 7, status: 'FAILED' } }));
    expect(getBalance).toHaveBeenCalledTimes(2);

    unmount();
    expect(disconnect).toHaveBeenCalledOnce();
    expect(useAppStore.getState().balance.data).toBeUndefined();
    expect(useAppStore.getState().conversations.data).toBeUndefined();
  });
});

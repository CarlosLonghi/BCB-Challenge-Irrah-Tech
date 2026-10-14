import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Message } from '@/shared/api/types';
import { PAGE_SIZE } from '@/shared/api/pagination';
import { listMessages } from './api';
import { useMessages } from './useMessages';

vi.mock('./api', () => ({ listMessages: vi.fn() }));

function message(id: number): Message {
  return {
    id, conversationId: 7, senderId: 1, recipientId: 55, content: `Mensagem ${id}`,
    createdAt: '2026-10-14T12:00:00Z', priority: 'NORMAL', status: 'READ', cost: 0.25,
  };
}

function range(from: number, to: number) {
  return Array.from({ length: to - from + 1 }, (_, index) => message(from + index));
}

describe('useMessages', () => {
  beforeEach(() => {
    vi.mocked(listMessages).mockReset();
  });

  it('página recente cheia: oferece as anteriores e as junta no topo, em ordem', async () => {
    vi.mocked(listMessages).mockImplementation(async (_conversationId, before) =>
      before === undefined ? range(11, 10 + PAGE_SIZE) : range(1, 10),
    );

    const { result } = renderHook(() => useMessages(7));
    await waitFor(() => expect(result.current.data).toHaveLength(PAGE_SIZE));
    expect(result.current.hasOlder).toBe(true);

    act(() => result.current.loadOlder());

    await waitFor(() => expect(result.current.data).toHaveLength(PAGE_SIZE + 10));
    expect(listMessages).toHaveBeenLastCalledWith(7, 11);
    expect(result.current.data?.[0].id).toBe(1);
    expect(result.current.data?.at(-1)?.id).toBe(10 + PAGE_SIZE);
    // A página antiga veio incompleta: não há mais nada antes dela
    expect(result.current.hasOlder).toBe(false);
  });

  it('página recente incompleta: não há anteriores', async () => {
    vi.mocked(listMessages).mockResolvedValue(range(1, 3));

    const { result } = renderHook(() => useMessages(7));

    await waitFor(() => expect(result.current.data).toHaveLength(3));
    expect(result.current.hasOlder).toBe(false);
  });
});

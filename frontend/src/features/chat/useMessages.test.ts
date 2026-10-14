import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { StreamEvent } from '@/shared/api/events';
import type { Message, MessageStatus } from '@/shared/api/types';
import { PAGE_SIZE } from '@/shared/api/pagination';
import type { EventStream, StreamListener } from '@/shared/hooks/useEventStream';
import { listMessages } from './api';
import { POLLING_INTERVAL_MS } from './messageRules';
import { useMessages } from './useMessages';

vi.mock('./api', () => ({ listMessages: vi.fn() }));

function message(id: number, status: MessageStatus = 'READ'): Message {
  return {
    id, conversationId: 7, senderId: 1, recipientId: 55, content: `Mensagem ${id}`,
    createdAt: '2026-10-14T12:00:00Z', priority: 'NORMAL', status, cost: 0.25,
  };
}

function range(from: number, to: number) {
  return Array.from({ length: to - from + 1 }, (_, index) => message(from + index));
}

/** Stream falso: guarda os listeners para o teste disparar eventos. */
function fakeStream(connected = true) {
  const listeners = new Set<StreamListener>();
  const stream: EventStream = {
    connected,
    subscribe: (listener) => {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
  };
  const emit = (event: StreamEvent) => act(() => listeners.forEach((listener) => listener(event)));
  const statusOf = (messageId: number, status: MessageStatus, conversationId = 7) =>
    emit({ type: 'message-status', data: { messageId, conversationId, status } });
  return { stream, emit, statusOf };
}

describe('useMessages', () => {
  beforeEach(() => {
    vi.mocked(listMessages).mockReset();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('página recente cheia: oferece as anteriores e as junta no topo, em ordem', async () => {
    vi.mocked(listMessages).mockImplementation(async (_conversationId, before) =>
      before === undefined ? range(11, 10 + PAGE_SIZE) : range(1, 10),
    );

    const { result } = renderHook(() => useMessages(7, fakeStream().stream));
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

    const { result } = renderHook(() => useMessages(7, fakeStream().stream));

    await waitFor(() => expect(result.current.data).toHaveLength(3));
    expect(result.current.hasOlder).toBe(false);
  });

  it('evento de mensagem já carregada: troca o status na hora, sem nova busca', async () => {
    vi.mocked(listMessages).mockResolvedValue([message(1), message(2, 'QUEUED')]);
    const { stream, statusOf } = fakeStream();
    const { result } = renderHook(() => useMessages(7, stream));
    await waitFor(() => expect(result.current.data).toHaveLength(2));

    statusOf(2, 'SENT');

    expect(result.current.data?.[1].status).toBe('SENT');
    expect(listMessages).toHaveBeenCalledOnce();
  });

  it('evento de mensagem mais nova que a página (ex.: enviada em outra aba): busca de novo', async () => {
    vi.mocked(listMessages).mockResolvedValueOnce([message(1)]).mockResolvedValueOnce([message(1), message(2, 'PROCESSING')]);
    const { stream, statusOf } = fakeStream();
    const { result } = renderHook(() => useMessages(7, stream));
    await waitFor(() => expect(result.current.data).toHaveLength(1));

    statusOf(2, 'PROCESSING');

    await waitFor(() => expect(result.current.data).toHaveLength(2));
    expect(listMessages).toHaveBeenCalledTimes(2);
  });

  it('ignora eventos de outra conversa', async () => {
    vi.mocked(listMessages).mockResolvedValue([message(1, 'QUEUED')]);
    const { stream, statusOf } = fakeStream();
    const { result } = renderHook(() => useMessages(7, stream));
    await waitFor(() => expect(result.current.data).toHaveLength(1));

    statusOf(1, 'SENT', 8);
    statusOf(9, 'SENT', 8);

    expect(result.current.data?.[0].status).toBe('QUEUED');
    expect(listMessages).toHaveBeenCalledOnce();
  });

  it('reconexão do stream: busca de novo, porque eventos podem ter se perdido', async () => {
    vi.mocked(listMessages).mockResolvedValue([message(1)]);
    const { stream, emit } = fakeStream();
    const { result } = renderHook(() => useMessages(7, stream));
    await waitFor(() => expect(result.current.data).toHaveLength(1));

    emit({ type: 'ready', reconnected: false });
    expect(listMessages).toHaveBeenCalledOnce();

    emit({ type: 'ready', reconnected: true });
    await waitFor(() => expect(listMessages).toHaveBeenCalledTimes(2));
  });

  it('com o stream conectado não faz polling, mesmo com pendentes', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    vi.mocked(listMessages).mockResolvedValue([message(1, 'QUEUED')]);
    const { result } = renderHook(() => useMessages(7, fakeStream(true).stream));
    await waitFor(() => expect(result.current.data).toHaveLength(1));

    await act(() => vi.advanceTimersByTimeAsync(POLLING_INTERVAL_MS * 2));

    expect(listMessages).toHaveBeenCalledOnce();
  });

  it('com o stream fora: volta ao polling enquanto há pendentes', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    vi.mocked(listMessages).mockResolvedValueOnce([message(1, 'QUEUED')]).mockResolvedValue([message(1, 'READ')]);
    const { result } = renderHook(() => useMessages(7, fakeStream(false).stream));
    await waitFor(() => expect(result.current.data).toHaveLength(1));

    await act(() => vi.advanceTimersByTimeAsync(POLLING_INTERVAL_MS));
    await waitFor(() => expect(result.current.data?.[0].status).toBe('READ'));

    // Nada mais pendente: para de buscar
    await act(() => vi.advanceTimersByTimeAsync(POLLING_INTERVAL_MS * 2));
    expect(listMessages).toHaveBeenCalledTimes(2);
  });
});

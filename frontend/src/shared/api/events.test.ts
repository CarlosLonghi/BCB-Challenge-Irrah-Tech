import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { connectEvents, parseSse, retryDelay, type StreamEvent } from './events';
import { setUnauthorizedHandler } from './http';

describe('parseSse', () => {
  it('separa eventos completos, junta linhas de data e guarda o pedaço incompleto', () => {
    const { events, rest } = parseSse('event:ready\ndata:\n\n:ping\n\nevent: message-status\ndata:{"a":\ndata:1}\n\nevent:mess');

    expect(events).toEqual([
      { event: 'ready', data: '' },
      { event: 'message-status', data: '{"a":\n1}' },
    ]);
    expect(rest).toBe('event:mess');
  });

  it('aceita \\r\\n e ignora blocos só com comentário', () => {
    expect(parseSse(':ping\r\n\r\ndata:oi\r\n\r\n').events).toEqual([{ event: 'message', data: 'oi' }]);
  });
});

describe('retryDelay', () => {
  it('dobra a cada tentativa, até 30 s', () => {
    expect([0, 1, 2, 10].map(retryDelay)).toEqual([1000, 2000, 4000, 30_000]);
  });
});

/** Resposta com corpo em stream, entregue em pedaços. */
function streamResponse(chunks: string[]) {
  const encoder = new TextEncoder();
  const body = new ReadableStream<Uint8Array>({
    start(controller) {
      chunks.forEach((chunk) => controller.enqueue(encoder.encode(chunk)));
      controller.close();
    },
  });
  return new Response(body, { status: 200, headers: { 'Content-Type': 'text/event-stream' } });
}

describe('connectEvents', () => {
  const fetchMock = vi.fn<typeof fetch>();

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock);
    fetchMock.mockReset();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    setUnauthorizedHandler(null);
  });

  it('entrega os eventos, mesmo cortados entre pedaços, e reconecta quando o servidor fecha', async () => {
    fetchMock
      .mockResolvedValueOnce(
        streamResponse(['event:ready\ndata:\n\n', 'event:message-status\ndata:{"messageId":1,', '"conversationId":7,"status":"SENT"}\n\n']),
      )
      .mockResolvedValueOnce(streamResponse(['event:ready\ndata:\n\n']))
      .mockReturnValue(new Promise(() => {})); // terceira conexão fica aberta
    const events: StreamEvent[] = [];
    const connection: boolean[] = [];

    const close = connectEvents({ onEvent: (event) => events.push(event), onConnectionChange: (c) => connection.push(c) });

    await vi.waitFor(() => expect(events).toHaveLength(3), { timeout: 3000 });
    close();
    expect(events).toEqual([
      { type: 'ready', reconnected: false },
      { type: 'message-status', data: { messageId: 1, conversationId: 7, status: 'SENT' } },
      { type: 'ready', reconnected: true },
    ]);
    expect(connection.slice(0, 3)).toEqual([true, false, true]);
  });

  it('401: encerra a sessão e não tenta de novo', async () => {
    const onUnauthorized = vi.fn();
    setUnauthorizedHandler(onUnauthorized);
    fetchMock.mockResolvedValue(new Response(null, { status: 401 }));

    const close = connectEvents({ onEvent: vi.fn(), onConnectionChange: vi.fn() });

    await vi.waitFor(() => expect(onUnauthorized).toHaveBeenCalledOnce());
    await new Promise((resolve) => setTimeout(resolve, 50));
    close();
    expect(fetchMock).toHaveBeenCalledOnce();
  });
});

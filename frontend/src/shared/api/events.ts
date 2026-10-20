import { API_URL, notifyUnauthorized } from './http';
import { loadSession } from './session';
import type { MessageStatusEvent } from './types';

// Stream SSE do backend (GET /events). É lido com fetch, e não com EventSource, porque o EventSource
// não manda o header Authorization, e o token na URL acabaria em logs de acesso.

export type StreamEvent =
  /** Conexão aberta. `reconnected`: já houve uma antes, então eventos podem ter se perdido no meio. */
  | { type: 'ready'; reconnected: boolean }
  | { type: 'message-status'; data: MessageStatusEvent };

export type StreamListener = (event: StreamEvent) => void;

/** O que uma parte da tela usa do stream (ver `useEvents` em `app/appStore.ts`). */
export interface EventStream {
  /** false enquanto o stream está fora: quem depende dele volta a fazer polling. */
  connected: boolean;
  /** Recebe cada evento até chamar a função devolvida. */
  subscribe: (listener: StreamListener) => () => void;
}

interface RawEvent {
  event: string;
  data: string;
}

/** Separa os eventos completos (terminam numa linha em branco) do texto recebido; `rest` espera o próximo pedaço. */
export function parseSse(text: string): { events: RawEvent[]; rest: string } {
  const blocks = text.replace(/\r\n?/g, '\n').split('\n\n');
  const rest = blocks.pop() ?? '';
  const events = blocks.flatMap((block) => {
    let event = 'message';
    const data: string[] = [];
    for (const line of block.split('\n')) {
      if (line === '' || line.startsWith(':')) continue; // comentário (heartbeat)
      const separator = line.indexOf(':');
      const field = separator === -1 ? line : line.slice(0, separator);
      const value = separator === -1 ? '' : line.slice(separator + 1).replace(/^ /, '');
      if (field === 'event') event = value;
      if (field === 'data') data.push(value);
    }
    return data.length > 0 || event !== 'message' ? [{ event, data: data.join('\n') }] : [];
  });
  return { events, rest };
}

const MAX_RETRY_DELAY_MS = 30_000;

/** Espera entre reconexões: 1 s, 2 s, 4 s... até 30 s. */
export function retryDelay(attempt: number) {
  return Math.min(1000 * 2 ** attempt, MAX_RETRY_DELAY_MS);
}

interface Handlers {
  onEvent: (event: StreamEvent) => void;
  onConnectionChange: (connected: boolean) => void;
}

class StreamClosed extends Error {}

/**
 * Abre o stream e reconecta sozinho quando ele cai (backend reiniciou, conexão venceu).
 * Um 401 encerra a sessão e para de tentar. Devolve a função que fecha a conexão.
 */
export function connectEvents({ onEvent, onConnectionChange }: Handlers): () => void {
  const controller = new AbortController();
  let connectedBefore = false;

  async function run() {
    let attempt = 0;
    while (!controller.signal.aborted) {
      try {
        await readStream(controller.signal, (event) => {
          if (event.type === 'ready') {
            attempt = 0;
            onConnectionChange(true);
            onEvent({ type: 'ready', reconnected: connectedBefore });
            connectedBefore = true;
            return;
          }
          onEvent(event);
        });
      } catch (error) {
        if (controller.signal.aborted) return;
        if (error instanceof StreamClosed) return;
      }
      onConnectionChange(false);
      await wait(retryDelay(attempt++), controller.signal);
    }
  }

  void run();
  return () => controller.abort();
}

/**
 * Uma conexão: abre o GET /events, lê o corpo aos pedaços e repassa os eventos conhecidos.
 * Termina quando o servidor fecha (o `connectEvents` reconecta) e lança StreamClosed num 401.
 */
async function readStream(signal: AbortSignal, emit: (event: StreamEvent) => void) {
  const token = loadSession()?.token;
  const response = await fetch(`${API_URL}/events`, {
    headers: { Accept: 'text/event-stream', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    signal,
  });
  if (response.status === 401) {
    notifyUnauthorized();
    throw new StreamClosed();
  }
  if (!response.ok || !response.body) throw new Error(`Stream de eventos respondeu ${response.status}`);

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  for (;;) {
    const { value, done } = await reader.read();
    if (done) return; // o servidor fechou (timeout da conexão): o laço reconecta
    const parsed = parseSse(buffer + decoder.decode(value, { stream: true }));
    buffer = parsed.rest;
    for (const { event, data } of parsed.events) {
      if (event === 'ready') emit({ type: 'ready', reconnected: false });
      if (event === 'message-status') emit({ type: 'message-status', data: JSON.parse(data) as MessageStatusEvent });
    }
  }
}

function wait(ms: number, signal: AbortSignal) {
  return new Promise<void>((resolve) => {
    const timer = setTimeout(resolve, ms);
    signal.addEventListener('abort', () => {
      clearTimeout(timer);
      resolve();
    });
  });
}

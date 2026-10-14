import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { connectEvents, type StreamEvent } from '@/shared/api/events';

export type StreamListener = (event: StreamEvent) => void;

export interface EventStream {
  /** false enquanto o stream está fora: quem depende dele volta a fazer polling. */
  connected: boolean;
  /** Recebe cada evento até chamar a função devolvida. */
  subscribe: (listener: StreamListener) => () => void;
}

/** Uma conexão SSE para a tela logada inteira; cada parte da tela assina os eventos que lhe interessam. */
export function useEventStream(): EventStream {
  const listeners = useRef(new Set<StreamListener>());
  const [connected, setConnected] = useState(false);

  useEffect(
    () =>
      connectEvents({
        onEvent: (event) => listeners.current.forEach((listener) => listener(event)),
        onConnectionChange: setConnected,
      }),
    [],
  );

  const subscribe = useCallback((listener: StreamListener) => {
    listeners.current.add(listener);
    return () => {
      listeners.current.delete(listener);
    };
  }, []);

  return useMemo(() => ({ connected, subscribe }), [connected, subscribe]);
}

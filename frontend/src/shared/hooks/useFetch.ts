import { useCallback, useEffect, useState } from 'react';

export interface FetchState<T> {
  data: T | undefined;
  error: Error | null;
  /** true só na primeira carga: ao recarregar, os dados antigos continuam na tela. */
  isPending: boolean;
  reload: () => void;
  /** Troca os dados sem ir ao backend (ex.: saldo que já veio na resposta do envio). */
  setData: (update: (current: T | undefined) => T | undefined) => void;
}

/**
 * Busca dados ao montar e sempre que `fetcher` mudar.
 * O chamador cria o fetcher com useCallback, ex.: useCallback(() => getConversation(id), [id]).
 */
export function useFetch<T>(fetcher: () => Promise<T>): FetchState<T> {
  const [data, setData] = useState<T | undefined>(undefined);
  const [error, setError] = useState<Error | null>(null);
  // Mudar este número dispara o efeito de novo: é assim que o reload funciona.
  const [reloadCount, setReloadCount] = useState(0);

  useEffect(() => {
    // Se o efeito rodar de novo (id mudou, reload, componente saiu da tela) antes da resposta
    // chegar, a resposta antiga é descartada: evita mostrar dados da conversa errada.
    let ignore = false;

    fetcher()
      .then((result) => {
        if (ignore) return;
        setData(result);
        setError(null);
      })
      .catch((err: unknown) => {
        if (ignore) return;
        setError(err instanceof Error ? err : new Error('Algo deu errado.'));
      });

    return () => {
      ignore = true;
    };
  }, [fetcher, reloadCount]);

  const reload = useCallback(() => setReloadCount((count) => count + 1), []);

  return { data, error, isPending: data === undefined && error === null, reload, setData };
}

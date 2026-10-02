import { useState } from 'react';

/**
 * Executa uma ação disparada pelo usuário (login, cadastro, envio) e guarda
 * os estados de carregando e erro para a tela mostrar.
 */
export function useAction<Input, Output>(action: (input: Input) => Promise<Output>) {
  const [isPending, setIsPending] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  async function run(input: Input): Promise<Output> {
    setIsPending(true);
    setError(null);
    try {
      return await action(input);
    } catch (err) {
      setError(err instanceof Error ? err : new Error('Algo deu errado.'));
      // Relança para quem chamou saber que falhou (ex.: o composer só limpa o texto no sucesso).
      throw err;
    } finally {
      setIsPending(false);
    }
  }

  return { run, isPending, error };
}

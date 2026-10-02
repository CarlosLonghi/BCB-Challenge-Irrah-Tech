interface Props {
  error: unknown;
  onRetry?: () => void;
}

/** Caixa de erro padrão: mostra a mensagem da API (ApiError) ou uma genérica. */
export function ErrorMessage({ error, onRetry }: Props) {
  if (!error) return null;
  const message = error instanceof Error ? error.message : 'Algo deu errado.';
  return (
    <div className="alert alert--error" role="alert">
      <span>{message}</span>
      {onRetry ? (
        <button type="button" className="button button--ghost" onClick={onRetry}>
          Tentar de novo
        </button>
      ) : null}
    </div>
  );
}

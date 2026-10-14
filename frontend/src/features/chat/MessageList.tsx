import { Fragment, useLayoutEffect, useRef } from 'react';
import type { Message } from '@/shared/api/types';
import { ErrorMessage } from '@/shared/components/ErrorMessage';
import { Loading } from '@/shared/components/Loading';
import { formatDay } from '@/shared/utils/format';
import { MessageBubble } from './MessageBubble';
import styles from './MessageList.module.css';

interface Props {
  messages: Message[] | undefined;
  isPending: boolean;
  error: Error | null;
  onRetry: () => void;
  hasOlder: boolean;
  onLoadOlder: () => void;
  isLoadingOlder: boolean;
  olderError: Error | null;
}

/** Histórico da conversa, com carregando/erro/vazio, "carregar anteriores" e rolagem até a última mensagem. */
export function MessageList({
  messages,
  isPending,
  error,
  onRetry,
  hasOlder,
  onLoadOlder,
  isLoadingOlder,
  olderError,
}: Props) {
  const listRef = useRef<HTMLOListElement>(null);
  const previous = useRef<{ firstId?: number; lastId?: number; scrollHeight: number }>({ scrollHeight: 0 });

  const count = messages?.length ?? 0;
  const firstId = messages?.[0]?.id;
  const lastId = messages?.[count - 1]?.id;

  // Antes de pintar a tela, para não piscar. Rola só a lista (scrollIntoView rolaria a página também):
  // - mensagem nova no fim: vai até a última;
  // - mensagens antigas no topo ("carregar anteriores"): mantém na tela o que o usuário estava lendo;
  // - polling que só muda status: não mexe na rolagem.
  useLayoutEffect(() => {
    const list = listRef.current;
    if (!list) return;
    if (lastId !== previous.current.lastId) {
      list.scrollTop = list.scrollHeight;
    } else if (firstId !== previous.current.firstId) {
      list.scrollTop += list.scrollHeight - previous.current.scrollHeight;
    }
    previous.current = { firstId, lastId, scrollHeight: list.scrollHeight };
  }, [firstId, lastId]);

  return (
    <ol ref={listRef} className={styles.list}>
      {isPending ? (
        <li>
          <Loading text="Carregando mensagens..." />
        </li>
      ) : null}
      {/* Também cobre falha no polling: as mensagens já carregadas continuam na tela. */}
      {error ? (
        <li>
          <ErrorMessage error={error} onRetry={onRetry} />
        </li>
      ) : null}
      {hasOlder ? (
        <li className={styles.older}>
          <ErrorMessage error={olderError} />
          <button type="button" className="button button--ghost button--small" onClick={onLoadOlder} disabled={isLoadingOlder}>
            {isLoadingOlder ? 'Carregando...' : 'Carregar anteriores'}
          </button>
        </li>
      ) : null}
      {count === 0 && !isPending && !error ? <li className={styles.info}>Nenhuma mensagem nesta conversa.</li> : null}
      {messages?.map((message, index) => {
        // Separador de dia antes da primeira mensagem de cada dia.
        const day = formatDay(message.createdAt);
        const showDay = index === 0 || formatDay(messages[index - 1].createdAt) !== day;
        return (
          <Fragment key={message.id}>
            {showDay ? <li className={`mono ${styles.day}`}>{day}</li> : null}
            <MessageBubble message={message} />
          </Fragment>
        );
      })}
    </ol>
  );
}

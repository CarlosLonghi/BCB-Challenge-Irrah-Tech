import { Fragment, useEffect, useRef } from 'react';
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
}

/** Histórico da conversa, com carregando/erro/vazio e rolagem até a última mensagem. */
export function MessageList({ messages, isPending, error, onRetry }: Props) {
  const listRef = useRef<HTMLOListElement>(null);

  // Rola até o fim quando chega mensagem nova (depende só do tamanho, não do array inteiro,
  // para não rolar a cada polling que só muda status).
  const count = messages?.length ?? 0;
  // Rola só a lista: scrollIntoView também rolaria a página e os outros ancestrais.
  useEffect(() => {
    const list = listRef.current;
    if (list) list.scrollTop = list.scrollHeight;
  }, [count]);

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

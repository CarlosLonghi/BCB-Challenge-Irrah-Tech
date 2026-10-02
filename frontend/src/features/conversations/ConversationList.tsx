import { PlusIcon } from '@phosphor-icons/react';
import { Link } from 'react-router-dom';
import { ErrorMessage } from '@/shared/components/ErrorMessage';
import { Loading } from '@/shared/components/Loading';
import { useAppData } from '@/app/appDataContext';
import { ConversationItem } from './ConversationItem';
import styles from './ConversationList.module.css';

/** Coluna da esquerda: conversas do cliente, a mais recente primeiro (ordem do backend). */
export function ConversationList() {
  const { data: conversations, isPending, error, reload } = useAppData().conversations;

  return (
    <aside className={styles.list}>
      <div className={styles.top}>
        <div className={styles.title}>
          <h2>Conversas</h2>
          {conversations ? <span className={`mono ${styles.count}`}>{conversations.length}</span> : null}
        </div>
        <Link to="/conversas/nova" className="button button--small">
          <PlusIcon size={14} weight="bold" aria-hidden="true" />
          Nova conversa
        </Link>
      </div>

      {isPending ? <Loading text="Carregando conversas..." /> : null}
      {error ? <ErrorMessage error={error} onRetry={reload} /> : null}
      {conversations?.length === 0 ? (
        <p className={styles.info}>Nenhuma conversa ainda. Comece uma em "Nova conversa".</p>
      ) : null}

      <ul className={styles.items}>
        {conversations?.map((conversation) => (
          <li key={conversation.id}>
            <ConversationItem conversation={conversation} />
          </li>
        ))}
      </ul>
    </aside>
  );
}

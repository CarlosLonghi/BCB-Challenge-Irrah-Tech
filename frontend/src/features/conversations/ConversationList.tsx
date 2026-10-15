import { PlusIcon } from '@phosphor-icons/react';
import { Link } from 'react-router';
import { ErrorMessage } from '@/shared/components/ErrorMessage';
import { Loading } from '@/shared/components/Loading';
import { useConversations } from '@/app/appStore';
import { ConversationItem } from './ConversationItem';
import styles from './ConversationList.module.css';

/** Coluna da esquerda: conversas do cliente, a mais recente primeiro (ordem do backend). */
export function ConversationList() {
  const { data: conversations, isPending, error, reload, hasMore, loadMore, isLoadingMore, loadMoreError } =
    useConversations();

  return (
    <aside className={styles.list}>
      <div className={styles.top}>
        <div className={styles.title}>
          <h2>Conversas</h2>
          {conversations ? (
            <span className={`mono ${styles.count}`}>
              {conversations.length}
              {hasMore ? '+' : ''}
            </span>
          ) : null}
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
        {hasMore ? (
          <li className={styles.more}>
            <ErrorMessage error={loadMoreError} />
            <button type="button" className="button button--ghost button--small" onClick={loadMore} disabled={isLoadingMore}>
              {isLoadingMore ? 'Carregando...' : 'Carregar mais'}
            </button>
          </li>
        ) : null}
      </ul>
    </aside>
  );
}

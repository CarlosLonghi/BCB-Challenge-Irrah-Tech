import { NavLink } from 'react-router-dom';
import type { Conversation } from '@/shared/api/types';
import { Avatar } from '@/shared/components/Avatar';
import { formatShortTime } from '@/shared/utils/format';
import styles from './ConversationItem.module.css';

/** Uma linha da lista: avatar, nome, horário e prévia da última mensagem. */
export function ConversationItem({ conversation }: { conversation: Conversation }) {
  return (
    <NavLink
      to={`/conversas/${conversation.id}`}
      className={({ isActive }) => (isActive ? `${styles.item} ${styles.active}` : styles.item)}
    >
      <Avatar name={conversation.recipientName} />
      <div className={styles.body}>
        <div className={styles.line}>
          <span className={styles.name}>{conversation.recipientName}</span>
          {conversation.lastMessageTime ? (
            <time className={styles.time}>{formatShortTime(conversation.lastMessageTime)}</time>
          ) : null}
        </div>
        <p className={styles.preview}>{conversation.lastMessageContent ?? 'Sem mensagens'}</p>
      </div>
    </NavLink>
  );
}

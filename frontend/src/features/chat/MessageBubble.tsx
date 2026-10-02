import type { Message } from '@/shared/api/types';
import { formatMoney, formatTime } from '@/shared/utils/format';
import styles from './MessageBubble.module.css';
import { MessageStatusIcon } from './MessageStatusIcon';

/** Um balão de mensagem: tag de urgente, texto e a linha "horário · custo · status". */
export function MessageBubble({ message }: { message: Message }) {
  const failed = message.status === 'FAILED';
  return (
    <li className={failed ? `${styles.bubble} ${styles.failed}` : styles.bubble}>
      {message.priority === 'URGENT' ? <span className={`mono ${styles.urgent}`}>Urgente</span> : null}
      <p className={styles.content}>{message.content}</p>
      <span className={`mono ${styles.meta}`}>
        {formatTime(message.createdAt)}
        <span aria-hidden="true">·</span>
        {formatMoney(message.cost)}
        <span aria-hidden="true">·</span>
        <MessageStatusIcon status={message.status} />
      </span>
    </li>
  );
}

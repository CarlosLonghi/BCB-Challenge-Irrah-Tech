import { CheckIcon, ChecksIcon, CircleNotchIcon, ClockIcon, WarningCircleIcon, type Icon } from '@phosphor-icons/react';
import type { MessageStatus } from '@/shared/api/types';
import styles from './MessageStatusIcon.module.css';

// Ícone + texto de cada status. READ é simulado pelo backend alguns segundos depois da entrega.
const STATUS: Record<MessageStatus, { Icon: Icon; label: string }> = {
  QUEUED: { Icon: ClockIcon, label: 'Na fila' },
  PROCESSING: { Icon: CircleNotchIcon, label: 'Enviando' },
  SENT: { Icon: CheckIcon, label: 'Enviada' },
  DELIVERED: { Icon: ChecksIcon, label: 'Entregue' },
  READ: { Icon: ChecksIcon, label: 'Lida' },
  FAILED: { Icon: WarningCircleIcon, label: 'Falhou' },
};

/** Ícone e rótulo do status; a cor vem da classe com o nome do status no CSS Module. */
export function MessageStatusIcon({ status }: { status: MessageStatus }) {
  const { Icon, label } = STATUS[status];
  const weight = status === 'FAILED' ? 'fill' : 'bold';
  return (
    <span className={`${styles.status} ${styles[status.toLowerCase()]}`}>
      <Icon size={13} weight={weight} aria-hidden="true" />
      {label}
    </span>
  );
}

import type { Message, MessageStatus, Priority } from '@/shared/api/types';

// Regras de mensagem que a tela precisa conhecer (sem chamadas à API).

/** Custo fixo por prioridade (regra de negócio; o backend cobra o mesmo valor). */
export const PRIORITY_COST: Record<Priority, number> = {
  NORMAL: 0.25,
  URGENT: 0.5,
};

export const MAX_MESSAGE_LENGTH = 255;

/** Status que ainda vão mudar no backend; enquanto houver algum, a tela faz polling. */
const PENDING_STATUSES: MessageStatus[] = ['QUEUED', 'PROCESSING', 'SENT'];

export const POLLING_INTERVAL_MS = 3000;

export function hasPendingMessages(messages: Message[] | undefined) {
  return messages?.some((message) => PENDING_STATUSES.includes(message.status)) ?? false;
}

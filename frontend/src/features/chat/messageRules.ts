import type { Message, MessageStatus, Priority } from '@/shared/api/types';

// Regras de mensagem que a tela precisa conhecer (sem chamadas à API).

/** Custo fixo por prioridade, só para exibir antes do envio. Quem cobra é o backend (MessageService); mudou lá, mude aqui. */
export const PRIORITY_COST: Record<Priority, number> = {
  NORMAL: 0.25,
  URGENT: 0.5,
};

export const MAX_MESSAGE_LENGTH = 255;

/**
 * Status que ainda vão mudar no backend; enquanto houver algum e o stream de eventos estiver fora, a tela
 * faz polling. DELIVERED entra porque a leitura (READ) é simulada alguns segundos depois da entrega.
 */
const PENDING_STATUSES: MessageStatus[] = ['QUEUED', 'PROCESSING', 'SENT', 'DELIVERED'];

export const POLLING_INTERVAL_MS = 3000;

export function hasPendingMessages(messages: Message[] | undefined) {
  return messages?.some((message) => PENDING_STATUSES.includes(message.status)) ?? false;
}

import { PAGE_SIZE } from '@/shared/api/pagination';
import { request } from '@/shared/api/http';
import type { Message, SendMessageRequest, SendMessageResponse } from '@/shared/api/types';

/** As PAGE_SIZE mensagens mais recentes (ou as anteriores a `before`), em ordem crescente. */
export function listMessages(conversationId: number, before?: number) {
  const cursor = before === undefined ? '' : `&before=${before}`;
  return request<Message[]>(`/conversations/${conversationId}/messages?limit=${PAGE_SIZE}${cursor}`);
}

/** Debita e enfileira; a resposta já traz o saldo/limite atualizado (`currentBalance`). */
export function sendMessage(message: SendMessageRequest) {
  return request<SendMessageResponse>('/messages', { method: 'POST', body: message });
}

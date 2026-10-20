import { PAGE_SIZE } from '@/shared/api/pagination';
import { request } from '@/shared/api/http';
import type { Conversation } from '@/shared/api/types';

/** Conversas da mais recente para a mais antiga, PAGE_SIZE por página (a primeira é 0). */
export function listConversations(page = 0) {
  return request<Conversation[]>(`/conversations?page=${page}&size=${PAGE_SIZE}`);
}

/** Conversa de outro cliente responde 404, como se não existisse. */
export function getConversation(id: number) {
  return request<Conversation>(`/conversations/${id}`);
}

import { request } from '@/shared/api/http';
import type { Conversation } from '@/shared/api/types';

export function listConversations() {
  return request<Conversation[]>('/conversations');
}

export function getConversation(id: number) {
  return request<Conversation>(`/conversations/${id}`);
}

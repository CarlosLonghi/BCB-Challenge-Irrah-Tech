import { request } from '@/shared/api/http';
import type { Message, SendMessageRequest, SendMessageResponse } from '@/shared/api/types';

export function listMessages(conversationId: number) {
  return request<Message[]>(`/conversations/${conversationId}/messages`);
}

export function sendMessage(message: SendMessageRequest) {
  return request<SendMessageResponse>('/messages', { method: 'POST', body: message });
}

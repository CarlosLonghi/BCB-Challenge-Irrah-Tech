import { useCallback } from 'react';
import { useFetch } from '@/shared/hooks/useFetch';
import { getConversation } from './api';

/** Dados da conversa aberta (cabeçalho do chat); busca de novo quando o id muda. */
export function useConversation(id: number) {
  return useFetch(useCallback(() => getConversation(id), [id]));
}

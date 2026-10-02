import { useCallback } from 'react';
import { useFetch } from '@/shared/hooks/useFetch';
import { getConversation } from './api';

export function useConversation(id: number) {
  return useFetch(useCallback(() => getConversation(id), [id]));
}

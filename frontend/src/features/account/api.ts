import { request } from '@/shared/api/http';
import type { Balance } from '@/shared/api/types';

/** Saldo e limite atuais; o `useAppSession` busca de novo depois de um estorno (FAILED) e a cada reconexão do SSE. */
export function getBalance(clientId: number) {
  return request<Balance>(`/clients/${clientId}/balance`);
}

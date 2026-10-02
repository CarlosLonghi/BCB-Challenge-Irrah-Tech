import { request } from '@/shared/api/http';
import type { Balance } from '@/shared/api/types';

export function getBalance(clientId: number) {
  return request<Balance>(`/clients/${clientId}/balance`);
}

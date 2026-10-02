import { request } from '@/shared/api/http';
import type { AuthResponse, Client, NewClient } from '@/shared/api/types';

// Rotas públicas: não mandam token.

export function login(document: string) {
  return request<AuthResponse>('/auth', { method: 'POST', body: { document }, auth: false });
}

export function createClient(client: NewClient) {
  return request<Client>('/clients', { method: 'POST', body: client, auth: false });
}

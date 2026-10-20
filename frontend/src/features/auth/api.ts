import { request } from '@/shared/api/http';
import type { AuthResponse, Client, NewClient } from '@/shared/api/types';

// Rotas públicas: não mandam token.

export function login(document: string, password: string) {
  return request<AuthResponse>('/auth', { method: 'POST', body: { document, password }, auth: false });
}

/** Cadastro; saldo, limite e plano só são definidos aqui (não há rota para mudar depois). */
export function createClient(client: NewClient) {
  return request<Client>('/clients', { method: 'POST', body: client, auth: false });
}

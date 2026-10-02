import { loadSession } from './session';
import type { ApiErrorBody } from './types';

export const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

/** Erro de API com o status HTTP e a mensagem pronta para mostrar ao usuário. */
export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

// Mensagens usadas quando o backend não manda `message` no corpo.
const DEFAULT_MESSAGES: Record<number, string> = {
  401: 'Sessão expirada. Faça login novamente.',
  402: 'Saldo ou limite insuficiente para enviar a mensagem.',
  403: 'Acesso negado.',
  404: 'Registro não encontrado.',
};

// Quem sabe "deslogar" é o AuthProvider; ele se registra aqui.
let unauthorizedHandler: (() => void) | null = null;

export function setUnauthorizedHandler(handler: (() => void) | null) {
  unauthorizedHandler = handler;
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT';
  body?: unknown;
  /** false nas rotas públicas (POST /auth e POST /clients). */
  auth?: boolean;
}

/** Única função que fala com o backend: monta headers, trata erros e devolve o JSON tipado. */
export async function request<T>(path: string, { method = 'GET', body, auth = true }: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  const token = auth ? loadSession()?.token : undefined;
  if (token) headers.Authorization = `Bearer ${token}`;

  let response: Response;
  try {
    response = await fetch(`${API_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, 'Não foi possível conectar ao servidor. Verifique se o backend está no ar.');
  }

  if (!response.ok) {
    // Em rota autenticada, 401 = token inválido (ex.: backend reiniciou): encerra a sessão.
    if (response.status === 401 && auth) unauthorizedHandler?.();
    throw new ApiError(response.status, await errorMessage(response));
  }

  return (await response.json()) as T;
}

async function errorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as ApiErrorBody;
    if (body.message) return body.message;
  } catch {
    // corpo vazio ou não-JSON: cai na mensagem padrão
  }
  return DEFAULT_MESSAGES[response.status] ?? 'Algo deu errado. Tente novamente.';
}

import type { AuthResponse } from './types';

// Sessão = { token, client } devolvidos por POST /auth.
// Fica em memória e é copiada no localStorage para sobreviver a um F5.
// O localStorage pode estar indisponível (modo privado, bloqueio), por isso o try/catch.

export type Session = AuthResponse;

// Versão na chave: se o formato mudar, a sessão antiga é ignorada em vez de quebrar a tela.
const KEY = 'bcb.session:v1';

let current: Session | null = readStorage();

function readStorage(): Session | null {
  try {
    const raw = localStorage.getItem(KEY);
    return raw ? (JSON.parse(raw) as Session) : null;
  } catch {
    return null;
  }
}

export function loadSession(): Session | null {
  return current;
}

export function saveSession(session: Session) {
  current = session;
  try {
    localStorage.setItem(KEY, JSON.stringify(session));
  } catch {
    // segue só em memória
  }
}

export function clearSession() {
  current = null;
  try {
    localStorage.removeItem(KEY);
  } catch {
    // nada a fazer
  }
}

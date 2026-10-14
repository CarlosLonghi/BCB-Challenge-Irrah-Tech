import type { DocumentType } from '@/shared/api/types';

// Dígitos verificadores de CPF e CNPJ, com as mesmas regras do DocumentValidator do backend.
// Recebe só números (sem máscara).

const CNPJ_WEIGHTS = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];

/** Tamanho certo, só dígitos e não todos iguais (11111111111 passa na conta, mas não é válido). */
function hasValidFormat(document: string, length: number) {
  return document.length === length && /^\d+$/.test(document) && new Set(document).size > 1;
}

function digits(document: string) {
  return [...document].map(Number);
}

/** Pesos decrescentes de (length + 1) até 2 sobre os primeiros `length` dígitos. */
function cpfDigit(d: number[], length: number) {
  const sum = d.slice(0, length).reduce((acc, digit, i) => acc + digit * (length + 1 - i), 0);
  const remainder = (sum * 10) % 11;
  return remainder === 10 ? 0 : remainder;
}

/** Primeiro dígito usa os pesos 5,4,3,2,9..2; o segundo, 6,5,4,3,2,9..2. */
function cnpjDigit(d: number[], length: number) {
  const weights = CNPJ_WEIGHTS.slice(CNPJ_WEIGHTS.length - length);
  const sum = d.slice(0, length).reduce((acc, digit, i) => acc + digit * weights[i], 0);
  const remainder = sum % 11;
  return remainder < 2 ? 0 : 11 - remainder;
}

export function isValidCpf(cpf: string) {
  if (!hasValidFormat(cpf, 11)) return false;
  const d = digits(cpf);
  return cpfDigit(d, 9) === d[9] && cpfDigit(d, 10) === d[10];
}

export function isValidCnpj(cnpj: string) {
  if (!hasValidFormat(cnpj, 14)) return false;
  const d = digits(cnpj);
  return cnpjDigit(d, 12) === d[12] && cnpjDigit(d, 13) === d[13];
}

export function isValidDocument(type: DocumentType, document: string) {
  return type === 'CPF' ? isValidCpf(document) : isValidCnpj(document);
}

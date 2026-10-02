// Funções puras de formatação usadas pelas telas.

export function onlyDigits(value: string) {
  return value.replace(/\D/g, '');
}

/** Máscara de CPF (11 dígitos) ou CNPJ (até 14), aplicada enquanto o usuário digita. */
export function formatDocument(value: string) {
  const d = onlyDigits(value).slice(0, 14);
  if (d.length <= 11) {
    return d
      .replace(/^(\d{3})(\d)/, '$1.$2')
      .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
      .replace(/\.(\d{3})(\d{1,2})$/, '.$1-$2');
  }
  return d
    .replace(/^(\d{2})(\d)/, '$1.$2')
    .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d)/, '.$1/$2')
    .replace(/(\d{4})(\d{1,2})$/, '$1-$2');
}

const money = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function formatMoney(value: number) {
  return money.format(value);
}

/**
 * Converte data do backend (ISO sem fuso, LocalDateTime) em Date.
 * Sem fuso, o navegador interpreta como horário local, como diz o contrato.
 */
export function parseServerDate(value: string) {
  return new Date(value);
}

/** Hoje: "14:05". Outro dia: "29/09". */
export function formatShortTime(value: string) {
  const date = parseServerDate(value);
  const today = new Date();
  if (date.toDateString() === today.toDateString()) {
    return date.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
  }
  return date.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' });
}

export function formatTime(value: string) {
  return parseServerDate(value).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

/** Separador de dia no chat: "Hoje", "Ontem" ou "29/09/2026". */
export function formatDay(value: string, today = new Date()) {
  const date = parseServerDate(value);
  const yesterday = new Date(today);
  yesterday.setDate(today.getDate() - 1);
  if (date.toDateString() === today.toDateString()) return 'Hoje';
  if (date.toDateString() === yesterday.toDateString()) return 'Ontem';
  return date.toLocaleDateString('pt-BR');
}

/** "Maria Souza" → "MS"; "Empresa" → "E". */
export function initials(name: string) {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  const letters = parts.length > 1 ? parts[0][0] + parts[parts.length - 1][0] : (parts[0]?.[0] ?? '?');
  return letters.toUpperCase();
}

// Paginação das listas: o backend devolve no máximo PAGE_SIZE itens por chamada.
// Página cheia = pode haver mais; página menor = acabou.

export const PAGE_SIZE = 50;

export function isFullPage(page: unknown[] | undefined) {
  return page?.length === PAGE_SIZE;
}

/**
 * Junta páginas sem repetir itens: na primeira ocorrência de cada id fica a versão de `preferred`.
 * Usado quando uma página recarregada (ex.: polling) se sobrepõe a uma já carregada.
 */
export function mergeById<T extends { id: number }>(preferred: T[], others: T[]): T[] {
  const seen = new Set(preferred.map((item) => item.id));
  return [...preferred, ...others.filter((item) => !seen.has(item.id))];
}

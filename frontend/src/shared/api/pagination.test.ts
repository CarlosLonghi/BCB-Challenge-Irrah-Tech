import { describe, expect, it } from 'vitest';
import { PAGE_SIZE, isFullPage, mergeById } from './pagination';

describe('isFullPage', () => {
  it('só a página com PAGE_SIZE itens indica que pode haver mais', () => {
    expect(isFullPage(Array.from({ length: PAGE_SIZE }, (_, id) => id))).toBe(true);
    expect(isFullPage([1, 2])).toBe(false);
    expect(isFullPage([])).toBe(false);
    expect(isFullPage(undefined)).toBe(false);
  });
});

describe('mergeById', () => {
  it('mantém a versão preferida de cada id e acrescenta só os que faltam', () => {
    const preferred = [{ id: 2, v: 'novo' }, { id: 3, v: 'novo' }];
    const others = [{ id: 1, v: 'velho' }, { id: 2, v: 'velho' }];
    expect(mergeById(preferred, others)).toEqual([
      { id: 2, v: 'novo' },
      { id: 3, v: 'novo' },
      { id: 1, v: 'velho' },
    ]);
  });
});

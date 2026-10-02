import { describe, expect, it } from 'vitest';
import { formatDay, formatDocument, initials, onlyDigits } from './format';

describe('onlyDigits', () => {
  it('remove pontos, traços, barras e letras', () => {
    expect(onlyDigits('123.456.789-01')).toBe('12345678901');
    expect(onlyDigits('12.345.678/0001-90')).toBe('12345678000190');
    expect(onlyDigits('abc')).toBe('');
  });
});

describe('formatDocument', () => {
  it('aplica a máscara de CPF enquanto o usuário digita', () => {
    expect(formatDocument('123')).toBe('123');
    expect(formatDocument('1234')).toBe('123.4');
    expect(formatDocument('1234567')).toBe('123.456.7');
    expect(formatDocument('12345678901')).toBe('123.456.789-01');
  });

  it('passa para a máscara de CNPJ a partir do 12º dígito', () => {
    expect(formatDocument('123456789012')).toBe('12.345.678/9012');
    expect(formatDocument('12345678000190')).toBe('12.345.678/0001-90');
  });

  it('ignora o que não é dígito e corta depois de 14 dígitos', () => {
    expect(formatDocument('123.456.789-01')).toBe('123.456.789-01');
    expect(formatDocument('1234567800019099')).toBe('12.345.678/0001-90');
  });
});

describe('initials', () => {
  it('usa a primeira letra do primeiro e do último nome', () => {
    expect(initials('Maria Souza')).toBe('MS');
    expect(initials('  ana maria da silva  ')).toBe('AS');
  });

  it('usa uma letra para nome único e "?" para nome vazio', () => {
    expect(initials('Empresa')).toBe('E');
    expect(initials('   ')).toBe('?');
  });
});

describe('formatDay', () => {
  const today = new Date(2026, 9, 2, 15, 0);

  it('usa "Hoje" e "Ontem" para os dois últimos dias e a data para os outros', () => {
    expect(formatDay('2026-10-02T09:12:00', today)).toBe('Hoje');
    expect(formatDay('2026-10-01T23:59:00', today)).toBe('Ontem');
    expect(formatDay('2026-09-29T10:00:00', today)).toBe('29/09/2026');
  });
});

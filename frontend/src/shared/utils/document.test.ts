import { describe, expect, it } from 'vitest';
import { isValidCnpj, isValidCpf, isValidDocument } from './document';

describe('isValidCpf', () => {
  it.each(['12345678909', '52998224725', '11144477735', '98765432100'])('aceita %s', (cpf) => {
    expect(isValidCpf(cpf)).toBe(true);
  });

  it.each(['12345678901', '52998224724', '11111111111', '00000000000', '1234567890', '123456789090', '1234567890a', '123.456.789-09', ''])(
    'recusa %s',
    (cpf) => {
      expect(isValidCpf(cpf)).toBe(false);
    },
  );
});

describe('isValidCnpj', () => {
  it.each(['11222333000181', '11444777000161', '04252011000110'])('aceita %s', (cnpj) => {
    expect(isValidCnpj(cnpj)).toBe(true);
  });

  it.each(['11222333000182', '11222333000191', '12345678000199', '22222222222222', '1122233300018', '1122233300018a', '11.222.333/0001-81', ''])(
    'recusa %s',
    (cnpj) => {
      expect(isValidCnpj(cnpj)).toBe(false);
    },
  );
});

describe('isValidDocument', () => {
  it('escolhe a regra pelo tipo', () => {
    expect(isValidDocument('CPF', '12345678909')).toBe(true);
    expect(isValidDocument('CNPJ', '11222333000181')).toBe(true);
    expect(isValidDocument('CPF', '11222333000181')).toBe(false);
    expect(isValidDocument('CNPJ', '12345678909')).toBe(false);
  });
});

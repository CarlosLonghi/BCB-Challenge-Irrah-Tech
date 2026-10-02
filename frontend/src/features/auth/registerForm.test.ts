import { describe, expect, it } from 'vitest';
import { INITIAL_REGISTER_FORM, toNewClient, validateRegisterForm, type RegisterForm } from './registerForm';

const valid: RegisterForm = { ...INITIAL_REGISTER_FORM, name: 'Empresa X', document: '123.456.789-01' };

describe('validateRegisterForm', () => {
  it('aceita um formulário válido', () => {
    expect(validateRegisterForm(valid)).toEqual([]);
  });

  it('confere o tamanho do documento pelo tipo', () => {
    expect(validateRegisterForm({ ...valid, documentType: 'CNPJ' })).toEqual(['O CNPJ deve ter 14 dígitos.']);
  });

  it('recusa nome vazio e valor negativo', () => {
    expect(validateRegisterForm({ ...valid, name: ' ', amount: '-1' })).toEqual([
      'Informe o nome.',
      'O saldo deve ser zero ou mais.',
    ]);
  });
});

describe('toNewClient', () => {
  it('pós-pago manda o valor como limite e saldo 0', () => {
    expect(toNewClient({ ...valid, planType: 'POST_PAID', amount: '50,5' })).toMatchObject({
      document: '12345678901',
      balance: 0,
      limit: 50.5,
    });
  });
});

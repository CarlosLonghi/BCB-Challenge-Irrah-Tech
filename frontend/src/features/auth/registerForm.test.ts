import { describe, expect, it } from 'vitest';
import { INITIAL_REGISTER_FORM, toNewClient, validateRegisterForm, type RegisterForm } from './registerForm';

const valid: RegisterForm = {
  ...INITIAL_REGISTER_FORM,
  name: 'Empresa X',
  document: '123.456.789-09',
  password: 'senha-forte-123',
  passwordConfirmation: 'senha-forte-123',
};

describe('validateRegisterForm', () => {
  it('aceita um formulário válido', () => {
    expect(validateRegisterForm(valid)).toEqual([]);
  });

  it('confere o tamanho do documento pelo tipo', () => {
    expect(validateRegisterForm({ ...valid, documentType: 'CNPJ' })).toEqual(['O CNPJ deve ter 14 dígitos.']);
  });

  it('confere os dígitos verificadores do CPF e do CNPJ', () => {
    expect(validateRegisterForm({ ...valid, document: '123.456.789-01' })).toEqual(['CPF inválido.']);
    expect(validateRegisterForm({ ...valid, documentType: 'CNPJ', document: '11.222.333/0001-82' })).toEqual([
      'CNPJ inválido.',
    ]);
    expect(validateRegisterForm({ ...valid, documentType: 'CNPJ', document: '11.222.333/0001-81' })).toEqual([]);
  });

  it('recusa nome vazio e valor negativo', () => {
    expect(validateRegisterForm({ ...valid, name: ' ', amount: '-1' })).toEqual([
      'Informe o nome.',
      'O saldo deve ser zero ou mais.',
    ]);
  });
});

describe('validateRegisterForm: senha', () => {
  it('exige de 8 a 72 caracteres', () => {
    const short = { ...valid, password: '1234567', passwordConfirmation: '1234567' };
    const long = { ...valid, password: 'a'.repeat(73), passwordConfirmation: 'a'.repeat(73) };
    expect(validateRegisterForm(short)).toEqual(['A senha deve ter entre 8 e 72 caracteres.']);
    expect(validateRegisterForm(long)).toEqual(['A senha deve ter entre 8 e 72 caracteres.']);
  });

  it('exige que a confirmação seja igual à senha', () => {
    expect(validateRegisterForm({ ...valid, passwordConfirmation: 'outra-senha' })).toEqual([
      'As senhas não conferem.',
    ]);
  });
});

describe('toNewClient', () => {
  it('pós-pago manda o valor como limite e saldo 0', () => {
    expect(toNewClient({ ...valid, planType: 'POST_PAID', amount: '50,5' })).toMatchObject({
      document: '12345678909',
      balance: 0,
      limit: 50.5,
      password: 'senha-forte-123',
    });
  });

  it('não manda a confirmação de senha para a API', () => {
    expect(toNewClient(valid)).not.toHaveProperty('passwordConfirmation');
  });
});

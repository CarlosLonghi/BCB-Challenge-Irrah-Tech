import type { DocumentType, NewClient, PlanType } from '@/shared/api/types';
import { isValidDocument } from '@/shared/utils/document';
import { onlyDigits } from '@/shared/utils/format';

// Regras do formulário de cadastro, separadas da tela para poder testar sem renderizar.

export interface RegisterForm {
  name: string;
  document: string;
  password: string;
  passwordConfirmation: string;
  documentType: DocumentType;
  planType: PlanType;
  /** Saldo inicial (pré-pago) ou limite mensal (pós-pago), conforme o plano. */
  amount: string;
}

export const INITIAL_REGISTER_FORM: RegisterForm = {
  name: '',
  document: '',
  password: '',
  passwordConfirmation: '',
  documentType: 'CPF',
  planType: 'PRE_PAID',
  amount: '10',
};

const DOCUMENT_LENGTH: Record<DocumentType, number> = { CPF: 11, CNPJ: 14 };

// Mesmo intervalo do backend; 72 é o limite do BCrypt.
export const PASSWORD_MIN_LENGTH = 8;
export const PASSWORD_MAX_LENGTH = 72;

/** Aceita "10,50" e "10.50". */
function parseAmount(value: string) {
  return value.trim() === '' ? NaN : Number(value.replace(',', '.'));
}

/** Mesmas regras que o backend valida em POST /clients; devolve a lista de problemas. */
export function validateRegisterForm(form: RegisterForm): string[] {
  const errors: string[] = [];
  const name = form.name.trim();
  if (!name) errors.push('Informe o nome.');
  if (name.length > 150) errors.push('O nome pode ter até 150 caracteres.');

  const document = onlyDigits(form.document);
  const expected = DOCUMENT_LENGTH[form.documentType];
  if (document.length !== expected) {
    errors.push(`O ${form.documentType} deve ter ${expected} dígitos.`);
  } else if (!isValidDocument(form.documentType, document)) {
    errors.push(`${form.documentType} inválido.`);
  }

  if (form.password.length < PASSWORD_MIN_LENGTH || form.password.length > PASSWORD_MAX_LENGTH) {
    errors.push(`A senha deve ter entre ${PASSWORD_MIN_LENGTH} e ${PASSWORD_MAX_LENGTH} caracteres.`);
  } else if (form.password !== form.passwordConfirmation) {
    errors.push('As senhas não conferem.');
  }

  const amount = parseAmount(form.amount);
  if (Number.isNaN(amount) || amount < 0) {
    errors.push(form.planType === 'PRE_PAID' ? 'O saldo deve ser zero ou mais.' : 'O limite deve ser zero ou mais.');
  }
  return errors;
}

/** Converte o formulário no corpo de POST /clients (o campo que não se aplica ao plano vai 0). */
export function toNewClient(form: RegisterForm): NewClient {
  const amount = parseAmount(form.amount);
  const prePaid = form.planType === 'PRE_PAID';
  return {
    name: form.name.trim(),
    document: onlyDigits(form.document),
    password: form.password,
    documentType: form.documentType,
    planType: form.planType,
    balance: prePaid ? amount : 0,
    limit: prePaid ? 0 : amount,
    active: true,
  };
}

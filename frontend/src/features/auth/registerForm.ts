import type { DocumentType, NewClient, PlanType } from '@/shared/api/types';
import { onlyDigits } from '@/shared/utils/format';

// Regras do formulário de cadastro, separadas da tela para poder testar sem renderizar.

export interface RegisterForm {
  name: string;
  document: string;
  documentType: DocumentType;
  planType: PlanType;
  /** Saldo inicial (pré-pago) ou limite mensal (pós-pago), conforme o plano. */
  amount: string;
}

export const INITIAL_REGISTER_FORM: RegisterForm = {
  name: '',
  document: '',
  documentType: 'CPF',
  planType: 'PRE_PAID',
  amount: '10',
};

const DOCUMENT_LENGTH: Record<DocumentType, number> = { CPF: 11, CNPJ: 14 };

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

  const expected = DOCUMENT_LENGTH[form.documentType];
  if (onlyDigits(form.document).length !== expected) {
    errors.push(`O ${form.documentType} deve ter ${expected} dígitos.`);
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
    documentType: form.documentType,
    planType: form.planType,
    balance: prePaid ? amount : 0,
    limit: prePaid ? 0 : amount,
    active: true,
  };
}

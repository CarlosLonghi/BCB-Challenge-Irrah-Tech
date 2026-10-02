import { useState, type FormEvent } from 'react';
import { Link, Navigate } from 'react-router-dom';
import type { DocumentType, NewClient, PlanType } from '@/shared/api/types';
import { ErrorMessage } from '@/shared/components/ErrorMessage';
import { useAction } from '@/shared/hooks/useAction';
import { SegmentedControl, type SegmentedOption } from '@/shared/components/SegmentedControl';
import { formatDocument } from '@/shared/utils/format';
import { createClient, login } from './api';
import { AuthCard } from './AuthCard';
import { INITIAL_REGISTER_FORM, toNewClient, validateRegisterForm, type RegisterForm } from './registerForm';
import { useAuth } from './useAuth';

const PLAN_OPTIONS: SegmentedOption<PlanType>[] = [
  { value: 'PRE_PAID', label: 'Pré-pago' },
  { value: 'POST_PAID', label: 'Pós-pago' },
];

export function RegisterPage() {
  const { session, signIn } = useAuth();
  const [form, setForm] = useState<RegisterForm>(INITIAL_REGISTER_FORM);
  const [errors, setErrors] = useState<string[]>([]);

  // Cadastra e já entra: quem acabou de se cadastrar não precisa redigitar o documento.
  // As duas chamadas são sequenciais de propósito: o login depende do cliente criado.
  const register = useAction(async (client: NewClient) => {
    const created = await createClient(client);
    signIn(await login(created.document));
  });

  if (session) return <Navigate to="/" replace />;

  function update<K extends keyof RegisterForm>(field: K, value: RegisterForm[K]) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  // Trocar CPF ↔ CNPJ limpa o documento: o que foi digitado era para o outro tipo.
  function changeDocumentType(documentType: DocumentType) {
    setForm((current) => ({ ...current, documentType, document: '' }));
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const problems = validateRegisterForm(form);
    setErrors(problems);
    // O erro já fica em register.error; o catch só evita o aviso de promise rejeitada.
    if (problems.length === 0) register.run(toNewClient(form)).catch(() => {});
  }

  const prePaid = form.planType === 'PRE_PAID';

  return (
    <AuthCard
      eyebrow="Novo cliente"
      title="Cadastrar cliente"
      subtitle="Crie a conta da empresa para começar a enviar mensagens."
      onSubmit={handleSubmit}
      footer={
        <>
          <span>Já tem cadastro?</span>
          <Link to="/login">Entrar →</Link>
        </>
      }
    >
      <label>
        Nome
        <input value={form.name} onChange={(e) => update('name', e.target.value)} maxLength={150} autoFocus />
      </label>

      <div className="form-row">
        <label>
          Tipo
          <select value={form.documentType} onChange={(e) => changeDocumentType(e.target.value as DocumentType)}>
            <option value="CPF">CPF</option>
            <option value="CNPJ">CNPJ</option>
          </select>
        </label>
        <label className="form-row__grow">
          Documento
          <input
            value={formatDocument(form.document)}
            onChange={(e) => update('document', e.target.value)}
            className="mono"
            inputMode="numeric"
            placeholder={form.documentType === 'CPF' ? '000.000.000-00' : '00.000.000/0000-00'}
          />
        </label>
      </div>

      <SegmentedControl
        name="planType"
        legend="Plano"
        options={PLAN_OPTIONS}
        value={form.planType}
        onChange={(planType) => update('planType', planType)}
      />

      <label>
        {prePaid ? 'Saldo inicial (R$)' : 'Limite mensal (R$)'}
        <input
          className="mono"
          value={form.amount}
          onChange={(e) => update('amount', e.target.value)}
          inputMode="decimal"
        />
      </label>

      {errors.length > 0 ? (
        <ul className="alert alert--error" role="alert">
          {errors.map((error) => (
            <li key={error}>{error}</li>
          ))}
        </ul>
      ) : null}
      <ErrorMessage error={register.error} />

      <button className="button button--large" type="submit" disabled={register.isPending}>
        {register.isPending ? 'Cadastrando...' : 'Cadastrar e entrar'}
      </button>
    </AuthCard>
  );
}

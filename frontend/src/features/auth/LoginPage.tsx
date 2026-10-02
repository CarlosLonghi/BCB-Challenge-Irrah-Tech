import { useState, type FormEvent } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { useAction } from '@/shared/hooks/useAction';
import { ErrorMessage } from '@/shared/components/ErrorMessage';
import { formatDocument, onlyDigits } from '@/shared/utils/format';
import { login } from './api';
import { AuthCard } from './AuthCard';
import { useAuth } from './useAuth';

export function LoginPage() {
  const { session, expired, signIn } = useAuth();
  const [document, setDocument] = useState('');
  const loginAction = useAction(async (digits: string) => signIn(await login(digits)));

  if (session) return <Navigate to="/" replace />;

  const digits = onlyDigits(document);
  const isValid = digits.length === 11 || digits.length === 14;

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    // O erro já fica em loginAction.error para a tela mostrar; aqui só evitamos o aviso de promise rejeitada.
    if (isValid) loginAction.run(digits).catch(() => {});
  }

  return (
    <AuthCard
      eyebrow="Acesso do cliente"
      title="Entrar"
      subtitle="Use o CPF ou CNPJ cadastrado da empresa."
      onSubmit={handleSubmit}
      footer={
        <>
          <span>Ainda não tem cadastro?</span>
          <Link to="/cadastro">Cadastrar cliente →</Link>
        </>
      }
    >
      {expired && !loginAction.error ? (
        <p className="alert alert--info" role="status">
          Sua sessão expirou. Entre novamente.
        </p>
      ) : null}

      <label>
        CPF ou CNPJ
        <input
          value={formatDocument(document)}
          onChange={(e) => setDocument(e.target.value)}
          className="mono"
          inputMode="numeric"
          placeholder="000.000.000-00"
          autoFocus
        />
      </label>

      <ErrorMessage error={loginAction.error} />

      <button className="button button--large" type="submit" disabled={!isValid || loginAction.isPending}>
        {loginAction.isPending ? 'Entrando...' : 'Entrar'}
      </button>
    </AuthCard>
  );
}

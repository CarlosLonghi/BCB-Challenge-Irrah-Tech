import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/shared/api/http';
import { clearSession, loadSession } from '@/shared/api/session';
import type { AuthResponse } from '@/shared/api/types';
import { login } from './api';
import { AuthProvider } from './AuthProvider';
import { LoginPage } from './LoginPage';

vi.mock('./api', () => ({ login: vi.fn() }));

const SESSION: AuthResponse = {
  token: 'jwt',
  client: {
    id: 1, name: 'Empresa X', document: '52998224725', documentType: 'CPF',
    planType: 'PRE_PAID', balance: 10, limit: 0, active: true,
  },
};

function renderLogin() {
  render(
    <MemoryRouter initialEntries={['/login']}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<p>Tela logada</p>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.mocked(login).mockReset();
  });

  afterEach(() => {
    clearSession();
  });

  it('só libera "Entrar" com documento completo e senha; envia só os dígitos e abre a tela logada', async () => {
    vi.mocked(login).mockResolvedValue(SESSION);
    renderLogin();
    const submit = screen.getByRole('button', { name: 'Entrar' });

    await userEvent.type(screen.getByLabelText('CPF ou CNPJ'), '529.982.247-25');
    expect(submit).toBeDisabled();
    await userEvent.type(screen.getByLabelText('Senha'), 'senha-forte-123');
    await userEvent.click(submit);

    expect(await screen.findByText('Tela logada')).toBeInTheDocument();
    expect(login).toHaveBeenCalledWith('52998224725', 'senha-forte-123');
    expect(loadSession()?.token).toBe('jwt');
  });

  it('credenciais erradas: mostra a mensagem do backend e continua no login', async () => {
    vi.mocked(login).mockRejectedValue(new ApiError(401, 'Documento ou senha inválidos.'));
    renderLogin();

    await userEvent.type(screen.getByLabelText('CPF ou CNPJ'), '52998224725');
    await userEvent.type(screen.getByLabelText('Senha'), 'errada-123');
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Documento ou senha inválidos.');
    expect(screen.queryByText('Tela logada')).not.toBeInTheDocument();
    expect(loadSession()).toBeNull();
  });
});

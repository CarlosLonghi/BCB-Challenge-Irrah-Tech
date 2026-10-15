import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { emit, useAppStore } from '@/app/appStore';
import { useAuthStore } from '@/features/auth/authStore';
import { getConversation } from '@/features/conversations/api';
import type { StreamEvent } from '@/shared/api/events';
import type { AuthResponse, Message } from '@/shared/api/types';
import { listMessages, sendMessage } from './api';
import { ChatPage } from './ChatPage';

vi.mock('./api', () => ({ listMessages: vi.fn(), sendMessage: vi.fn() }));
vi.mock('@/features/conversations/api', () => ({ getConversation: vi.fn() }));

const SESSION: AuthResponse = {
  token: 'jwt',
  client: {
    id: 1, name: 'Empresa X', document: '52998224725', documentType: 'CPF',
    planType: 'PRE_PAID', balance: 10, limit: 0, active: true,
  },
};

const MESSAGE: Message = {
  id: 3, conversationId: 7, senderId: 1, recipientId: 55, content: 'Olá, Maria',
  createdAt: '2026-10-14T12:00:00Z', priority: 'NORMAL', status: 'QUEUED', cost: 0.25,
};

function renderChat() {
  // Os stores são globais: o teste define o estado e troca as ações do envio por espiões.
  const setBalance = vi.fn();
  const reloadConversations = vi.fn();
  useAuthStore.setState({ session: SESSION, expired: false });
  useAppStore.setState({ connected: true, setBalance, reloadConversations });

  render(
    <MemoryRouter initialEntries={['/conversas/7']}>
      <Routes>
        <Route path="/conversas/:id" element={<ChatPage />} />
      </Routes>
    </MemoryRouter>,
  );

  return { setBalance, reloadConversations, emit: (event: StreamEvent) => act(() => emit(event)) };
}

describe('ChatPage', () => {
  beforeEach(() => {
    // Reset no início, não no afterEach: o afterEach do arquivo roda antes do cleanup do setup,
    // e a tela ainda montada quebraria ao perder a sessão.
    useAppStore.setState(useAppStore.getInitialState(), true);
    useAuthStore.setState(useAuthStore.getInitialState(), true);
    vi.mocked(listMessages).mockReset().mockResolvedValue([MESSAGE]);
    vi.mocked(sendMessage).mockReset();
    vi.mocked(getConversation).mockResolvedValue({
      id: 7, clientId: 1, recipientId: 55, recipientName: 'Maria Souza',
      lastMessageContent: 'Olá, Maria', lastMessageTime: '2026-10-14T12:00:00Z', unreadCount: 0,
    });
  });

  it('mostra destinatário e histórico, e o status muda quando chega o evento', async () => {
    const { emit } = renderChat();

    expect(await screen.findByRole('heading', { name: 'Maria Souza' })).toBeInTheDocument();
    expect(await screen.findByText('Olá, Maria')).toBeInTheDocument();
    expect(screen.getByText('Na fila')).toBeInTheDocument();

    emit({ type: 'message-status', data: { messageId: 3, conversationId: 7, status: 'DELIVERED' } });

    expect(await screen.findByText('Entregue')).toBeInTheDocument();
    expect(screen.queryByText('Na fila')).not.toBeInTheDocument();
    expect(listMessages).toHaveBeenCalledOnce();
  });

  it('envia na conversa aberta, atualiza o saldo com a resposta e recarrega conversas e histórico', async () => {
    vi.mocked(sendMessage).mockResolvedValue({
      id: 4, conversationId: 7, status: 'QUEUED', estimatedDelivery: '2026-10-14T12:00:30Z', cost: 0.5, currentBalance: 9.5,
    });
    const { setBalance, reloadConversations } = renderChat();
    await screen.findByText('Olá, Maria');

    await userEvent.type(screen.getByRole('textbox', { name: 'Mensagem' }), 'Urgente!');
    await userEvent.click(screen.getByRole('radio', { name: /Urgente/ }));
    await userEvent.click(screen.getByRole('button', { name: /Enviar/ }));

    expect(sendMessage).toHaveBeenCalledWith({ conversationId: 7, content: 'Urgente!', priority: 'URGENT' });
    expect(reloadConversations).toHaveBeenCalledOnce();
    const updateBalance = setBalance.mock.calls[0][0];
    expect(updateBalance({ balance: 10, limit: 0 })).toEqual({ balance: 9.5, limit: 0 });
    expect(listMessages).toHaveBeenCalledTimes(2);
    expect(screen.getByRole('textbox', { name: 'Mensagem' })).toHaveValue('');
  });
});

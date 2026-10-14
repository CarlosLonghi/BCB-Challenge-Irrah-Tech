import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { Message } from '@/shared/api/types';
import { MessageList } from './MessageList';

function message(id: number): Message {
  return {
    id, conversationId: 7, senderId: 1, recipientId: 55, content: `Mensagem ${id}`,
    createdAt: '2026-10-14T12:00:00Z', priority: 'NORMAL', status: 'READ', cost: 0.25,
  };
}

const baseProps = {
  isPending: false,
  error: null,
  onRetry: () => {},
  isLoadingOlder: false,
  olderError: null,
};

describe('MessageList', () => {
  it('mostra "Carregar anteriores" quando há mensagens mais antigas e chama o callback', async () => {
    const onLoadOlder = vi.fn();
    render(<MessageList {...baseProps} messages={[message(51), message(52)]} hasOlder onLoadOlder={onLoadOlder} />);

    await userEvent.click(screen.getByRole('button', { name: 'Carregar anteriores' }));

    expect(onLoadOlder).toHaveBeenCalledOnce();
  });

  it('sem mensagens mais antigas, não mostra o botão', () => {
    render(<MessageList {...baseProps} messages={[message(1)]} hasOlder={false} onLoadOlder={() => {}} />);

    expect(screen.queryByRole('button', { name: 'Carregar anteriores' })).not.toBeInTheDocument();
  });

  it('enquanto carrega, o botão fica desabilitado', () => {
    render(<MessageList {...baseProps} messages={[message(51)]} hasOlder isLoadingOlder onLoadOlder={() => {}} />);

    expect(screen.getByRole('button', { name: 'Carregando...' })).toBeDisabled();
  });
});

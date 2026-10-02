import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { MessageComposer } from './MessageComposer';

describe('MessageComposer', () => {
  it('mostra o custo da prioridade escolhida e envia com ela', async () => {
    const onSend = vi.fn().mockResolvedValue(undefined);
    render(<MessageComposer onSend={onSend} isSending={false} />);

    await userEvent.type(screen.getByLabelText('Mensagem'), 'Olá');
    expect(screen.getByRole('button', { name: /Enviar\s*R\$\s0,25/ })).toBeEnabled();

    await userEvent.click(screen.getByLabelText(/Urgente/));
    await userEvent.click(screen.getByRole('button', { name: /Enviar\s*R\$\s0,50/ }));

    expect(onSend).toHaveBeenCalledWith('Olá', 'URGENT');
    expect(screen.getByLabelText('Mensagem')).toHaveValue('');
  });

  it('mantém o texto quando o envio falha', async () => {
    const onSend = vi.fn().mockRejectedValue(new Error('Saldo insuficiente.'));
    render(<MessageComposer onSend={onSend} isSending={false} />);

    await userEvent.type(screen.getByLabelText('Mensagem'), 'Olá{Enter}');

    expect(onSend).toHaveBeenCalledOnce();
    expect(screen.getByLabelText('Mensagem')).toHaveValue('Olá');
  });
});

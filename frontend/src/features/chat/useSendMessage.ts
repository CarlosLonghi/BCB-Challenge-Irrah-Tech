import { useAppData } from '@/app/appDataContext';
import type { Balance, PlanType, SendMessageRequest } from '@/shared/api/types';
import { useAction } from '@/shared/hooks/useAction';
import { sendMessage } from './api';

/**
 * Envia uma mensagem e mantém a tela consistente:
 * 1. atualiza o saldo/limite do cabeçalho com `currentBalance` (sem nova chamada);
 * 2. recarrega a lista de conversas (a conversa sobe para o topo);
 * 3. avisa quem chamou (`onSent`), que cuida do próprio histórico.
 */
export function useSendMessage(planType: PlanType, onSent?: () => void) {
  const { balance, conversations } = useAppData();

  return useAction(async (message: SendMessageRequest) => {
    const response = await sendMessage(message);
    balance.setData((old) => old && withCurrentBalance(old, planType, response.currentBalance));
    conversations.reload();
    onSent?.();
    return response;
  });
}

/** Pré-pago: `currentBalance` é o novo saldo. Pós-pago: é o limite restante. */
function withCurrentBalance(old: Balance, planType: PlanType, currentBalance: number): Balance {
  return planType === 'PRE_PAID' ? { ...old, balance: currentBalance } : { ...old, limit: currentBalance };
}

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSession } from '@/features/auth/useAuth';
import { MessageComposer } from '@/features/chat/MessageComposer';
import { useSendMessage } from '@/features/chat/useSendMessage';
import type { Priority } from '@/shared/api/types';
import { BackLink } from '@/shared/components/BackLink';
import { ErrorMessage } from '@/shared/components/ErrorMessage';
import styles from './NewConversationPage.module.css';

/**
 * A API não tem "criar conversa": ela nasce no primeiro POST /messages sem conversationId.
 * Por isso a tela pede destinatário + primeira mensagem e, ao enviar,
 * abre o chat com o conversationId devolvido.
 */
export function NewConversationPage() {
  const session = useSession();
  const navigate = useNavigate();
  const send = useSendMessage(session.client.planType);
  const [recipientId, setRecipientId] = useState('');
  const [recipientName, setRecipientName] = useState('');

  const idNumber = Number(recipientId);
  const recipientIsValid = Number.isInteger(idNumber) && idNumber > 0 && recipientName.trim() !== '';

  async function handleSend(content: string, priority: Priority) {
    const response = await send.run({
      recipientId: idNumber,
      recipientName: recipientName.trim(),
      content,
      priority,
    });
    navigate(`/conversas/${response.conversationId}`, { replace: true });
  }

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <BackLink />
        <h2>Nova conversa</h2>
      </div>

      <div className={styles.body}>
        <div className={`form-row ${styles.recipient}`}>
          <label>
            ID do destinatário
            <input
              value={recipientId}
              onChange={(e) => setRecipientId(e.target.value.replace(/\D/g, ''))}
              className="mono"
              inputMode="numeric"
              placeholder="55"
              autoFocus
            />
          </label>
          <label className="form-row__grow">
            Nome do destinatário
            <input value={recipientName} onChange={(e) => setRecipientName(e.target.value)} placeholder="Maria Souza" />
          </label>
        </div>

        <ErrorMessage error={send.error} />
        <MessageComposer onSend={handleSend} isSending={send.isPending} disabled={!recipientIsValid} />
      </div>
    </div>
  );
}

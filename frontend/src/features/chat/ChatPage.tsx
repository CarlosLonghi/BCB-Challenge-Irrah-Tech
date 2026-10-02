import { useParams } from 'react-router-dom';
import { useSession } from '@/features/auth/useAuth';
import { useConversation } from '@/features/conversations/useConversation';
import type { Priority } from '@/shared/api/types';
import { Avatar } from '@/shared/components/Avatar';
import { BackLink } from '@/shared/components/BackLink';
import { ErrorMessage } from '@/shared/components/ErrorMessage';
import styles from './ChatPage.module.css';
import { MessageComposer } from './MessageComposer';
import { MessageList } from './MessageList';
import { useMessages } from './useMessages';
import { useSendMessage } from './useSendMessage';

/**
 * A `key` faz o React montar um Chat novo a cada conversa:
 * texto digitado e erro de envio não "vazam" de uma conversa para outra.
 */
export function ChatPage() {
  const conversationId = Number(useParams().id);
  return <Chat key={conversationId} conversationId={conversationId} />;
}

function Chat({ conversationId }: { conversationId: number }) {
  const session = useSession();
  // Conversa e mensagens são buscadas em paralelo (duas buscas independentes).
  const conversation = useConversation(conversationId);
  const messages = useMessages(conversationId);
  const send = useSendMessage(session.client.planType, messages.reload);

  // Conversa inexistente ou de outro cliente (404): não há o que mostrar além do erro.
  if (conversation.error) {
    return (
      <div className={styles.chat}>
        <ChatHeader />
        <div className={styles.errorBody}>
          <ErrorMessage error={conversation.error} />
        </div>
      </div>
    );
  }

  function handleSend(content: string, priority: Priority) {
    return send.run({ conversationId, content, priority });
  }

  return (
    <div className={styles.chat}>
      <ChatHeader recipientName={conversation.data?.recipientName} recipientId={conversation.data?.recipientId} />

      <MessageList
        messages={messages.data}
        isPending={messages.isPending}
        error={messages.error}
        onRetry={messages.reload}
      />

      <div className={styles.footer}>
        <ErrorMessage error={send.error} />
        <MessageComposer onSend={handleSend} isSending={send.isPending} autoFocus />
      </div>
    </div>
  );
}

interface ChatHeaderProps {
  recipientName?: string;
  recipientId?: number;
}

/** Botão de voltar e, quando já carregado, avatar, nome e id do destinatário. */
function ChatHeader({ recipientName, recipientId }: ChatHeaderProps) {
  return (
    <div className={styles.header}>
      <BackLink />
      {recipientName ? (
        <>
          <Avatar name={recipientName} />
          <div className={styles.recipient}>
            <h2>{recipientName}</h2>
            <span className={`mono ${styles.recipientId}`}>destinatário #{recipientId}</span>
          </div>
        </>
      ) : null}
    </div>
  );
}

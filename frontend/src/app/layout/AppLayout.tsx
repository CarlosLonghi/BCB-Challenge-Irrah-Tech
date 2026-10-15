import { Outlet, useMatch } from 'react-router';
import { useSession } from '@/features/auth/useAuth';
import { ConversationList } from '@/features/conversations/ConversationList';
import { useAppSession } from '../useAppSession';
import { AppHeader } from './AppHeader';
import styles from './AppLayout.module.css';

/** Tela principal (logado): cabeçalho, lista de conversas e, ao lado, a rota filha (chat). */
export function AppLayout() {
  const session = useSession();
  useAppSession(session.client.id);
  // Em tela pequena mostramos lista OU chat; o CSS usa esta classe para decidir.
  const chatOpen = useMatch('/conversas/*') !== null;

  return (
    <div className={styles.app}>
      <AppHeader client={session.client} />
      <div className={chatOpen ? `${styles.body} ${styles.chatOpen}` : styles.body}>
        <div className={styles.listPane}>
          <ConversationList />
        </div>
        <section className={styles.chatPane}>
          <Outlet />
        </section>
      </div>
    </div>
  );
}

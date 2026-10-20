import styles from './EmptyChat.module.css';

/** Painel do chat quando nenhuma conversa está aberta (rota `/`). */
export function EmptyChat() {
  return (
    <div className={styles.empty}>
      <p>Selecione uma conversa ao lado ou comece uma nova.</p>
    </div>
  );
}

import styles from './EmptyChat.module.css';

export function EmptyChat() {
  return (
    <div className={styles.empty}>
      <p>Selecione uma conversa ao lado ou comece uma nova.</p>
    </div>
  );
}

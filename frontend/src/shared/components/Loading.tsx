import styles from './Loading.module.css';

/** Indicador de carregamento padrão (spinner + texto). */
export function Loading({ text = 'Carregando...' }: { text?: string }) {
  return (
    <div className={styles.loading} role="status">
      <span className={styles.spinner} aria-hidden="true" />
      {text}
    </div>
  );
}

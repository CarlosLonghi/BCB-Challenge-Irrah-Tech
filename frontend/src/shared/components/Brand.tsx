import styles from './Brand.module.css';

/** Marca "BCB" + nome do produto (cabeçalho e tela de login). */
export function Brand({ size = 'normal' }: { size?: 'normal' | 'large' }) {
  return (
    <div className={size === 'large' ? `${styles.brand} ${styles.large}` : styles.brand}>
      <span className={styles.mark} aria-hidden="true">
        BCB
      </span>
      <span className={styles.name}>Big Chat Brasil</span>
    </div>
  );
}

import { initials } from '@/shared/utils/format';
import styles from './Avatar.module.css';

/** Iniciais do nome num círculo; decorativo (o nome sempre aparece ao lado). */
export function Avatar({ name }: { name: string }) {
  return (
    <span className={styles.avatar} aria-hidden="true">
      {initials(name)}
    </span>
  );
}

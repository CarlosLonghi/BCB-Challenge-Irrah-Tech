import { CaretLeftIcon } from '@phosphor-icons/react';
import { Link } from 'react-router-dom';
import styles from './BackLink.module.css';

/** "Voltar" para a lista; só aparece em telas pequenas, onde lista e chat não cabem juntos. */
export function BackLink() {
  return (
    <Link to="/" className={styles.backLink} aria-label="Voltar para as conversas">
      <CaretLeftIcon size={20} weight="bold" aria-hidden="true" />
    </Link>
  );
}

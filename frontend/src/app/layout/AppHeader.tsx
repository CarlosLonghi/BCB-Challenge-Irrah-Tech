import { useAuth } from '@/features/auth/useAuth';
import type { Client } from '@/shared/api/types';
import { Brand } from '@/shared/components/Brand';
import { formatMoney } from '@/shared/utils/format';
import { useAppData } from '../appDataContext';
import styles from './AppHeader.module.css';

/** Marca, plano, saldo (pré-pago) ou limite restante (pós-pago), nome do cliente e "Sair". */
export function AppHeader({ client }: { client: Client }) {
  const { signOut } = useAuth();
  const { balance } = useAppData();
  const prePaid = client.planType === 'PRE_PAID';

  let amount = '...';
  if (balance.data) amount = formatMoney(prePaid ? balance.data.balance : balance.data.limit);
  else if (balance.error) amount = 'indisponível';

  return (
    <header className={styles.header}>
      <div className={styles.brand}>
        <Brand />
      </div>

      <div className={styles.client}>
        <span className={styles.name}>{client.name}</span>
        <div className={styles.balance}>
          <span className={`mono ${styles.plan}`}>{prePaid ? 'PRÉ-PAGO' : 'PÓS-PAGO'}</span>
          <span className={styles.label}>{prePaid ? 'Saldo' : 'Limite restante'}</span>
          <strong className={`mono ${styles.amount}`}>{amount}</strong>
        </div>
      </div>

      <button type="button" className={styles.logout} onClick={signOut}>
        Sair
      </button>
    </header>
  );
}

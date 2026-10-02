import type { FormEvent, ReactNode } from 'react';
import { Brand } from '@/shared/components/Brand';
import { formatMoney } from '@/shared/utils/format';
import { PRIORITY_COST } from '@/features/chat/messageRules';
import styles from './AuthCard.module.css';

interface Props {
  /** Linha pequena acima do título (ex.: "Acesso do cliente"). */
  eyebrow: string;
  title: string;
  subtitle: string;
  onSubmit: (event: FormEvent) => void;
  /** Linha de baixo do formulário (link para a outra tela). */
  footer: ReactNode;
  children: ReactNode;
}

/**
 * Moldura comum das telas de login e cadastro: painel escuro com a marca e os fatos do
 * produto à esquerda, formulário à direita. No celular o painel vira só a marca, no topo.
 */
export function AuthCard({ eyebrow, title, subtitle, onSubmit, footer, children }: Props) {
  return (
    <div className={styles.page}>
      <section className={styles.panel}>
        <Brand size="large" />
        <div className={styles.pitch}>
          <h1 className={styles.headline}>Mensageria corporativa para falar com seus clientes.</h1>
          <dl className={styles.facts}>
            <div>
              <dt>Planos</dt>
              <dd>Pré-pago e pós-pago</dd>
            </div>
            <div>
              <dt>Prioridade</dt>
              <dd className="mono">
                Normal {formatMoney(PRIORITY_COST.NORMAL)} · Urgente {formatMoney(PRIORITY_COST.URGENT)}
              </dd>
            </div>
            <div>
              <dt>Entrega</dt>
              <dd>Status de cada mensagem</dd>
            </div>
          </dl>
        </div>
      </section>

      <main className={styles.main}>
        <form className={styles.form} onSubmit={onSubmit} noValidate>
          <div className={styles.heading}>
            <p className={`mono ${styles.eyebrow}`}>{eyebrow}</p>
            <h2 className={styles.title}>{title}</h2>
            <p className={styles.subtitle}>{subtitle}</p>
          </div>
          {children}
          <div className={styles.footer}>{footer}</div>
        </form>
      </main>
    </div>
  );
}

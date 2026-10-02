import { useState, type FormEvent, type KeyboardEvent } from 'react';
import type { Priority } from '@/shared/api/types';
import { SegmentedControl, type SegmentedOption } from '@/shared/components/SegmentedControl';
import { formatMoney } from '@/shared/utils/format';
import styles from './MessageComposer.module.css';
import { MAX_MESSAGE_LENGTH, PRIORITY_COST } from './messageRules';

interface Props {
  /** Deve rejeitar em caso de erro; o texto só é limpo quando o envio dá certo. */
  onSend: (content: string, priority: Priority) => Promise<unknown>;
  isSending: boolean;
  /** Bloqueia o envio por motivo externo (ex.: destinatário ainda não preenchido). */
  disabled?: boolean;
  autoFocus?: boolean;
}

// O custo aparece na própria opção, para ser visto antes de enviar.
const PRIORITY_OPTIONS: SegmentedOption<Priority>[] = [
  { value: 'NORMAL', label: 'Normal', hint: formatMoney(PRIORITY_COST.NORMAL) },
  { value: 'URGENT', label: 'Urgente', hint: formatMoney(PRIORITY_COST.URGENT) },
];

/** Campo de envio: texto (até 255), prioridade e custo visível antes de enviar. */
export function MessageComposer({ onSend, isSending, disabled = false, autoFocus = false }: Props) {
  const [content, setContent] = useState('');
  const [priority, setPriority] = useState<Priority>('NORMAL');

  const canSend = content.trim() !== '' && !isSending && !disabled;

  async function submit() {
    if (!canSend) return;
    try {
      await onSend(content.trim(), priority);
      setContent('');
    } catch {
      // o erro é exibido por quem chamou (estado do useAction); mantém o texto para tentar de novo
    }
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    submit();
  }

  // Enter envia; Shift+Enter quebra linha.
  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      submit();
    }
  }

  return (
    <form className={styles.composer} onSubmit={handleSubmit}>
      <textarea
        className={styles.input}
        value={content}
        onChange={(e) => setContent(e.target.value)}
        onKeyDown={handleKeyDown}
        maxLength={MAX_MESSAGE_LENGTH}
        rows={2}
        placeholder="Escreva uma mensagem..."
        aria-label="Mensagem"
        autoFocus={autoFocus}
      />

      <div className={styles.bar}>
        <SegmentedControl
          name="priority"
          legend="Prioridade"
          hideLegend
          size="small"
          options={PRIORITY_OPTIONS}
          value={priority}
          onChange={setPriority}
        />

        <span className={`mono ${styles.counter}`}>
          {content.length}/{MAX_MESSAGE_LENGTH}
        </span>

        <button className={`button ${styles.send}`} type="submit" disabled={!canSend}>
          {isSending ? 'Enviando...' : 'Enviar'}
          <span className={`mono ${styles.cost}`}>{formatMoney(PRIORITY_COST[priority])}</span>
        </button>
      </div>
    </form>
  );
}

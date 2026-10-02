import styles from './SegmentedControl.module.css';

export interface SegmentedOption<T extends string> {
  value: T;
  label: string;
  /** Detalhe em fonte mono ao lado do rótulo (ex.: o custo da prioridade). */
  hint?: string;
}

interface Props<T extends string> {
  /** Nome do grupo de rádios (precisa ser único na tela). */
  name: string;
  legend: string;
  /** Esconde a legenda visualmente (continua para leitores de tela). */
  hideLegend?: boolean;
  options: SegmentedOption<T>[];
  value: T;
  onChange: (value: T) => void;
  size?: 'normal' | 'small';
}

/** Grupo de botões de opção (um rádio por opção), usado para plano e prioridade. */
export function SegmentedControl<T extends string>({
  name,
  legend,
  hideLegend = false,
  options,
  value,
  onChange,
  size = 'normal',
}: Props<T>) {
  return (
    <fieldset className={size === 'small' ? `${styles.group} ${styles.small}` : styles.group}>
      <legend className={hideLegend ? 'visually-hidden' : styles.legend}>{legend}</legend>
      <div className={styles.options}>
        {options.map((option) => (
          <label key={option.value} className={styles.option}>
            <input
              type="radio"
              name={name}
              checked={value === option.value}
              onChange={() => onChange(option.value)}
            />
            {option.label}
            {option.hint ? <span className={styles.hint}>{option.hint}</span> : null}
          </label>
        ))}
      </div>
    </fieldset>
  );
}

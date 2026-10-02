import { createContext, useContext } from 'react';
import type { Balance, Conversation } from '@/shared/api/types';
import type { FetchState } from '@/shared/hooks/useFetch';

/**
 * Dados usados por mais de uma parte da tela: o cabeçalho mostra o saldo, a coluna da
 * esquerda mostra as conversas, e o envio de mensagem (no chat) precisa atualizar os dois.
 */
export interface AppData {
  balance: FetchState<Balance>;
  conversations: FetchState<Conversation[]>;
}

// Arquivo separado do provider: o lint (react-refresh/only-export-components) pede que
// arquivos de componente exportem só componentes.
export const AppDataContext = createContext<AppData | null>(null);

export function useAppData() {
  const value = useContext(AppDataContext);
  if (!value) throw new Error('useAppData precisa estar dentro de <AppDataProvider>');
  return value;
}

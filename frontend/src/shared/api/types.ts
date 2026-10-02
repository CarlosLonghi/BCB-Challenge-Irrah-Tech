// Tipos que espelham os DTOs da API do backend.
// Datas vêm como string ISO-8601 sem fuso (LocalDateTime do servidor);
// a conversão para Date fica em utils/format.ts.

export type DocumentType = 'CPF' | 'CNPJ';
export type PlanType = 'PRE_PAID' | 'POST_PAID';
export type Priority = 'NORMAL' | 'URGENT';
export type MessageStatus = 'QUEUED' | 'PROCESSING' | 'SENT' | 'DELIVERED' | 'READ' | 'FAILED';

export interface Client {
  id: number;
  name: string;
  document: string;
  documentType: DocumentType;
  planType: PlanType;
  balance: number;
  limit: number;
  active: boolean;
}

/** Corpo de POST /clients (o id é gerado pelo servidor). */
export type NewClient = Omit<Client, 'id'>;

export interface AuthResponse {
  token: string;
  client: Client;
}

export interface Balance {
  balance: number;
  limit: number;
}

export interface Conversation {
  id: number;
  clientId: number;
  recipientId: number;
  recipientName: string;
  lastMessageContent: string | null;
  lastMessageTime: string | null;
  unreadCount: number;
}

export interface Message {
  id: number;
  conversationId: number;
  senderId: number;
  recipientId: number;
  content: string;
  createdAt: string;
  priority: Priority;
  status: MessageStatus;
  cost: number;
}

/**
 * Corpo de POST /messages.
 * Conversa existente: só conversationId. Nova conversa: recipientId + recipientName.
 */
export interface SendMessageRequest {
  conversationId?: number;
  recipientId?: number;
  recipientName?: string;
  content: string;
  priority: Priority;
}

export interface SendMessageResponse {
  id: number;
  conversationId: number;
  status: MessageStatus;
  estimatedDelivery: string;
  cost: number;
  /** Pré-pago: saldo após o débito. Pós-pago: limite restante. */
  currentBalance: number;
}

/** Formato de todo erro do backend. */
export interface ApiErrorBody {
  timestamp: string;
  status: number;
  error: string;
  message?: string;
  path: string;
}

import type { components } from './schema';

// Nomes usados no app para os schemas da API. schema.d.ts é gerado de openapi.json (npm run api:types),
// que o OpenApiContractIT do backend mantém igual ao /v3/api-docs: campo novo ou renomeado no backend
// quebra o build aqui, em vez de passar despercebido.
// Datas vêm como string ISO-8601 em UTC; a conversão para Date fica em utils/format.ts.

type Schemas = components['schemas'];

export type DocumentType = Schemas['ClientDocumentType'];
export type PlanType = Schemas['ClientPlanType'];
export type Priority = Schemas['MessagePriority'];
export type MessageStatus = Schemas['MessageStatus'];

export type Client = Schemas['ClientResponse'];
/** Corpo de POST /clients (o id é gerado pelo servidor; a senha só vai na ida). */
export type NewClient = Schemas['ClientRequest'];
export type AuthResponse = Schemas['AuthResponse'];
export type Balance = Schemas['ClientBalanceResponse'];
export type Conversation = Schemas['ConversationResponse'];
export type Message = Schemas['MessageResponse'];
/** Corpo de POST /messages. Conversa existente: só conversationId. Nova conversa: recipientId + recipientName. */
export type SendMessageRequest = Schemas['MessageRequest'];
/** `currentBalance`: no pré-pago é o saldo após o débito; no pós-pago, o limite restante. */
export type SendMessageResponse = Schemas['SendMessageResponse'];
/** Dado do evento SSE "message-status" (GET /events). */
export type MessageStatusEvent = Schemas['MessageStatusEventResponse'];
/** Formato de todo erro do backend. */
export type ApiErrorBody = Schemas['ErrorResponse'];

import { describe, expect, it } from 'vitest';
import type { Message, MessageStatus } from '@/shared/api/types';
import { hasPendingMessages } from './messageRules';

function message(status: MessageStatus): Message {
  return {
    id: 1, conversationId: 7, senderId: 1, recipientId: 55, content: 'Olá',
    createdAt: '2026-09-30T13:45:00', priority: 'NORMAL', status, cost: 0.25,
  };
}

describe('hasPendingMessages', () => {
  it('continua o polling enquanto há mensagem que ainda vai mudar de status', () => {
    expect(hasPendingMessages([message('DELIVERED'), message('QUEUED')])).toBe(true);
    expect(hasPendingMessages([message('PROCESSING')])).toBe(true);
    expect(hasPendingMessages([message('SENT')])).toBe(true);
  });

  it('para quando todas estão em estado final', () => {
    expect(hasPendingMessages([message('DELIVERED'), message('FAILED'), message('READ')])).toBe(false);
    expect(hasPendingMessages([])).toBe(false);
    expect(hasPendingMessages(undefined)).toBe(false);
  });
});

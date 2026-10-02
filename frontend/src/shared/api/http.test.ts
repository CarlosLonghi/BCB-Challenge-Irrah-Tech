import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, request, setUnauthorizedHandler } from './http';
import { clearSession, saveSession } from './session';

function mockFetch(status: number, body?: unknown) {
  const fetchMock = vi.fn().mockResolvedValue(
    new Response(body === undefined ? null : JSON.stringify(body), { status }),
  );
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

const session = {
  token: 'abc',
  client: {
    id: 1, name: 'Empresa X', document: '12345678901', documentType: 'CPF' as const,
    planType: 'PRE_PAID' as const, balance: 10, limit: 0, active: true,
  },
};

afterEach(() => {
  vi.unstubAllGlobals();
  clearSession();
  setUnauthorizedHandler(null);
});

describe('request', () => {
  it('envia o token e devolve o JSON', async () => {
    saveSession(session);
    const fetchMock = mockFetch(200, [{ id: 7 }]);

    await expect(request('/conversations')).resolves.toEqual([{ id: 7 }]);
    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe('Bearer abc');
  });

  it('usa a message do backend no erro', async () => {
    mockFetch(402, { status: 402, error: 'Payment Required', message: 'Saldo insuficiente.' });

    await expect(request('/messages', { method: 'POST', body: {} })).rejects.toEqual(
      new ApiError(402, 'Saldo insuficiente.'),
    );
  });

  it('em 401 de rota autenticada chama o handler de logout', async () => {
    const onUnauthorized = vi.fn();
    setUnauthorizedHandler(onUnauthorized);
    mockFetch(401);

    await expect(request('/conversations')).rejects.toMatchObject({ status: 401 });
    expect(onUnauthorized).toHaveBeenCalledOnce();
  });

  it('em 401 do login (rota pública) não chama o handler', async () => {
    const onUnauthorized = vi.fn();
    setUnauthorizedHandler(onUnauthorized);
    mockFetch(401, { status: 401, message: 'Documento não cadastrado.' });

    await expect(request('/auth', { method: 'POST', body: {}, auth: false })).rejects.toThrow(
      'Documento não cadastrado.',
    );
    expect(onUnauthorized).not.toHaveBeenCalled();
  });
});

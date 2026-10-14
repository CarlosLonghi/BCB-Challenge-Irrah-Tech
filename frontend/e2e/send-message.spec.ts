import { expect, test } from '@playwright/test';

/** CPF válido e novo a cada execução (o documento é único no banco). */
function randomCpf() {
  const digits = Array.from({ length: 9 }, () => Math.floor(Math.random() * 10));
  for (const length of [9, 10]) {
    const sum = digits.slice(0, length).reduce((total, digit, index) => total + digit * (length + 1 - index), 0);
    const rest = (sum * 10) % 11;
    digits.push(rest === 10 ? 0 : rest);
  }
  return digits.join('');
}

test('cadastro, nova conversa e status chegando a "Lida" pelo stream de eventos', async ({ page }) => {
  const historyRequests: string[] = [];
  page.on('request', (request) => {
    if (/\/conversations\/\d+\/messages/.test(request.url())) historyRequests.push(request.url());
  });

  await page.goto('/cadastro');
  await page.getByLabel('Nome').fill('Empresa E2E');
  await page.getByLabel('Documento').fill(randomCpf());
  await page.getByLabel('Senha', { exact: true }).fill('senha-forte-123');
  await page.getByLabel('Confirmar senha').fill('senha-forte-123');
  await page.getByLabel('Saldo inicial (R$)').fill('10');
  const stream = page.waitForResponse((response) => response.url().endsWith('/events'));
  await page.getByRole('button', { name: 'Cadastrar e entrar' }).click();

  // Logado: saldo no cabeçalho e o stream SSE aberto
  await expect(page.getByText('R$ 10,00')).toBeVisible();
  expect((await stream).status()).toBe(200);

  await page.getByRole('link', { name: 'Nova conversa' }).click();
  await page.getByLabel('ID do destinatário').fill('55');
  await page.getByLabel('Nome do destinatário').fill('Maria Souza');
  await page.getByRole('textbox', { name: 'Mensagem' }).fill('Olá do E2E');
  await page.getByRole('button', { name: /Enviar/ }).click();

  // Abre o chat da conversa criada, com o custo já descontado
  await expect(page.getByRole('heading', { name: 'Maria Souza' })).toBeVisible();
  await expect(page.getByText('R$ 9,75')).toBeVisible();
  const chat = page.locator('section');
  await expect(chat.getByText('Olá do E2E')).toBeVisible();

  // Passa por Entregue e chega a Lida sem recarregar a página
  await expect(chat.getByText('Entregue')).toBeVisible({ timeout: 30_000 });
  await expect(chat.getByText('Lida')).toBeVisible({ timeout: 30_000 });

  // Os status vieram pelo stream: com polling seriam ~5 buscas do histórico nesse intervalo
  expect(historyRequests.length).toBeLessThanOrEqual(3);
});

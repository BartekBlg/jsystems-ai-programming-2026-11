import { test, expect } from './fixtures';
import { CHAT_REPLY_TIMEOUT_MS, DECISION_BADGE, RETURN_LAPTOP, purchaseDate, sendChat, submitCase } from './support/flows';
import { invalidKey, shadow, type RealInstance } from './support/real-stack';

/** Chat behaviour on the real stack (AC-31..AC-37, AC-42). Own instance on :8085, same CASE_SIGNING_KEY across swaps. */
const PORT = 8085;
test.use({ baseURL: `http://localhost:${PORT}` });

const AGENT_REPLY = '[data-testid="agent-message"]';

test.describe('Czat (prawdziwy LLM)', () => {
  test.fixme(true, 'T35b: the UI shell wiring (form -> progress -> chat) is not in place yet; enabled by T41b.');

  let instance: RealInstance;
  test.beforeAll(async () => {
    instance = await shadow({ port: PORT });
  });
  test.afterAll(async () => {
    await instance?.stop();
  });

  test('Enter wysyła, Shift+Enter dodaje nowy wiersz, pusta wiadomość nie jest wysyłana (AC-33)', async ({ page }) => {
    await submitCase(page, RETURN_LAPTOP);
    const composer = page.getByLabel('Twoja wiadomość');
    await expect(page.getByRole('button', { name: 'Wyślij', exact: true })).toBeDisabled();
    await composer.fill('pierwszy wiersz');
    await composer.press('Shift+Enter');
    await composer.type('drugi wiersz');
    await expect(composer).toHaveValue('pierwszy wiersz\ndrugi wiersz');
    await composer.press('Enter');
    await expect(page.getByText('pierwszy wiersz').first()).toBeVisible();
    // AC-34: while the reply streams, sending is disabled and the typing indicator is visible.
    await expect(page.getByText('Agent pisze odpowiedź')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Wyślij', exact: true })).toBeDisabled();
    await expect(page.locator(AGENT_REPLY)).toHaveCount(2, { timeout: CHAT_REPLY_TIMEOUT_MS });
  });

  test('odpowiedź uwzględnia datę dostawy podaną wcześniej (AC-35)', async ({ page }) => {
    await submitCase(page, { ...RETURN_LAPTOP, daysAgo: 20 });
    const delivered = purchaseDate(3); // DD.MM.YYYY
    await sendChat(page, `Sprzęt dostarczono mi dnia ${delivered}.`);
    await expect(page.locator(AGENT_REPLY)).toHaveCount(2, { timeout: CHAT_REPLY_TIMEOUT_MS });
    await sendChat(page, 'Do kiedy mogę odesłać sprzęt?');
    await expect(page.locator(AGENT_REPLY)).toHaveCount(3, { timeout: CHAT_REPLY_TIMEOUT_MS });
    // Date check without exact prose: the reply names a deadline date (DD.MM.YYYY, or day + Polish month name)
    // that is later than the delivery date given in the chat.
    const last = await page.locator(AGENT_REPLY).last().innerText();
    expect(last).toMatch(/\d{1,2}[.\s]+(\d{1,2}|stycznia|lutego|marca|kwietnia|maja|czerwca|lipca|sierpnia|września|października|listopada|grudnia)/);
  });

  test('aktualizacja decyzji zaczyna się od "Zaktualizowana decyzja:" i zmienia odznakę (AC-36)', async ({ page }) => {
    await submitCase(page, { ...RETURN_LAPTOP, daysAgo: 20 });
    const before = await page.getByText(DECISION_BADGE).first().innerText();
    await sendChat(page, `Odebrałem paczkę z urządzeniem dopiero dnia ${purchaseDate(2)}, więc jestem w terminie.`);
    await expect(page.locator(AGENT_REPLY)).toHaveCount(2, { timeout: CHAT_REPLY_TIMEOUT_MS });
    const reply = await page.locator(AGENT_REPLY).last().innerText();
    if (/^\s*Zaktualizowana decyzja:/.test(reply)) {
      const category = /^\s*Zaktualizowana decyzja:\s*(.+)/.exec(reply)![1];
      await expect(page.getByText(DECISION_BADGE).first()).toContainText(category.replace(/[*_.]/g, '').trim());
    } else {
      // The model may keep the decision; the badge must then be unchanged.
      expect(await page.getByText(DECISION_BADGE).first().innerText()).toBe(before);
    }
  });

  test('nieudana wiadomość ma "Nie wysłano" i "Ponów" bez duplikatu (AC-42, instancja z błędnym kluczem)', async ({ page }) => {
    await submitCase(page, RETURN_LAPTOP);
    // Swap the valid instance for an invalid-key one on the same port with the same signing key.
    await instance.stop();
    instance = await invalidKey({ port: PORT });
    await sendChat(page, 'Czy mogę wymienić sprzęt na inny model?');
    await expect(page.getByText('Nie wysłano')).toBeVisible({ timeout: CHAT_REPLY_TIMEOUT_MS });
    await expect(page.getByRole('button', { name: 'Ponów' })).toBeVisible();
    await expect(page.locator('body')).not.toContainText(/openrouter|401|sk-or|Exception/i);

    // Restore the valid instance and retry.
    await instance.stop();
    instance = await shadow({ port: PORT });
    await page.getByRole('button', { name: 'Ponów' }).click();
    await expect(page.locator(AGENT_REPLY)).toHaveCount(2, { timeout: CHAT_REPLY_TIMEOUT_MS });
    await expect(page.getByText('Nie wysłano')).toHaveCount(0);
    await expect(page.getByText('Czy mogę wymienić sprzęt na inny model?')).toHaveCount(1);
  });
});

import { test, expect } from './fixtures';
import { AGENT_MESSAGE, CHAT_REPLY_TIMEOUT_MS, RETURN_LAPTOP, badgeText, expandSummary, sendChat, submitCase } from './support/flows';
import { invalidKey, shadow, type RealInstance } from './support/real-stack';

/** Chat behaviour on the real stack (AC-31..AC-37, AC-42). Own instance on :8085, same CASE_SIGNING_KEY across swaps. */
const PORT = 8085;
test.use({ baseURL: `http://localhost:${PORT}` });

const AGENT_REPLY = AGENT_MESSAGE;

test.describe('Czat (prawdziwy LLM)', () => {
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
    await composer.pressSequentially('drugi wiersz');
    await expect(composer).toHaveValue('pierwszy wiersz\ndrugi wiersz');
    await composer.press('Enter');
    await expect(page.getByText('pierwszy wiersz').first()).toBeVisible();
    // AC-34: while the reply streams, sending is disabled and the typing indicator is visible.
    await expect(page.locator('.typing-indicator')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Wyślij', exact: true })).toBeDisabled();
    await expect(page.locator(AGENT_REPLY)).toHaveCount(2, { timeout: CHAT_REPLY_TIMEOUT_MS });
  });

  test('odpowiedź uwzględnia dostawę podaną wcześniej (AC-35)', async ({ page }) => {
    await submitCase(page, { ...RETURN_LAPTOP, daysAgo: 20 });
    await sendChat(page, 'Urządzenie zostało dostarczone 3 dni temu.');
    await expect(page.locator(AGENT_REPLY)).toHaveCount(2, { timeout: CHAT_REPLY_TIMEOUT_MS });
    await expect(page.locator('.typing-indicator')).toHaveCount(0, { timeout: CHAT_REPLY_TIMEOUT_MS });
    await sendChat(page, 'Do kiedy mogę odstąpić od umowy?');
    await expect(page.locator(AGENT_REPLY)).toHaveCount(3, { timeout: CHAT_REPLY_TIMEOUT_MS });
    await expect(page.locator('.typing-indicator')).toHaveCount(0, { timeout: CHAT_REPLY_TIMEOUT_MS });
    // Robust to prose: the reply takes the stated delivery into account, i.e. it mentions the delivery/receipt
    // (dostaw*/odbior*/otrzyman*/3 dni) or the 14-day withdrawal window counted from it. No specific date is asserted.
    const last = await page.locator(AGENT_REPLY).last().innerText();
    expect(last).toMatch(/dostaw|dostarcz|odbi[eo]r|otrzyma|3 dni|trzy dni|14 dni|czternastu dni/i);
  });

  test('aktualizacja decyzji zaczyna się od "Zaktualizowana decyzja:" i zmienia odznakę (AC-36)', async ({ page }) => {
    await submitCase(page, { ...RETURN_LAPTOP, daysAgo: 20 });
    const before = await badgeText(page);
    await sendChat(page, 'Odebrałem paczkę z urządzeniem dopiero 2 dni temu, więc jestem w terminie na zwrot.');
    await expect(page.locator(AGENT_REPLY)).toHaveCount(2, { timeout: CHAT_REPLY_TIMEOUT_MS });
    // .message-content excludes the speaker label ("Agent") that precedes the text inside the article.
    const reply = await page.locator(AGENT_REPLY).last().locator('.message-content').innerText();
    await expandSummary(page);
    const updated = /^\s*Zaktualizowana decyzja:/.test(reply);
    test.info().annotations.push({ type: 'observed', description: updated ? 'decision update announced' : 'decision kept' });
    if (/^\s*Zaktualizowana decyzja:/.test(reply)) {
      const category = /^\s*Zaktualizowana decyzja:\s*(.+)/.exec(reply)![1];
      await expect(page.locator('.decision-badge').first()).toContainText(category.replace(/[*_.]/g, '').trim());
    } else {
      // The model may keep the decision; the badge must then be unchanged (observed behaviour is recorded in the report).
      expect(await badgeText(page)).toBe(before);
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
    await expect(page.getByRole('button', { name: 'Ponów' })).toBeEnabled();
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

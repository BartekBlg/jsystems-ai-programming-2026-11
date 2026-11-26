import path from 'node:path';
import { test, expect, TEXT } from './fixtures';

/** Real photo from the repository (never generated). */
const PHOTO = path.resolve(__dirname, '../../../assets/example-images/laptop-2.webp');
const MODEL = 'Lenovo ThinkPad T14 Gen 4';
const DISCLAIMER =
  'To wstępna, automatyczna ocena przygotowana na podstawie formularza i zdjęcia. Ostateczną decyzję podejmie nasz zespół serwisu po otrzymaniu i sprawdzeniu urządzenia.';
const DISCLAIMER_FOLLOW_UP = 'Nie ogranicza to Twojego ustawowego prawa do odstąpienia od umowy.';
const DECISION_BADGE = /Przyjęte|Odrzucone|Potrzebne dodatkowe informacje|Przekazane do specjalisty/;
const CHAT_TIMEOUT_MS = 90_000;

/** DD.MM.YYYY for a date five days before today (local time). */
function purchaseDateFiveDaysAgo(): string {
  const d = new Date();
  d.setDate(d.getDate() - 5);
  const dd = String(d.getDate()).padStart(2, '0');
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  return `${dd}.${mm}.${d.getFullYear()}`;
}

test.describe('Flow 4.1 — zwrot, ścieżka szczęśliwa (AC-21, prawdziwy LLM)', () => {
  test('zgłoszenie zwrotu prowadzi do czatu z wstępną decyzją', async ({ page }) => {
    await page.goto('/');

    // 1. Wypełnienie formularza
    await page.getByRole('radio', { name: /Zwrot/ }).check();
    await page.getByLabel(TEXT.form.category).click();
    await page.getByRole('option', { name: 'Laptop', exact: true }).click();
    await page.getByLabel(TEXT.form.modelName).fill(MODEL);
    await page.getByLabel(TEXT.form.purchaseDate).fill(purchaseDateFiveDaysAgo());
    await page.locator('input[type="file"]').setInputFiles(PHOTO);

    // 2. Wysłanie — ekran postępu
    await page.getByRole('button', { name: TEXT.form.submit }).click();
    await expect(page.getByText(TEXT.progress.analysing)).toBeVisible();
    await expect(page.getByText(TEXT.progress.deciding)).toBeVisible();
    await expect(page.getByText(TEXT.progress.note)).toBeVisible();

    // 3. Czat w ciągu 90 s: podsumowanie i odznaka decyzji
    await expect(page.getByText(TEXT.chat.summaryTitle)).toBeVisible({ timeout: CHAT_TIMEOUT_MS });
    await expect(page.getByText('Zwrot', { exact: true }).first()).toBeVisible();
    await expect(page.getByText('Laptop', { exact: true }).first()).toBeVisible();
    await expect(page.getByText(MODEL, { exact: true }).first()).toBeVisible();
    await expect(page.getByText(DECISION_BADGE).first()).toBeVisible();

    // 4. Pierwsza wiadomość agenta: nagłówki w kolejności, § + cyfra, zakończenie
    const headings = ['Decyzja', 'Uzasadnienie', 'Co widać na zdjęciu', 'Kolejne kroki'];
    const tops: number[] = [];
    for (const heading of headings) {
      const box = await page.getByRole('heading', { name: heading, exact: true }).first().boundingBox();
      expect(box, `brak nagłówka "${heading}" w wiadomości agenta`).not.toBeNull();
      tops.push(box!.y);
    }
    expect(tops, 'nagłówki w złej kolejności').toEqual([...tops].sort((a, b) => a - b));

    const bodyText = await page.locator('body').innerText();
    expect(bodyText).toMatch(/§\s?\d/);

    const disclaimerAt = bodyText.indexOf(DISCLAIMER);
    expect(disclaimerAt, 'brak zakończenia wiadomości agenta').toBeGreaterThan(-1);
    const afterDisclaimer = bodyText.slice(disclaimerAt + DISCLAIMER.length);
    expect(afterDisclaimer.trimStart().startsWith(DISCLAIMER_FOLLOW_UP), 'zdanie po zastrzeżeniu').toBe(true);

    // 5. Notka demonstracyjna nadal widoczna; brak poziomego przewijania przy 360 px
    await expect(page.getByText(TEXT.demoNote)).toBeVisible();
    const width = page.viewportSize()!.width;
    const scrollWidth = await page.evaluate(() => document.documentElement.scrollWidth);
    expect(scrollWidth).toBeLessThanOrEqual(width);
    if (test.info().project.name === 'mobile-360') {
      expect(scrollWidth).toBeLessThanOrEqual(360);
    }
  });
});

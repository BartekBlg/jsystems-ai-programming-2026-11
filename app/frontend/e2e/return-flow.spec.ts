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
    // The floating label overlays the middle of the select (Material); a real user opens it by clicking the arrow side.
    const category = page.getByRole('combobox', { name: TEXT.form.category });
    const categoryBox = (await category.boundingBox())!;
    await category.click({ position: { x: categoryBox.width - 20, y: categoryBox.height / 2 } });
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
    // at 360 px the summary is a collapsed accordion: its title is rendered twice (panel header + hidden h2)
    await expect(page.getByText(TEXT.chat.summaryTitle).first()).toBeVisible({ timeout: CHAT_TIMEOUT_MS });
    // at 360 px the summary accordion starts collapsed; a user opens it to read the details
    const summaryToggle = page.getByRole('button', { name: TEXT.chat.summaryTitle });
    if ((await summaryToggle.count()) > 0 && (await summaryToggle.getAttribute('aria-expanded')) === 'false') {
      await summaryToggle.click();
    }
    await expect(page.getByText('Zwrot', { exact: true }).first()).toBeVisible();
    await expect(page.getByText('Laptop', { exact: true }).first()).toBeVisible();
    await expect(page.getByText(MODEL, { exact: true }).first()).toBeVisible();
    await expect(page.getByText(DECISION_BADGE).first()).toBeVisible();

    // 4. Pierwsza wiadomość agenta: nagłówki w kolejności, § + cyfra, zakończenie
    const headings = ['Decyzja', 'Uzasadnienie', 'Co widać na zdjęciu', 'Kolejne kroki'];
    const tops: number[] = [];
    const agentMessage = page.locator('article.message--agent').first(); // the summary card has its own "Decyzja" heading
    for (const heading of headings) {
      const box = await agentMessage.getByRole('heading', { name: heading, exact: true }).first().boundingBox();
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

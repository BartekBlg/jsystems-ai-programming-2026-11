import path from 'node:path';
import { test, expect, TEXT } from './fixtures';

const PHOTO = path.resolve(__dirname, '../../../assets/example-images/laptop-1.png'); // real JPEG content, .png name

/** Records every API request so the spec can prove no submission reached the backend. */
function trackApiPosts(page: import('@playwright/test').Page): string[] {
  const posts: string[] = [];
  page.on('request', (req) => {
    if (req.method() === 'POST' && new URL(req.url()).pathname.startsWith('/api/')) {
      posts.push(`${req.method()} ${req.url()}`);
    }
  });
  return posts;
}

test.describe('Formularz zgłoszenia — walidacja (AC-01..AC-13)', () => {
  test('pola pojawiają się w kolejności z PRD §4.5', async ({ page }) => {
    await page.goto('/');

    const labels = [
      TEXT.form.caseType,
      TEXT.form.category,
      TEXT.form.modelName,
      TEXT.form.purchaseDate,
      TEXT.form.reasonNeutral,
      TEXT.form.photo,
    ];
    const ys: number[] = [];
    for (const label of labels) {
      const box = await page.getByText(label, { exact: true }).first().boundingBox();
      expect(box, `brak etykiety "${label}"`).not.toBeNull();
      ys.push(box!.y);
    }
    const sorted = [...ys].sort((a, b) => a - b);
    expect(ys).toEqual(sorted);

    await expect(page.getByRole('radio', { name: /Zwrot/ })).not.toBeChecked();
    await expect(page.getByRole('radio', { name: /Reklamacja/ })).not.toBeChecked();
  });

  test('pusty formularz: wszystkie komunikaty naraz, fokus na pierwszym radio, brak wysyłki', async ({ page }) => {
    const posts = trackApiPosts(page);
    await page.goto('/');
    const urlBefore = page.url();

    await page.getByRole('button', { name: TEXT.form.submit }).click();

    await expect(page.getByText(TEXT.errors.caseTypeRequired)).toBeVisible();
    await expect(page.getByText(TEXT.errors.categoryRequired)).toBeVisible();
    await expect(page.getByText(TEXT.errors.modelName)).toBeVisible();
    await expect(page.getByText(TEXT.errors.purchaseDateRequired)).toBeVisible();
    await expect(page.getByText(TEXT.errors.photoRequired, { exact: true })).toBeVisible();

    await expect(page.getByRole('radio', { name: /Zwrot/ })).toBeFocused();
    expect(page.url()).toBe(urlBefore);
    await expect(page.getByText('Analizujemy zdjęcie')).toHaveCount(0);
    expect(posts, 'żadne żądanie POST nie powinno trafić do backendu').toEqual([]);
  });

  test('Reklamacja + krótki powód: błąd długości, wpisane wartości zachowane', async ({ page }) => {
    const posts = trackApiPosts(page);
    await page.goto('/');

    await page.getByRole('radio', { name: /Reklamacja/ }).check();
    await page.getByLabel(TEXT.form.modelName).fill('Lenovo ThinkPad T14');
    await page.getByLabel(/^Powód/).fill('Krótko');

    await page.getByRole('button', { name: TEXT.form.submit }).click();

    await expect(page.getByText(TEXT.errors.reasonComplaint)).toBeVisible();
    await expect(page.getByLabel(TEXT.form.modelName)).toHaveValue('Lenovo ThinkPad T14');
    await expect(page.getByLabel(/^Powód/)).toHaveValue('Krótko');
    expect(posts, 'żadne żądanie POST nie powinno trafić do backendu').toEqual([]);
  });

  test('zdjęcie: podgląd i „Usuń” po wgraniu pliku, po usunięciu podgląd znika', async ({ page }) => {
    await page.goto('/');

    await page.locator('input[type="file"]').setInputFiles(PHOTO);

    // Local-file preview is rendered from a blob: or data: URL.
    const preview = page.locator('img[src^="blob:"], img[src^="data:"]');
    await expect(page.getByRole('button', { name: TEXT.form.removePhoto })).toBeVisible();
    await expect(preview).toBeVisible();

    await page.getByRole('button', { name: TEXT.form.removePhoto }).click();

    await expect(page.getByRole('button', { name: TEXT.form.removePhoto })).toHaveCount(0);
    await expect(preview).toHaveCount(0);
  });

  test('podpowiedź dla zdjęcia zależy od rodzaju zgłoszenia', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByText(TEXT.photoHint.RETURN)).toHaveCount(0);
    await expect(page.getByText(TEXT.photoHint.COMPLAINT)).toHaveCount(0);

    await page.getByRole('radio', { name: /Zwrot/ }).check();
    await expect(page.getByText(TEXT.photoHint.RETURN)).toBeVisible();
    await expect(page.getByText(TEXT.form.reasonOptional, { exact: true })).toBeVisible();
    await expect(page.getByText(TEXT.photoHint.COMPLAINT)).toHaveCount(0);

    await page.getByRole('radio', { name: /Reklamacja/ }).check();
    await expect(page.getByText(TEXT.photoHint.COMPLAINT)).toBeVisible();
    await expect(page.getByText(TEXT.photoHint.RETURN)).toHaveCount(0);
  });

  test('notka demonstracyjna widoczna; brak poziomego przewijania (360 px)', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByText(TEXT.demoNote)).toBeVisible();

    const width = page.viewportSize()!.width;
    const scrollWidth = await page.evaluate(() => document.documentElement.scrollWidth);
    expect(scrollWidth).toBeLessThanOrEqual(width);
    if (test.info().project.name === 'mobile-360') {
      expect(scrollWidth).toBeLessThanOrEqual(360);
    }
  });
});

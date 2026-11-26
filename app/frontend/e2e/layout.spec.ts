import { test, expect, TEXT } from './fixtures';
import { RETURN_LAPTOP, expectNoHorizontalScroll, fillForm, submitCase } from './support/flows';

/** AC-45 / AC-47: run in both projects (360 and 1280 px). */
test.describe('Układ i klawiatura (AC-45, AC-47)', () => {
  test.fixme(true, 'T35b: the UI shell wiring (form -> progress -> chat) is not in place yet; enabled by T41b.');

  test('brak poziomego przewijania na formularzu, postępie i czacie', async ({ page }) => {
    await page.goto('/');
    await expectNoHorizontalScroll(page);
    await fillForm(page, RETURN_LAPTOP);
    await expectNoHorizontalScroll(page); // form with photo preview
    await page.getByRole('button', { name: TEXT.form.submit }).click();
    await expect(page.getByText(TEXT.progress.analysing)).toBeVisible();
    await expectNoHorizontalScroll(page); // progress screen
    await expect(page.getByText(TEXT.chat.summaryTitle)).toBeVisible({ timeout: 90_000 });
    await expectNoHorizontalScroll(page); // chat screen
  });

  test('formularz obsługiwany samą klawiaturą (AC-47)', async ({ page }) => {
    await page.goto('/');
    // Tab order: case type radios -> category -> model -> date -> reason -> photo -> submit; every control has a visible label.
    await page.getByRole('radio', { name: /Zwrot/ }).focus();
    await page.keyboard.press('Space');
    await expect(page.getByRole('radio', { name: /Zwrot/ })).toBeChecked();
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');
    await expect(page.getByLabel(TEXT.form.category)).toBeFocused();
    await page.keyboard.press('Enter');
    await page.getByRole('option', { name: 'Laptop', exact: true }).focus();
    await page.keyboard.press('Enter');
    await page.keyboard.press('Tab');
    await expect(page.getByLabel(TEXT.form.modelName)).toBeFocused();
    await page.keyboard.type(RETURN_LAPTOP.model);
    await page.keyboard.press('Tab');
    await expect(page.getByLabel(TEXT.form.purchaseDate)).toBeFocused();
    // Submitting with the keyboard on an incomplete form shows the Polish validation errors.
    await page.getByRole('button', { name: TEXT.form.submit }).focus();
    await page.keyboard.press('Enter');
    await expect(page.getByText(TEXT.errors.purchaseDateRequired)).toBeVisible();
    await expect(page.getByText(TEXT.errors.photoRequired)).toBeVisible();
    await expectNoHorizontalScroll(page);
    void submitCase; // full keyboard-only submission is added by T41b together with a real photo pick
  });
});

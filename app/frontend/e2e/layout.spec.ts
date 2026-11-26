import { test, expect, TEXT } from './fixtures';
import { RETURN_LAPTOP, expectNoHorizontalScroll, fillForm, purchaseDate, waitForFirstDecision } from './support/flows';

/** AC-45 / AC-47: run in both projects (360 and 1280 px). */
test.describe('Układ i klawiatura (AC-45, AC-47)', () => {
  test('brak poziomego przewijania na formularzu, postępie i czacie', async ({ page }) => {
    await page.goto('/');
    await expectNoHorizontalScroll(page);
    await fillForm(page, RETURN_LAPTOP);
    await expectNoHorizontalScroll(page); // form with photo preview
    await page.getByRole('button', { name: TEXT.form.submit }).click();
    await expect(page.getByText(TEXT.progress.analysing)).toBeVisible();
    await expectNoHorizontalScroll(page); // progress screen
    await waitForFirstDecision(page);
    await expectNoHorizontalScroll(page); // chat screen
  });

  test('formularz obsługiwany samą klawiaturą (AC-47)', async ({ page }) => {
    await page.goto('/');
    // Tab order: case type radios -> category -> model -> date -> reason -> photo -> submit; every control has a visible label.
    await page.getByRole('radio', { name: /Zwrot/ }).focus();
    await page.keyboard.press('Space');
    await expect(page.getByRole('radio', { name: /Zwrot/ })).toBeChecked();
    await page.keyboard.press('Tab'); // the radio group is a single tab stop
    await expect(page.getByRole('combobox', { name: TEXT.form.category })).toBeFocused();
    await page.keyboard.press('Enter');
    await page.getByRole('option', { name: 'Laptop', exact: true }).waitFor();
    await page.keyboard.press('ArrowDown');
    await page.keyboard.press('Enter'); // options: Laptop is first
    await page.keyboard.press('Tab');
    await expect(page.getByLabel(TEXT.form.modelName)).toBeFocused();
    await page.keyboard.type(RETURN_LAPTOP.model);
    await page.keyboard.press('Tab');
    await expect(page.getByLabel(TEXT.form.purchaseDate)).toBeFocused();
    // Submitting with the keyboard on an incomplete form shows the Polish validation errors.
    await page.getByRole('button', { name: TEXT.form.submit }).focus();
    await page.keyboard.press('Enter');
    await expect(page.getByText(TEXT.errors.purchaseDateRequired, { exact: true })).toBeVisible();
    await expect(page.getByText(TEXT.errors.photoRequired, { exact: true })).toBeVisible();
    await expectNoHorizontalScroll(page);

    // Complete the form with the keyboard: date typed as DD.MM.YYYY, photo chosen through the (native) file input,
    // submission with Enter on the focused submit button.
    await page.getByLabel(TEXT.form.purchaseDate).focus();
    await page.keyboard.type(purchaseDate(RETURN_LAPTOP.daysAgo));
    await page.locator('input[type="file"]').setInputFiles(RETURN_LAPTOP.photo);
    await page.getByRole('button', { name: TEXT.form.submit }).focus();
    await expect(page.getByRole('button', { name: TEXT.form.submit })).toBeFocused();
    await page.keyboard.press('Enter');
    await waitForFirstDecision(page);
  });
});

import { test, expect, TEXT } from './fixtures';
import { DECISION_BADGE, RETURN_LAPTOP, fillForm, submitCase } from './support/flows';

test.describe('Odświeżenie i nowe zgłoszenie (AC-38, AC-39, AC-40)', () => {
  test.fixme(true, 'T35b: the UI shell wiring (form -> progress -> chat) is not in place yet; enabled by T41b.');

  test('AC-38: odświeżenie czatu zachowuje podsumowanie i wiadomości w tej samej kolejności', async ({ page }) => {
    await submitCase(page, RETURN_LAPTOP);
    const agentBefore = await page.locator('[data-testid="agent-message"]').allInnerTexts();
    const badge = await page.getByText(DECISION_BADGE).first().innerText();
    await page.reload();
    await expect(page.getByText(TEXT.chat.summaryTitle)).toBeVisible();
    await expect(page.getByText(RETURN_LAPTOP.model, { exact: true }).first()).toBeVisible();
    expect(await page.getByText(DECISION_BADGE).first().innerText()).toBe(badge);
    expect(await page.locator('[data-testid="agent-message"]').allInnerTexts()).toEqual(agentBefore);
  });

  test('AC-39: Nowe zgłoszenie pyta o potwierdzenie; anulowanie zostawia rozmowę, potwierdzenie czyści formularz', async ({ page }) => {
    await submitCase(page, RETURN_LAPTOP);
    await page.getByRole('button', { name: TEXT.newCase }).first().click();
    await expect(page.getByText('Rozpocząć nowe zgłoszenie? Bieżąca rozmowa zostanie usunięta.')).toBeVisible();
    await page.getByRole('button', { name: 'Anuluj' }).click();
    await expect(page.getByText(TEXT.chat.summaryTitle)).toBeVisible();

    await page.getByRole('button', { name: TEXT.newCase }).first().click();
    await page.getByRole('button', { name: 'Rozpocznij nowe' }).click();
    await expect(page.getByRole('button', { name: TEXT.form.submit })).toBeVisible();
    await expect(page.getByLabel(TEXT.form.modelName)).toHaveValue('');
    await page.reload();
    await expect(page.getByRole('button', { name: TEXT.form.submit })).toBeVisible(); // the case is gone
  });

  test('AC-40: odświeżenie podczas postępu wraca do formularza z danymi bez zdjęcia i komunikatem', async ({ page }) => {
    await page.goto('/');
    await fillForm(page, RETURN_LAPTOP);
    await page.getByRole('button', { name: TEXT.form.submit }).click();
    await expect(page.getByText(TEXT.progress.analysing)).toBeVisible();
    await page.reload();
    await expect(page.getByText('Przetwarzanie zostało przerwane. Wyślij zgłoszenie ponownie.')).toBeVisible();
    await expect(page.getByLabel(TEXT.form.modelName)).toHaveValue(RETURN_LAPTOP.model);
    await expect(page.getByText(TEXT.errors.photoRequired)).toBeHidden(); // shown only after a submit attempt
    await expect(page.getByRole('button', { name: TEXT.form.choosePhoto })).toBeVisible(); // photo not restored
  });
});

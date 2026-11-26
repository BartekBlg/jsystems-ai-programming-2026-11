import { test, expect, TEXT } from './fixtures';
import { AGENT_MESSAGE, IMAGES, expandSummary, fillForm, waitForFirstDecision } from './support/flows';

/**
 * AC-19/AC-26 + ADR-005 D9: when the photo is not sufficient, the decision is "Potrzebne dodatkowe informacje"
 * and the message offers an inline "Nowe zgłoszenie" button. Uses a real example photo of a different device
 * category than the one selected (no generated images): a laptop photo submitted as a smartphone.
 */
test.describe('Niewystarczające zdjęcie (AC-26, D9)', () => {
  test('zdjęcie innej kategorii daje prośbę o dodatkowe informacje i przycisk Nowe zgłoszenie w wiadomości', async ({ page }) => {
    await page.goto('/');
    await fillForm(page, {
      type: 'Zwrot',
      category: 'Smartfon',
      model: 'Samsung Galaxy S23',
      daysAgo: 3,
      photo: IMAGES.laptop1,
    });
    await page.getByRole('button', { name: TEXT.form.submit }).click();
    await waitForFirstDecision(page);
    await expandSummary(page);
    await expect(page.locator('.decision-badge').first()).toHaveText('Potrzebne dodatkowe informacje');
    // The inline button lives inside the agent message, in addition to the header button.
    const inline = page.locator('.inline-new-case').getByRole('button', { name: TEXT.newCase });
    await expect(page.locator(AGENT_MESSAGE).first()).toBeVisible();
    await expect(inline).toBeVisible();

    // It opens the AC-39 confirmation; confirming returns to an empty form.
    await inline.click();
    await expect(page.getByText('Rozpocząć nowe zgłoszenie? Bieżąca rozmowa zostanie usunięta.')).toBeVisible();
    await page.getByRole('button', { name: 'Rozpocznij nowe' }).click();
    await expect(page.getByRole('button', { name: TEXT.form.submit })).toBeVisible();
  });
});

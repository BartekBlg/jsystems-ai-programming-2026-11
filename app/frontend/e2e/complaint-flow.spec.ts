import { test, expect, TEXT } from './fixtures';
import { COMPLAINT_PHONE, DECISION_BADGE, DISCLAIMER_START, FIRST_DECISION_TIMEOUT_MS, expectNoHorizontalScroll, fillForm } from './support/flows';

test.describe('Flow — reklamacja, ścieżka szczęśliwa (AC-16, AC-18, AC-21..AC-23, prawdziwy LLM)', () => {
  test.fixme(true, 'T35b: the UI shell wiring (form -> progress -> chat) is not in place yet; enabled by T41b.');

  test('zgłoszenie reklamacji prowadzi do czatu z wstępną decyzją', async ({ page }) => {
    await page.goto('/');
    await fillForm(page, COMPLAINT_PHONE);
    // Complaint wording: reason is required and the photo hint asks for the defect.
    await expect(page.getByText(TEXT.photoHint.COMPLAINT)).toBeVisible();
    await expect(page.getByLabel(TEXT.form.reasonNeutral)).toBeVisible();

    await page.getByRole('button', { name: TEXT.form.submit }).click();
    await expect(page.getByText(TEXT.progress.analysing)).toBeVisible();

    await expect(page.getByText(TEXT.chat.summaryTitle)).toBeVisible({ timeout: FIRST_DECISION_TIMEOUT_MS });
    await expect(page.getByText('Reklamacja', { exact: true }).first()).toBeVisible();
    await expect(page.getByText(COMPLAINT_PHONE.model, { exact: true }).first()).toBeVisible();
    await expect(page.getByText(DECISION_BADGE).first()).toBeVisible();

    // Robust to LLM prose: headings, a § citation and the disclaimer only.
    for (const heading of ['Decyzja', 'Uzasadnienie', 'Kolejne kroki']) {
      await expect(page.getByRole('heading', { name: heading, exact: true }).first()).toBeVisible();
    }
    const body = await page.locator('body').innerText();
    expect(body).toMatch(/§\s?\d/);
    expect(body).toContain(DISCLAIMER_START);
    await expect(page.getByText(TEXT.demoNote)).toBeVisible();
    await expectNoHorizontalScroll(page);
  });
});

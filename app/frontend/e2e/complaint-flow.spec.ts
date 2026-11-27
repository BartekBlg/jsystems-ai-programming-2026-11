import { test, expect, TEXT } from './fixtures';
import { AGENT_MESSAGE, COMPLAINT_PHONE, DISCLAIMER_START, expectNoHorizontalScroll, fillForm, waitForFirstDecision } from './support/flows';

test.describe('Flow — reklamacja, ścieżka szczęśliwa (AC-16, AC-18, AC-21..AC-23, prawdziwy LLM)', () => {
  test('zgłoszenie reklamacji prowadzi do czatu z wstępną decyzją', async ({ page }) => {
    await page.goto('/');
    await fillForm(page, COMPLAINT_PHONE);
    // Complaint wording: reason is required and the photo hint asks for the defect.
    await expect(page.getByText(TEXT.photoHint.COMPLAINT)).toBeVisible();
    await expect(page.getByLabel(TEXT.form.reasonNeutral, { exact: true })).toBeVisible();

    await page.getByRole('button', { name: TEXT.form.submit }).click();
    await expect(page.getByText(TEXT.progress.analysing)).toBeVisible();

    await waitForFirstDecision(page);
    await expect(page.getByText('Reklamacja', { exact: true }).first()).toBeVisible();
    await expect(page.getByText(COMPLAINT_PHONE.model, { exact: true }).first()).toBeVisible();

    // Robust to LLM prose: headings in the agent message, a § citation and the disclaimer only.
    const agent = page.locator(AGENT_MESSAGE).first();
    for (const heading of ['Decyzja', 'Uzasadnienie', 'Kolejne kroki']) {
      await expect(agent.getByRole('heading', { name: heading, exact: true }).first()).toBeVisible();
    }
    const body = await page.locator('body').innerText();
    expect(body).toMatch(/§\s?\d/);
    expect(body).toContain(DISCLAIMER_START);
    await expect(page.getByText(TEXT.demoNote)).toBeVisible();
    await expectNoHorizontalScroll(page);
  });
});

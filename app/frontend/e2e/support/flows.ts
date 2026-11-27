import path from 'node:path';
import fs from 'node:fs';
import { expect, type Page } from '@playwright/test';
import { TEXT } from '../fixtures';

/** Real example photos from the repository (never generated). */
export const IMAGES = {
  laptop1: path.resolve(__dirname, '../../../../assets/example-images/laptop-1.png'),
  laptop2: path.resolve(__dirname, '../../../../assets/example-images/laptop-2.webp'),
  phone1: path.resolve(__dirname, '../../../../assets/example-images/phone-1.jpg'),
  phone2: path.resolve(__dirname, '../../../../assets/example-images/phone-2.jpeg'),
  phone3: path.resolve(__dirname, '../../../../assets/example-images/phone-3.jpeg'),
};

export const DECISION_BADGE = /Przyjęte|Odrzucone|Potrzebne dodatkowe informacje|Przekazane do specjalisty/;
export const DISCLAIMER_START = 'To wstępna, automatyczna ocena';
export const FIRST_DECISION_TIMEOUT_MS = 90_000;
export const CHAT_REPLY_TIMEOUT_MS = 90_000;

/** DD.MM.YYYY for a date `days` before today (local time). */
export function purchaseDate(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return `${String(d.getDate()).padStart(2, '0')}.${String(d.getMonth() + 1).padStart(2, '0')}.${d.getFullYear()}`;
}

export interface FormInput {
  type: 'Zwrot' | 'Reklamacja';
  category: string; // Polish option label, e.g. 'Laptop', 'Smartfon'
  model: string;
  daysAgo: number;
  reason?: string;
  photo: string;
}

export const AGENT_MESSAGE = 'article.message--agent';

export async function fillForm(page: Page, input: FormInput): Promise<void> {
  expect(fs.existsSync(input.photo), `missing example photo ${input.photo}`).toBe(true);
  await page.getByRole('radio', { name: new RegExp(input.type) }).check();
  // Material select: click the combobox on its arrow side; the floating label/required marker overlays its middle.
  const category = page.getByRole('combobox', { name: TEXT.form.category });
  const box = (await category.boundingBox())!;
  await category.click({ position: { x: box.width - 20, y: box.height / 2 } });
  await page.getByRole('option', { name: input.category, exact: true }).click();
  await page.getByLabel(TEXT.form.modelName).fill(input.model);
  await page.getByLabel(TEXT.form.purchaseDate).fill(purchaseDate(input.daysAgo));
  if (input.reason) {
    await page.getByLabel(/^Powód/).fill(input.reason);
  }
  await page.locator('input[type="file"]').setInputFiles(input.photo);
}

/** Fills and submits the form, then waits for the chat screen with the first decision. */
export async function submitCase(page: Page, input: FormInput): Promise<void> {
  await page.goto('/');
  await fillForm(page, input);
  await page.getByRole('button', { name: TEXT.form.submit }).click();
  await waitForFirstDecision(page);
}

/** Waits for the chat screen with the first agent message (summary title is rendered twice at 360 px). */
export async function waitForFirstDecision(page: Page): Promise<void> {
  await expect(page.getByText(TEXT.chat.summaryTitle).first()).toBeVisible({ timeout: FIRST_DECISION_TIMEOUT_MS });
  await expect(page.locator(AGENT_MESSAGE).first()).toBeVisible({ timeout: FIRST_DECISION_TIMEOUT_MS });
  await expandSummary(page);
  await expect(page.locator('.decision-badge').first()).toBeVisible({ timeout: FIRST_DECISION_TIMEOUT_MS });
  await expect(page.locator('.decision-badge').first()).toHaveText(DECISION_BADGE);
}

/** On mobile the case summary is a collapsed accordion: open it. No-op on desktop. */
export async function expandSummary(page: Page): Promise<void> {
  const toggle = page.getByRole('button', { name: TEXT.chat.summaryTitle });
  if ((await toggle.count()) > 0 && (await toggle.getAttribute('aria-expanded')) === 'false') await toggle.click();
}

export async function badgeText(page: Page): Promise<string> {
  return (await page.locator('.decision-badge').first().innerText()).trim();
}

export async function sendChat(page: Page, text: string): Promise<void> {
  await page.getByLabel('Twoja wiadomość').fill(text);
  await page.getByRole('button', { name: 'Wyślij', exact: true }).click();
}

/** No horizontal scrolling at the current viewport width (AC-45). */
export async function expectNoHorizontalScroll(page: Page): Promise<void> {
  const width = page.viewportSize()!.width;
  const scrollWidth = await page.evaluate(() => document.documentElement.scrollWidth);
  expect(scrollWidth).toBeLessThanOrEqual(width);
}

export const RETURN_LAPTOP: FormInput = {
  type: 'Zwrot',
  category: 'Laptop',
  model: 'Lenovo ThinkPad T14 Gen 4',
  daysAgo: 5,
  photo: IMAGES.laptop2,
};

export const COMPLAINT_PHONE: FormInput = {
  type: 'Reklamacja',
  category: 'Smartfon',
  model: 'Samsung Galaxy S23',
  daysAgo: 60,
  reason: 'Ekran jest pęknięty po upadku telefonu, a dotyk działa nieprawidłowo.',
  photo: IMAGES.phone1,
};

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

export async function fillForm(page: Page, input: FormInput): Promise<void> {
  expect(fs.existsSync(input.photo), `missing example photo ${input.photo}`).toBe(true);
  await page.getByRole('radio', { name: new RegExp(input.type) }).check();
  await page.getByLabel(TEXT.form.category).click();
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
  await expect(page.getByText(TEXT.chat.summaryTitle)).toBeVisible({ timeout: FIRST_DECISION_TIMEOUT_MS });
  await expect(page.getByText(DECISION_BADGE).first()).toBeVisible();
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

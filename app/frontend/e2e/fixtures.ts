import { test as base, expect } from '@playwright/test';

/** Exact Polish texts (mirrors src/app/core/labels.ts; copied so specs stay free of app imports). */
export const TEXT = {
  demoNote: 'Wersja demonstracyjna — przykładowy regulamin fikcyjnego sklepu. Decyzja jest wstępna.',
  form: {
    caseType: 'Rodzaj zgłoszenia',
    category: 'Kategoria sprzętu',
    modelName: 'Nazwa lub model urządzenia',
    purchaseDate: 'Data zakupu',
    reasonNeutral: 'Powód',
    reasonOptional: 'Powód (opcjonalnie)',
    photo: 'Zdjęcie urządzenia',
    choosePhoto: 'Wybierz zdjęcie',
    removePhoto: 'Usuń',
    submit: 'Wyślij zgłoszenie',
  },
  newCase: 'Nowe zgłoszenie',
  photoHint: {
    RETURN: 'Pokaż całe urządzenie, tak aby było widać, że nie jest uszkodzone ani używane.',
    COMPLAINT: 'Pokaż wyraźnie uszkodzenie lub usterkę.',
  },
  errors: {
    caseTypeRequired: 'Wybierz rodzaj zgłoszenia.',
    categoryRequired: 'Wybierz kategorię sprzętu.',
    modelName: 'Podaj nazwę lub model urządzenia (2–100 znaków).',
    purchaseDateRequired: 'Podaj datę zakupu.',
    reasonComplaint: 'Opisz problem (co najmniej 10 znaków).',
    photoRequired: 'Dodaj zdjęcie urządzenia.',
  },
  progress: {
    analysing: 'Analizujemy zdjęcie',
    deciding: 'Przygotowujemy decyzję',
    note: 'To może potrwać do minuty.',
    retry: 'Spróbuj ponownie',
    backToForm: 'Wróć do formularza',
  },
  chat: {
    summaryTitle: 'Twoje zgłoszenie',
  },
} as const;

type ConsoleGuard = { consoleGuard: void };
type GuardOptions = {
  /** Console messages a test deliberately provokes (e.g. the browser's own line for an expected 502); default none. */
  expectedConsoleErrors: RegExp[];
};

/**
 * Auto fixture (TAC-003-06): records browser console errors/warnings and uncaught page errors
 * during each test and fails the test at teardown, listing them.
 */
export const test = base.extend<ConsoleGuard & GuardOptions>({
  expectedConsoleErrors: [[], { option: true }],
  consoleGuard: [
    async ({ page, expectedConsoleErrors }, use) => {
      const problems: string[] = [];
      page.on('console', (msg) => {
        if (msg.type() !== 'error' && msg.type() !== 'warning') return;
        const { url, lineNumber, columnNumber } = msg.location();
        if (expectedConsoleErrors.some((re) => re.test(msg.text()))) return;
        problems.push(`console.${msg.type()}: ${msg.text()} (${url}:${lineNumber}:${columnNumber})`);
      });
      page.on('pageerror', (err) => problems.push(`pageerror: ${err.message}`));
      await use();
      expect(problems, `browser console errors/warnings or uncaught exceptions:\n${problems.join('\n')}`).toEqual([]);
    },
    { auto: true },
  ],
});

export { expect } from '@playwright/test';

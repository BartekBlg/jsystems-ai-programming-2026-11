import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withFetch } from '@angular/common/http';
import localePl from '@angular/common/locales/pl';
import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { MAT_DATE_LOCALE, provideNativeDateAdapter } from '@angular/material/core';
import { provideMarkdown } from 'ngx-markdown';

registerLocaleData(localePl);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(withFetch()),
    provideMarkdown(),
    provideNativeDateAdapter(),
    { provide: MAT_DATE_LOCALE, useValue: 'pl-PL' },
    { provide: LOCALE_ID, useValue: 'pl' },
  ],
};

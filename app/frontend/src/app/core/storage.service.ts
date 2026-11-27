import { Injectable } from '@angular/core';
import { FORM_DRAFT_KEY, FormDraft, STORED_CASE_KEY, StoredCase } from './models';

/**
 * localStorage persistence for the current case and the form draft (ADR-003 §3.3, §4; AC-38, AC-40).
 * Every access is guarded: storage may be unavailable, full, or hold data from an older schema.
 * Invalid entries are removed so the app starts clean instead of failing on every load.
 */
@Injectable({ providedIn: 'root' })
export class StorageService {
  loadCase(): StoredCase | null {
    const parsed = this.readJson(STORED_CASE_KEY);
    if (parsed === undefined) {
      return null;
    }
    if (!isStoredCase(parsed)) {
      this.remove(STORED_CASE_KEY);
      return null;
    }
    return parsed;
  }

  saveCase(storedCase: StoredCase): boolean {
    return this.write(STORED_CASE_KEY, storedCase);
  }

  clearCase(): void {
    this.remove(STORED_CASE_KEY);
  }

  loadDraft(): FormDraft | null {
    const parsed = this.readJson(FORM_DRAFT_KEY);
    if (parsed === undefined) {
      return null;
    }
    if (!isFormDraft(parsed)) {
      this.remove(FORM_DRAFT_KEY);
      return null;
    }
    return parsed;
  }

  saveDraft(draft: FormDraft): boolean {
    return this.write(FORM_DRAFT_KEY, draft);
  }

  clearDraft(): void {
    this.remove(FORM_DRAFT_KEY);
  }

  /** Returns undefined when the key is absent, unreadable or not valid JSON (invalid JSON is removed). */
  private readJson(key: string): unknown | undefined {
    let raw: string | null;
    try {
      raw = localStorage.getItem(key);
    } catch {
      return undefined;
    }
    if (raw === null) {
      return undefined;
    }
    try {
      return JSON.parse(raw) as unknown;
    } catch {
      this.remove(key);
      return undefined;
    }
  }

  private write(key: string, value: unknown): boolean {
    try {
      localStorage.setItem(key, JSON.stringify(value));
      return true;
    } catch {
      return false;
    }
  }

  private remove(key: string): void {
    try {
      localStorage.removeItem(key);
    } catch {
      // Nothing more can be done when storage is inaccessible.
    }
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function isStoredCase(value: unknown): value is StoredCase {
  return (
    isRecord(value) &&
    value['version'] === 1 &&
    isStoredCaseContext(value['caseContext']) &&
    typeof value['signature'] === 'string' &&
    typeof value['thumbnailDataUrl'] === 'string' &&
    typeof value['photoFileName'] === 'string' &&
    Array.isArray(value['messages']) &&
    value['messages'].every(isChatMessage) &&
    isDecisionCategory(value['currentDecision'])
  );
}

function isStoredCaseContext(value: unknown): boolean {
  return (
    isRecord(value) &&
    isCaseType(value['caseType']) &&
    isRecord(value['form']) &&
    isRecord(value['assessment']) &&
    typeof value['contextVersion'] === 'number'
  );
}

function isChatMessage(value: unknown): boolean {
  return (
    isRecord(value) &&
    typeof value['id'] === 'string' &&
    (value['role'] === 'user' || value['role'] === 'assistant') &&
    typeof value['text'] === 'string' &&
    typeof value['createdAt'] === 'string' &&
    (value['status'] === undefined || isMessageStatus(value['status'])) &&
    (value['decisionCategory'] === undefined || isDecisionCategory(value['decisionCategory']))
  );
}

function isCaseType(value: unknown): boolean {
  return value === 'RETURN' || value === 'COMPLAINT';
}

function isMessageStatus(value: unknown): boolean {
  return value === 'pending' || value === 'sent' || value === 'failed';
}

function isDecisionCategory(value: unknown): boolean {
  return value === 'ACCEPTED' || value === 'REJECTED' || value === 'MORE_INFO_NEEDED' || value === 'ESCALATED';
}

function isFormDraft(value: unknown): value is FormDraft {
  return isRecord(value) && typeof value['interrupted'] === 'boolean';
}

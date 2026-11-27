import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { loadJsonRecording } from '../../testing/recording-loader';
import { DecisionResponse, FORM_DRAFT_KEY, FormDraft, STORED_CASE_KEY, StoredCase } from './models';
import { StorageService } from './storage.service';

function syntheticCase(overrides: Partial<StoredCase> = {}): StoredCase {
  return {
    version: 1,
    caseContext: {
      contextVersion: 1,
      caseType: 'RETURN',
      form: {
        caseType: 'RETURN',
        category: 'HEADPHONES',
        modelName: 'Model Testowy X1',
        purchaseDate: '2026-01-15',
        reason: 'Syntetyczny powód zwrotu',
      },
      submittedAt: '2026-02-01T10:00:00Z',
      assessment: {
        photoSufficient: true,
        insufficiencyReason: '',
        visibleDescription: 'Syntetyczny opis zdjęcia',
        confidence: 'HIGH',
        damageVisible: false,
        signsOfUse: [],
        resellableAsNew: true,
      },
      assessmentSource: 'LLM',
      prescreen: null,
      consistencySignals: null,
      ruleFacts: {},
      decision: { category: 'ACCEPTED' },
    },
    signature: 'synthetic-signature',
    thumbnailDataUrl: 'data:image/jpeg;base64,AAAA',
    photoFileName: 'synthetic.jpg',
    messages: [
      { id: 'm1', role: 'user', text: 'Syntetyczna wiadomość', createdAt: '2026-02-01T10:01:00Z', status: 'sent' },
    ],
    currentDecision: 'ACCEPTED',
    ...overrides,
  };
}

const DRAFT: FormDraft = {
  caseType: 'COMPLAINT',
  category: 'CAMERA',
  modelName: 'Syntetyczna kamera',
  purchaseDate: '2026-03-03',
  reason: 'Syntetyczny opis',
  interrupted: true,
};

describe('StorageService', () => {
  let service: StorageService;

  beforeEach(() => {
    localStorage.clear();
    service = new StorageService();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
  });

  describe('case', () => {
    it('round-trips a stored case', () => {
      const stored = syntheticCase();
      expect(service.saveCase(stored)).toBe(true);
      expect(service.loadCase()).toEqual(stored);
    });

    it('returns null when no case is stored', () => {
      expect(service.loadCase()).toBeNull();
    });

    it('clears corrupted JSON and returns null', () => {
      localStorage.setItem(STORED_CASE_KEY, '{not json');
      expect(service.loadCase()).toBeNull();
      expect(localStorage.getItem(STORED_CASE_KEY)).toBeNull();
    });

    it('clears a case with a different schema version', () => {
      const v2 = { ...syntheticCase(), version: 2 };
      localStorage.setItem(STORED_CASE_KEY, JSON.stringify(v2));
      expect(service.loadCase()).toBeNull();
      expect(localStorage.getItem(STORED_CASE_KEY)).toBeNull();
    });

    it('clears a case missing required fields', () => {
      const { messages: _messages, ...withoutMessages } = syntheticCase();
      localStorage.setItem(STORED_CASE_KEY, JSON.stringify(withoutMessages));
      expect(service.loadCase()).toBeNull();
      expect(localStorage.getItem(STORED_CASE_KEY)).toBeNull();
    });

    it.each([
      ['a null message', { ...syntheticCase(), messages: [null] }],
      ['a message with an unsupported role', {
        ...syntheticCase(),
        messages: [{ ...syntheticCase().messages[0], role: 'system' }],
      }],
      ['an unknown current decision', { ...syntheticCase(), currentDecision: 'UNKNOWN' }],
    ])('clears a schema-mismatched case containing %s', (_description, invalidCase) => {
      localStorage.setItem(STORED_CASE_KEY, JSON.stringify(invalidCase));

      expect(service.loadCase()).toBeNull();
      expect(localStorage.getItem(STORED_CASE_KEY)).toBeNull();
    });

    it('loads a valid case based on the T26 decision recording', async () => {
      const recording = await loadJsonRecording<DecisionResponse>('decision-return-ok');
      const { body } = recording;
      const recordedCase: StoredCase = {
        version: 1,
        caseContext: body.caseContext,
        signature: body.signature,
        thumbnailDataUrl: 'data:image/jpeg;base64,AAAA',
        photoFileName: 'recorded-laptop.jpg',
        messages: [{
          id: 'recorded-decision',
          role: 'assistant',
          text: body.decision.message,
          createdAt: body.caseContext.submittedAt,
          decisionCategory: body.decision.category,
        }],
        currentDecision: body.decision.category,
      };
      localStorage.setItem(STORED_CASE_KEY, JSON.stringify(recordedCase));

      expect(service.loadCase()).toEqual(recordedCase);
    });

    it('returns false instead of throwing when the quota is exceeded', () => {
      vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
        throw new DOMException('quota', 'QuotaExceededError');
      });
      expect(service.saveCase(syntheticCase())).toBe(false);
    });

    it('returns null instead of throwing when localStorage read fails', () => {
      vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
        throw new DOMException('denied', 'SecurityError');
      });
      expect(service.loadCase()).toBeNull();
    });

    it('clearCase removes the stored case', () => {
      service.saveCase(syntheticCase());
      service.clearCase();
      expect(localStorage.getItem(STORED_CASE_KEY)).toBeNull();
      expect(service.loadCase()).toBeNull();
    });
  });

  describe('draft', () => {
    it('round-trips a form draft', () => {
      expect(service.saveDraft(DRAFT)).toBe(true);
      expect(service.loadDraft()).toEqual(DRAFT);
    });

    it('returns null when no draft is stored', () => {
      expect(service.loadDraft()).toBeNull();
    });

    it('clears a draft without the interrupted flag', () => {
      const { interrupted: _interrupted, ...withoutFlag } = DRAFT;
      localStorage.setItem(FORM_DRAFT_KEY, JSON.stringify(withoutFlag));
      expect(service.loadDraft()).toBeNull();
      expect(localStorage.getItem(FORM_DRAFT_KEY)).toBeNull();
    });

    it('clears corrupted draft JSON and returns null', () => {
      localStorage.setItem(FORM_DRAFT_KEY, '[[');
      expect(service.loadDraft()).toBeNull();
      expect(localStorage.getItem(FORM_DRAFT_KEY)).toBeNull();
    });

    it('returns false instead of throwing when saving the draft fails', () => {
      vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
        throw new DOMException('quota', 'QuotaExceededError');
      });
      expect(service.saveDraft(DRAFT)).toBe(false);
    });

    it('clearDraft removes the stored draft and leaves the case untouched', () => {
      service.saveCase(syntheticCase());
      service.saveDraft(DRAFT);
      service.clearDraft();
      expect(service.loadDraft()).toBeNull();
      expect(service.loadCase()).not.toBeNull();
    });
  });
});

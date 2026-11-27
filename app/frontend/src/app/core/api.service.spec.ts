import { provideHttpClient, withFetch } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { loadJsonRecording } from '../../testing/recording-loader';
import { ApiError, ApiService } from './api.service';
import { AssessmentResponse, CaseForm, DecisionResponse } from './models';

const FORM: CaseForm = {
  caseType: 'RETURN',
  category: 'LAPTOP',
  modelName: 'Lenovo ThinkPad T14 Gen 4',
  purchaseDate: '2026-11-21',
  reason: '',
};

describe('ApiService', () => {
  let api: ApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withFetch()), provideHttpClientTesting()],
    });
    api = TestBed.inject(ApiService);
    http = TestBed.inject(HttpTestingController);
  });

  async function failWith(recording: string, url: string): Promise<ApiError> {
    const rec = await loadJsonRecording(recording);
    const promise = api.assess(FORM, new File(['x'], 'photo.jpg')).catch((e: unknown) => e);
    http.expectOne(url).flush(rec.body, { status: rec.status, statusText: 'Error', headers: rec.headers });
    const error = await promise;
    expect(error).toBeInstanceOf(ApiError);
    return error as ApiError;
  }

  it('assess posts multipart FormData with a JSON form blob and the original photo', async () => {
    const rec = await loadJsonRecording<AssessmentResponse>('assessment-return-ok');
    const photo = new File(['photo-bytes'], 'laptop.webp', { type: 'image/webp' });
    const promise = api.assess(FORM, photo);

    const req = http.expectOne('/api/cases/assessments');
    expect(req.request.method).toBe('POST');
    const body = req.request.body as FormData;
    expect(body).toBeInstanceOf(FormData);
    const formPart = body.get('form') as Blob;
    expect(formPart.type).toBe('application/json');
    expect(JSON.parse(await formPart.text())).toEqual(FORM);
    expect(body.get('photo')).toBe(photo);
    req.flush(rec.body, { status: rec.status, statusText: 'OK', headers: rec.headers });

    const result = await promise;
    expect(result.signature).toBe(rec.body.signature);
    expect(result.caseContext.caseType).toBe('RETURN');
  });

  it('decide posts the case context and signature as JSON', async () => {
    const assessment = await loadJsonRecording<AssessmentResponse>('assessment-return-ok');
    const rec = await loadJsonRecording<DecisionResponse>('decision-return-ok');
    const promise = api.decide(assessment.body.caseContext, assessment.body.signature);

    const req = http.expectOne('/api/cases/decisions');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      caseContext: assessment.body.caseContext,
      signature: assessment.body.signature,
    });
    req.flush(rec.body, { status: rec.status, statusText: 'OK', headers: rec.headers });

    const result = await promise;
    expect(result.decision.category).toBe(rec.body.decision.category);
    expect(result.decision.message).toBe(rec.body.decision.message);
  });

  it('400 validation exposes status, code, detail and fieldErrors', async () => {
    const rec = await loadJsonRecording<{ detail: string; fieldErrors: unknown[] }>('problem-400-validation');
    const error = await failWith('problem-400-validation', '/api/cases/assessments');
    expect(error.status).toBe(400);
    expect(error.code).toBe('VALIDATION_FAILED');
    expect(error.detail).toBe(rec.body.detail);
    expect(error.fieldErrors).toEqual(rec.body.fieldErrors);
  });

  it('413 maps to PHOTO_TOO_LARGE', async () => {
    const error = await failWith('problem-413-too-large', '/api/cases/assessments');
    expect(error.status).toBe(413);
    expect(error.code).toBe('PHOTO_TOO_LARGE');
  });

  it('429 keeps the server Polish detail', async () => {
    const rec = await loadJsonRecording<{ detail: string }>('problem-429');
    const error = await failWith('problem-429', '/api/cases/assessments');
    expect(error.code).toBe('RATE_LIMITED');
    expect(error.detail).toBe(rec.body.detail);
    expect(error.fieldErrors).toBeUndefined();
  });

  it('502 maps to LLM_FAILED', async () => {
    const error = await failWith('problem-502-llm-failed', '/api/cases/assessments');
    expect(error.status).toBe(502);
    expect(error.code).toBe('LLM_FAILED');
  });

  it('401 signature problem maps to INVALID_CASE_SIGNATURE on decide', async () => {
    const rec = await loadJsonRecording('problem-401-signature');
    const assessment = await loadJsonRecording<AssessmentResponse>('assessment-return-ok');
    const promise = api.decide(assessment.body.caseContext, 'x').catch((e: unknown) => e);
    http.expectOne('/api/cases/decisions').flush(rec.body, { status: rec.status, statusText: 'E', headers: rec.headers });
    expect(((await promise) as ApiError).code).toBe('INVALID_CASE_SIGNATURE');
  });

  it('409 decided is typed as DECISION_ALREADY_MADE', async () => {
    const error = await failWith('problem-409-decided', '/api/cases/assessments');
    expect(error.status).toBe(409);
    expect(error.code).toBe('DECISION_ALREADY_MADE');
  });

  it('a network error becomes code INTERNAL with status 0', async () => {
    const promise = api.assess(FORM, new File(['x'], 'p.jpg')).catch((e: unknown) => e);
    http.expectOne('/api/cases/assessments').error(new ProgressEvent('error'));
    const error = (await promise) as ApiError;
    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(0);
    expect(error.code).toBe('INTERNAL');
  });

  it('an unparseable error body becomes INTERNAL keeping the HTTP status', async () => {
    // Derived from problem-502-llm-failed: same status, body replaced by the HTML-free empty string (proxy-style failure).
    const rec = await loadJsonRecording('problem-502-llm-failed');
    const promise = api.assess(FORM, new File(['x'], 'p.jpg')).catch((e: unknown) => e);
    http.expectOne('/api/cases/assessments').flush('', { status: rec.status, statusText: 'Bad Gateway' });
    const error = (await promise) as ApiError;
    expect(error.status).toBe(502);
    expect(error.code).toBe('INTERNAL');
  });

  it('rejects with INTERNAL and cancels the request when the signal aborts', async () => {
    const controller = new AbortController();
    const promise = api.assess(FORM, new File(['x'], 'p.jpg'), controller.signal).catch((e: unknown) => e);
    const req = http.expectOne('/api/cases/assessments');
    controller.abort();
    const error = (await promise) as ApiError;
    expect(error.code).toBe('INTERNAL');
    expect(req.cancelled).toBe(true);
  });
});

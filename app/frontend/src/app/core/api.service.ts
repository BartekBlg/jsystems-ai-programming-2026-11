import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ApiProblem,
  AssessmentResponse,
  CaseContext,
  CaseForm,
  DecisionResponse,
  ErrorCode,
  FieldError,
} from './models';

/** Typed backend failure (ADR-003 §5). Raw HTTP/network text is never meant for display (AC-43). */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: ErrorCode,
    readonly detail: string,
    readonly fieldErrors?: FieldError[],
  ) {
    super(`${code} (${status})`);
    this.name = 'ApiError';
  }
}

const KNOWN_CODES: readonly ErrorCode[] = [
  'VALIDATION_FAILED',
  'UNSUPPORTED_IMAGE',
  'PHOTO_TOO_LARGE',
  'INVALID_CASE_SIGNATURE',
  'DECISION_ALREADY_MADE',
  'NOT_FOUND',
  'RATE_LIMITED',
  'LLM_FAILED',
  'LLM_TIMEOUT',
  'INTERNAL',
];

/** Converts a parsed problem+json object (or anything else) into an ApiError. */
export function toApiError(status: number, body: unknown): ApiError {
  if (typeof body === 'object' && body !== null) {
    const problem = body as Partial<ApiProblem>;
    if (typeof problem.code === 'string' && KNOWN_CODES.includes(problem.code)) {
      return new ApiError(
        status,
        problem.code,
        typeof problem.detail === 'string' ? problem.detail : '',
        Array.isArray(problem.fieldErrors) ? problem.fieldErrors : undefined,
      );
    }
  }
  return new ApiError(status, 'INTERNAL', '');
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  assess(form: CaseForm, photo: File, signal?: AbortSignal): Promise<AssessmentResponse> {
    const data = new FormData();
    data.append('form', new Blob([JSON.stringify(form)], { type: 'application/json' }), 'form.json');
    data.append('photo', photo);
    return this.run(this.http.post<AssessmentResponse>('/api/cases/assessments', data), signal);
  }

  decide(caseContext: CaseContext, signature: string, signal?: AbortSignal): Promise<DecisionResponse> {
    return this.run(this.http.post<DecisionResponse>('/api/cases/decisions', { caseContext, signature }), signal);
  }

  private run<T>(request: Observable<T>, signal?: AbortSignal): Promise<T> {
    return new Promise<T>((resolve, reject) => {
      if (signal?.aborted) {
        reject(new ApiError(0, 'INTERNAL', ''));
        return;
      }
      const onAbort = (): void => {
        subscription.unsubscribe();
        reject(new ApiError(0, 'INTERNAL', ''));
      };
      const subscription = request.subscribe({
        next: (value) => {
          signal?.removeEventListener('abort', onAbort);
          resolve(value);
        },
        error: (error: unknown) => {
          signal?.removeEventListener('abort', onAbort);
          reject(error instanceof HttpErrorResponse ? toApiError(error.status, error.error) : new ApiError(0, 'INTERNAL', ''));
        },
      });
      signal?.addEventListener('abort', onAbort, { once: true });
    });
  }
}

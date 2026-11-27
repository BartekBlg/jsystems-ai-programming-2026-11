/**
 * Types mirroring ADR-000 §5, ADR-001 §4 and ADR-005 D2/D3.
 * The browser never modifies a CaseContext: it stores and echoes it together with its signature.
 * Server-computed parts (ruleFacts, prescreen, consistencySignals) are therefore opaque here.
 */

export type CaseType = 'RETURN' | 'COMPLAINT';

export type EquipmentCategory =
  | 'LAPTOP'
  | 'SMARTPHONE'
  | 'TABLET'
  | 'MONITOR'
  | 'HEADPHONES'
  | 'SMARTWATCH'
  | 'GAME_CONSOLE'
  | 'CAMERA'
  | 'OTHER';

export type DecisionCategory = 'ACCEPTED' | 'REJECTED' | 'MORE_INFO_NEEDED' | 'ESCALATED';

export type Confidence = 'HIGH' | 'MEDIUM' | 'LOW';

export type AssessmentSource = 'LLM' | 'DECISIONS';

export interface CaseForm {
  caseType: CaseType;
  category: EquipmentCategory;
  modelName: string;
  /** ISO local date, YYYY-MM-DD */
  purchaseDate: string;
  reason: string;
}

interface ImageAssessmentBase {
  photoSufficient: boolean;
  insufficiencyReason: string;
  visibleDescription: string;
  confidence: Confidence;
}

export type SignOfUse = 'SCRATCHES' | 'DIRT' | 'WEAR' | 'MISSING_PROTECTIVE_FILM' | 'MISSING_ACCESSORIES';

export interface ReturnImageAssessment extends ImageAssessmentBase {
  damageVisible: boolean;
  signsOfUse: SignOfUse[];
  resellableAsNew: boolean;
}

export type ProbableCause =
  | 'MANUFACTURING_DEFECT'
  | 'NORMAL_WEAR'
  | 'MECHANICAL_DAMAGE'
  | 'LIQUID_DAMAGE'
  | 'UNKNOWN';

export interface ComplaintImageAssessment extends ImageAssessmentBase {
  defectVisible: boolean;
  defectType: string;
  defectLocation: string;
  probableCause: ProbableCause;
  causeReasoning: string;
}

export type ImageAssessment = ReturnImageAssessment | ComplaintImageAssessment;

/** ADR-005 D2: single field set; nullable fields are always present as null. */
export interface CaseContext {
  contextVersion: number;
  caseType: CaseType;
  form: CaseForm;
  submittedAt: string;
  assessment: ImageAssessment;
  assessmentSource: AssessmentSource;
  prescreen: unknown | null;
  consistencySignals: unknown | null;
  ruleFacts: Record<string, unknown>;
  decision: { category: DecisionCategory } | null;
}

export interface AssessmentResponse {
  caseContext: CaseContext;
  signature: string;
}

export interface Decision {
  category: DecisionCategory;
  /** Markdown */
  message: string;
}

export interface DecisionResponse {
  decision: Decision;
  caseContext: CaseContext;
  signature: string;
}

export type ChatRole = 'assistant' | 'user';
export type MessageStatus = 'pending' | 'sent' | 'failed';

export interface ChatMessage {
  id: string;
  role: ChatRole;
  text: string;
  createdAt: string;
  /** user messages only */
  status?: MessageStatus;
  /** assistant messages that carry a decision or an update */
  decisionCategory?: DecisionCategory;
}

/** Wire shape of a history item in ChatRequest (ADR-001 §4). */
export interface ChatRequestMessage {
  id: string;
  role: ChatRole;
  text: string;
}

export interface ChatRequest {
  caseContext: CaseContext;
  signature: string;
  messages: ChatRequestMessage[];
}

export type ChatStreamEvent =
  | { type: 'decision'; category: DecisionCategory }
  | { type: 'delta'; text: string }
  | { type: 'done'; category: DecisionCategory }
  | { type: 'error'; code: 'LLM_FAILED' | 'LLM_TIMEOUT' };

export type ErrorCode =
  | 'VALIDATION_FAILED'
  | 'UNSUPPORTED_IMAGE'
  | 'PHOTO_TOO_LARGE'
  | 'INVALID_CASE_SIGNATURE'
  | 'DECISION_ALREADY_MADE'
  | 'NOT_FOUND'
  | 'RATE_LIMITED'
  | 'LLM_FAILED'
  | 'LLM_TIMEOUT'
  | 'INTERNAL';

export interface FieldError {
  field: string;
  message: string;
}

export interface ApiProblem {
  type?: string;
  title?: string;
  status: number;
  detail: string;
  code: ErrorCode;
  instance?: string;
  fieldErrors?: FieldError[];
}

export const STORED_CASE_KEY = 'hsdc.case.v1';
export const FORM_DRAFT_KEY = 'hsdc.draft.v1';

export interface StoredCase {
  version: 1;
  caseContext: CaseContext;
  signature: string;
  thumbnailDataUrl: string;
  photoFileName: string;
  messages: ChatMessage[];
  currentDecision: DecisionCategory;
}

export interface FormDraft {
  caseType: CaseType | null;
  category: EquipmentCategory | null;
  modelName: string;
  purchaseDate: string | null;
  reason: string;
  interrupted: boolean;
}

export type Screen = 'form' | 'progress' | 'chat';
export type StepStatus = 'waiting' | 'running' | 'done';

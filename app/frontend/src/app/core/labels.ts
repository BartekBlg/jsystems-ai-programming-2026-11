import { CaseType, DecisionCategory, EquipmentCategory } from './models';

/** Every Polish UI text (PRD §6, §8, §9; ADR-003; ADR-005 D3, D8, D9). AC-44. */
export const LABELS = {
  brandWordmark: 'ElektroDemo',
  productName: 'Copilot serwisowy',
  demoNote: 'Wersja demonstracyjna — przykładowy regulamin fikcyjnego sklepu. Decyzja jest wstępna.',
  newCase: 'Nowe zgłoszenie',

  form: {
    title: 'Zgłoś zwrot lub reklamację',
    intro:
      'Wypełnij formularz i dodaj zdjęcie urządzenia. Otrzymasz wstępną decyzję, a potem możesz zadać pytania.',
    caseType: 'Rodzaj zgłoszenia',
    category: 'Kategoria sprzętu',
    modelName: 'Nazwa lub model urządzenia',
    modelNamePlaceholder: 'np. Lenovo ThinkPad T14 Gen 4',
    purchaseDate: 'Data zakupu',
    reasonNeutral: 'Powód',
    reasonOptional: 'Powód (opcjonalnie)',
    photo: 'Zdjęcie urządzenia',
    choosePhoto: 'Wybierz zdjęcie',
    dropHint: 'lub przeciągnij i upuść plik tutaj',
    removePhoto: 'Usuń',
    submit: 'Wyślij zgłoszenie',
    interrupted: 'Przetwarzanie zostało przerwane. Wyślij zgłoszenie ponownie.',
  },

  photoHint: {
    RETURN: 'Pokaż całe urządzenie, tak aby było widać, że nie jest uszkodzone ani używane.',
    COMPLAINT: 'Pokaż wyraźnie uszkodzenie lub usterkę.',
  } satisfies Record<CaseType, string>,

  errors: {
    caseTypeRequired: 'Wybierz rodzaj zgłoszenia.',
    categoryRequired: 'Wybierz kategorię sprzętu.',
    modelName: 'Podaj nazwę lub model urządzenia (2–100 znaków).',
    purchaseDateRequired: 'Podaj datę zakupu.',
    purchaseDateFuture: 'Data zakupu nie może być z przyszłości.',
    reasonComplaint: 'Opisz problem (co najmniej 10 znaków).',
    reasonTooLong: 'Powód może mieć najwyżej 2000 znaków.',
    photoRequired: 'Dodaj zdjęcie urządzenia.',
    photoUnsupported: 'Nieobsługiwany format. Dodaj zdjęcie JPEG, PNG lub WebP.',
    photoTooLarge: 'Zdjęcie przekracza limit 10 MB.',
    decisionFailed: 'Nie udało się przygotować decyzji. Spróbuj ponownie za chwilę.',
    invalidSignature: 'Nie udało się odczytać zgłoszenia. Rozpocznij nowe zgłoszenie.',
    storageFailed: 'Nie udało się zapisać rozmowy w przeglądarce.',
  },

  progress: {
    analysing: 'Analizujemy zdjęcie',
    deciding: 'Przygotowujemy decyzję',
    note: 'To może potrwać do minuty.',
    retry: 'Spróbuj ponownie',
    backToForm: 'Wróć do formularza',
    statusWaiting: 'oczekuje',
    statusRunning: 'w toku',
    statusDone: 'gotowe',
  },

  chat: {
    summaryTitle: 'Twoje zgłoszenie',
    caseType: 'Rodzaj',
    category: 'Kategoria',
    model: 'Model',
    purchaseDate: 'Data zakupu',
    reason: 'Powód',
    noReason: 'brak',
    showMore: 'pokaż więcej',
    showLess: 'pokaż mniej',
    photo: 'Zdjęcie',
    openPhoto: 'Powiększ zdjęcie',
    decision: 'Decyzja',
    composerLabel: 'Twoja wiadomość',
    send: 'Wyślij',
    typing: 'Agent pisze odpowiedź',
    newReply: 'Nowa odpowiedź agenta',
    notSent: 'Nie wysłano',
    retry: 'Ponów',
    agent: 'Agent',
    you: 'Ty',
    close: 'Zamknij',
  },

  confirmNewCase: {
    text: 'Rozpocząć nowe zgłoszenie? Bieżąca rozmowa zostanie usunięta.',
    confirm: 'Rozpocznij nowe',
    cancel: 'Anuluj',
  },
} as const;

export const CASE_TYPE_LABELS: Record<CaseType, string> = {
  RETURN: 'Zwrot',
  COMPLAINT: 'Reklamacja',
};

export const CASE_TYPE_DESCRIPTIONS: Record<CaseType, string> = {
  RETURN: 'Rezygnuję z zakupu w ciągu 14 dni',
  COMPLAINT: 'Urządzenie jest uszkodzone lub nie działa',
};

/** PRD §8 order. */
export const CATEGORY_LABELS: Record<EquipmentCategory, string> = {
  LAPTOP: 'Laptop',
  SMARTPHONE: 'Smartfon',
  TABLET: 'Tablet',
  MONITOR: 'Monitor',
  HEADPHONES: 'Słuchawki',
  SMARTWATCH: 'Smartwatch',
  GAME_CONSOLE: 'Konsola do gier',
  CAMERA: 'Aparat fotograficzny',
  OTHER: 'Inne',
};

export const DECISION_LABELS: Record<DecisionCategory, string> = {
  ACCEPTED: 'Przyjęte',
  REJECTED: 'Odrzucone',
  MORE_INFO_NEEDED: 'Potrzebne dodatkowe informacje',
  ESCALATED: 'Przekazane do specjalisty',
};

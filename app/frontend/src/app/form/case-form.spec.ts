import { AbstractControl, NgControl } from '@angular/forms';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { MAT_DATE_LOCALE, provideNativeDateAdapter } from '@angular/material/core';
import { CASE_TYPE_LABELS, LABELS } from '../core/labels';
import { FormDraft } from '../core/models';
import { CaseFormComponent } from './case-form';

const MB = 1024 * 1024;
const JPEG_HEADER = [0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10, 0x4a, 0x46, 0x49, 0x46, 0x00, 0x01];
const GIF_HEADER = [0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00];
const FIELD_ORDER = ['caseType', 'category', 'modelName', 'purchaseDate', 'reason', 'photo'];

function imageFile(
  name: string,
  header: number[],
  size = header.length,
  type = 'image/jpeg',
): File {
  const bytes = new Uint8Array(size);
  bytes.set(header);
  return new File([bytes], name, { type });
}

describe('CaseFormComponent', () => {
  let fixture: ComponentFixture<CaseFormComponent>;
  let urlCounter: number;

  beforeEach(async () => {
    urlCounter = 0;
    globalThis.URL.createObjectURL = vi.fn(() => `blob:preview-${++urlCounter}`);
    globalThis.URL.revokeObjectURL = vi.fn();
    await TestBed.configureTestingModule({
      imports: [CaseFormComponent],
      providers: [provideNativeDateAdapter(), { provide: MAT_DATE_LOCALE, useValue: 'pl-PL' }],
    }).compileComponents();
    fixture = TestBed.createComponent(CaseFormComponent);
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  /** Prefill inputs are read on init, so they must be set before the first change detection. */
  function renderWith(inputs: Record<string, unknown>): void {
    fixture.destroy();
    fixture = TestBed.createComponent(CaseFormComponent);
    for (const [name, value] of Object.entries(inputs)) {
      fixture.componentRef.setInput(name, value);
    }
    fixture.detectChanges();
  }

  function el(selector: string): HTMLElement | null {
    return fixture.nativeElement.querySelector(selector) as HTMLElement | null;
  }

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  function control(name: string): AbstractControl {
    const debugEl = fixture.debugElement.query(By.css(`[formcontrolname="${name}"]`));
    return debugEl.injector.get(NgControl).control!;
  }

  function type(name: string, value: string): void {
    const input = el(`[formcontrolname="${name}"]`) as HTMLInputElement | HTMLTextAreaElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function submit(): void {
    el('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();
  }

  function fillValid(photo: File = imageFile('a.jpg', JPEG_HEADER)): void {
    control('caseType').setValue('RETURN');
    control('category').setValue('LAPTOP');
    type('modelName', 'Lenovo ThinkPad T14');
    control('purchaseDate').setValue(new Date(2024, 0, 15, 0, 30));
    type('reason', '  nie działa  ');
    control('photo').setValue(photo);
    fixture.detectChanges();
  }

  async function pickFile(file: File): Promise<void> {
    const input = el('app-photo-picker input[type="file"]') as HTMLInputElement;
    Object.defineProperty(input, 'files', { value: [file], configurable: true });
    input.dispatchEvent(new Event('change'));
    await new Promise((resolve) => setTimeout(resolve, 0));
    fixture.detectChanges();
  }

  describe('layout and defaults', () => {
    it('renders the six fields in PRD order', () => {
      const names = Array.from(
        fixture.nativeElement.querySelectorAll('[formcontrolname]') as NodeListOf<HTMLElement>,
      ).map((node) => node.getAttribute('formcontrolname'));
      expect(names).toEqual(FIELD_ORDER);
    });

    it('has no case type checked and no category selected by default', () => {
      expect(el('input[type="radio"]:checked')).toBeNull();
      expect(control('caseType').value).toBeNull();
      expect(control('category').value).toBeNull();
    });

    it('renders the title and intro from LABELS', () => {
      expect(el('h1')?.textContent?.trim()).toBe(LABELS.form.title);
      expect(text()).toContain(LABELS.form.intro);
    });
  });

  describe('validation messages', () => {
    it('shows all required messages at once, emits nothing and focuses the first radio', () => {
      const spy = vi.fn();
      fixture.componentInstance.submitted.subscribe(spy);
      submit();
      expect(text()).toContain(LABELS.errors.caseTypeRequired);
      expect(text()).toContain(LABELS.errors.categoryRequired);
      expect(text()).toContain(LABELS.errors.modelName);
      expect(text()).toContain(LABELS.errors.purchaseDateRequired);
      expect(text()).toContain(LABELS.errors.photoRequired);
      expect(spy).not.toHaveBeenCalled();
      expect(document.activeElement).toBe(el('input[type="radio"]'));
    });

    it.each([
      ['a', false],
      ['ab', true],
      ['a'.repeat(100), true],
      ['a'.repeat(101), false],
      ['   a   ', false],
    ])('model name %j valid=%s (trimmed length 2-100)', (value, valid) => {
      type('modelName', value);
      expect(control('modelName').valid).toBe(valid);
    });

    it.each([
      ['9 trimmed characters', 'x'.repeat(9), false],
      ['10 characters', 'x'.repeat(10), true],
      ['whitespace only', '          ', false],
    ])('COMPLAINT reason with %s valid=%s', (_label, value, valid) => {
      control('caseType').setValue('COMPLAINT');
      type('reason', value);
      expect(control('reason').valid).toBe(valid);
    });

    it('COMPLAINT reason shows the reasonComplaint message when too short', () => {
      control('caseType').setValue('COMPLAINT');
      type('reason', 'krótko');
      control('reason').markAsTouched();
      fixture.detectChanges();
      expect(text()).toContain(LABELS.errors.reasonComplaint);
    });

    it('RETURN reason may be empty', () => {
      control('caseType').setValue('RETURN');
      expect(control('reason').valid).toBe(true);
    });

    it('reason longer than 2000 characters is invalid', () => {
      control('caseType').setValue('RETURN');
      type('reason', 'x'.repeat(2001));
      expect(control('reason').valid).toBe(false);
    });

    it('re-validates reason when the case type changes', () => {
      control('caseType').setValue('COMPLAINT');
      type('reason', 'krótko');
      expect(control('reason').valid).toBe(false);
      control('caseType').setValue('RETURN');
      expect(control('reason').valid).toBe(true);
    });

    it('a future purchase date (tomorrow) shows purchaseDateFuture; today is valid', () => {
      const tomorrow = new Date();
      tomorrow.setDate(tomorrow.getDate() + 1);
      control('purchaseDate').setValue(tomorrow);
      control('purchaseDate').markAsTouched();
      fixture.detectChanges();
      expect(text()).toContain(LABELS.errors.purchaseDateFuture);

      control('purchaseDate').setValue(new Date());
      fixture.detectChanges();
      expect(control('purchaseDate').valid).toBe(true);
    });
  });

  describe('reason label and hints by case type', () => {
    function reasonLabel(): string {
      return (
        el('textarea')!.closest('mat-form-field')!.querySelector('mat-label')!.textContent ?? ''
      );
    }

    it('no type selected: label "Powód", no photo hint', () => {
      expect(reasonLabel()).toContain(LABELS.form.reasonNeutral);
      expect(el('app-photo-picker .hint')).toBeNull();
    });

    it('RETURN: label "Powód (opcjonalnie)" and the RETURN hint', () => {
      control('caseType').setValue('RETURN');
      fixture.detectChanges();
      expect(reasonLabel()).toContain(LABELS.form.reasonOptional);
      expect(el('app-photo-picker .hint')?.textContent?.trim()).toBe(LABELS.photoHint.RETURN);
      expect(el('textarea')!.hasAttribute('required')).toBe(false);
    });

    it('COMPLAINT: label "Powód", required textarea and the COMPLAINT hint', () => {
      control('caseType').setValue('COMPLAINT');
      fixture.detectChanges();
      expect(reasonLabel()).toContain(LABELS.form.reasonNeutral);
      expect(reasonLabel()).not.toContain(LABELS.form.reasonOptional);
      expect(el('textarea')!.hasAttribute('required')).toBe(true);
      expect(el('app-photo-picker .hint')?.textContent?.trim()).toBe(LABELS.photoHint.COMPLAINT);
    });

    it('character counter shows 0 / 2000 and updates on input', () => {
      expect(text()).toContain('0 / 2000');
      type('reason', 'abc');
      expect(text()).toContain('3 / 2000');
    });
  });

  describe('case type radio cards', () => {
    it('shows both case types with their descriptions', () => {
      expect(text()).toContain(CASE_TYPE_LABELS.RETURN);
      expect(text()).toContain(CASE_TYPE_LABELS.COMPLAINT);
    });

    it('clicking a card selects that case type', () => {
      const radios = fixture.nativeElement.querySelectorAll(
        'input[type="radio"]',
      ) as NodeListOf<HTMLInputElement>;
      radios[1].click();
      fixture.detectChanges();
      expect(control('caseType').value).toBe('COMPLAINT');
    });
  });

  describe('photo', () => {
    it('accepts a 10 MB JPEG and rejects 10 MB + 1 byte with photoTooLarge', async () => {
      await pickFile(imageFile('max.jpg', JPEG_HEADER, 10 * MB));
      await vi.waitFor(() => expect(control('photo').value).toBeInstanceOf(File));

      await pickFile(imageFile('big.jpg', JPEG_HEADER, 10 * MB + 1));
      await vi.waitFor(() => expect(text()).toContain(LABELS.errors.photoTooLarge));
      expect(control('photo').value).toBeNull();
      expect(control('photo').valid).toBe(false);
    });

    it('rejects a GIF named .jpg with photoUnsupported', async () => {
      await pickFile(imageFile('fake.jpg', GIF_HEADER, GIF_HEADER.length, 'image/gif'));
      await vi.waitFor(() => expect(text()).toContain(LABELS.errors.photoUnsupported));
      expect(control('photo').value).toBeNull();
    });

    it('accepts a JPEG named .png', async () => {
      const file = imageFile('photo.png', JPEG_HEADER);
      await pickFile(file);
      await vi.waitFor(() => expect(control('photo').value).toBe(file));
    });

    it('a second photo replaces the first and the old object URL is revoked', async () => {
      await pickFile(imageFile('a.jpg', JPEG_HEADER));
      await vi.waitFor(() => expect(el('img')).not.toBeNull());
      const second = imageFile('b.jpg', JPEG_HEADER);
      await pickFile(second);
      await vi.waitFor(() => expect(control('photo').value).toBe(second));
      expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:preview-1');
    });

    it('remove button clears the value and the preview', async () => {
      await pickFile(imageFile('a.jpg', JPEG_HEADER));
      await vi.waitFor(() => expect(el('img')).not.toBeNull());
      const remove = Array.from(fixture.nativeElement.querySelectorAll('button')).find(
        (b) => (b as HTMLElement).textContent?.trim() === LABELS.form.removePhoto,
      ) as HTMLButtonElement;
      remove.click();
      fixture.detectChanges();
      expect(control('photo').value).toBeNull();
      expect(el('img')).toBeNull();
    });

    it('shows the photo hint for the selected case type', () => {
      control('caseType').setValue('RETURN');
      fixture.detectChanges();
      expect(text()).toContain(LABELS.photoHint.RETURN);
    });
  });

  describe('submit', () => {
    it('emits once with trimmed strings and the local YYYY-MM-DD date', () => {
      const photo = imageFile('a.jpg', JPEG_HEADER);
      const spy = vi.fn();
      fixture.componentInstance.submitted.subscribe(spy);
      fillValid(photo);
      submit();
      submit();
      expect(spy).toHaveBeenCalledTimes(1);
      expect(spy).toHaveBeenCalledWith({
        form: {
          caseType: 'RETURN',
          category: 'LAPTOP',
          modelName: 'Lenovo ThinkPad T14',
          purchaseDate: '2024-01-15',
          reason: 'nie działa',
        },
        photo,
      });
    });

    it('unlocks again after a submitting true-then-false cycle', () => {
      const spy = vi.fn();
      fixture.componentInstance.submitted.subscribe(spy);
      fillValid();
      submit();
      fixture.componentRef.setInput('submitting', true);
      fixture.detectChanges();
      fixture.componentRef.setInput('submitting', false);
      fixture.detectChanges();
      submit();
      expect(spy).toHaveBeenCalledTimes(2);
    });

    it('submitting=true disables the submit button and blocks emission', () => {
      const spy = vi.fn();
      fixture.componentInstance.submitted.subscribe(spy);
      fillValid();
      fixture.componentRef.setInput('submitting', true);
      fixture.detectChanges();
      const button = el('button[type="submit"]') as HTMLButtonElement;
      expect(button.disabled).toBe(true);
      expect(button.textContent?.trim()).toBe(LABELS.form.submit);
      submit();
      expect(spy).not.toHaveBeenCalled();
    });
  });

  describe('prefill and notices', () => {
    it('initialDraft fills the values but not the photo', () => {
      const draft: FormDraft = {
        caseType: 'COMPLAINT',
        category: 'SMARTPHONE',
        modelName: 'Pixel 8',
        purchaseDate: '2024-03-02',
        reason: 'pęknięty ekran',
        interrupted: false,
      };
      renderWith({ initialDraft: draft });
      expect(control('caseType').value).toBe('COMPLAINT');
      expect(control('category').value).toBe('SMARTPHONE');
      expect(control('modelName').value).toBe('Pixel 8');
      expect(control('purchaseDate').value).toEqual(new Date(2024, 2, 2));
      expect(control('reason').value).toBe('pęknięty ekran');
      expect(control('photo').value).toBeNull();
    });

    it('initialPhoto prefills the photo and shows its preview', () => {
      const photo = imageFile('back.jpg', JPEG_HEADER);
      renderWith({ initialPhoto: photo });
      expect(control('photo').value).toBe(photo);
      expect(el('img')).not.toBeNull();
    });

    it('interruptedNotice shows the AC-40 text in a status box', () => {
      fixture.componentRef.setInput('interruptedNotice', true);
      fixture.detectChanges();
      expect(el('[role="status"]')?.textContent?.trim()).toBe(LABELS.form.interrupted);
    });

    it('no interrupted notice by default', () => {
      expect(el('[role="status"]')).toBeNull();
    });
  });

  describe('server field errors', () => {
    it('shows the server message under the matching field', () => {
      fixture.componentRef.setInput('serverFieldErrors', [{ field: 'modelName', message: 'X' }]);
      fixture.detectChanges();
      control('modelName').markAsTouched();
      fixture.detectChanges();
      expect(text()).toContain('X');
    });
  });
});

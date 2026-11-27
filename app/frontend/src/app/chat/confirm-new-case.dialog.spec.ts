import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { CaseStore } from '../core/case-store';
import { LABELS } from '../core/labels';
import { ConfirmNewCaseDialogComponent } from './confirm-new-case.dialog';
import { PhotoPreviewDialogComponent } from './photo-preview.dialog';

describe('ConfirmNewCaseDialogComponent', () => {
  let fixture: ComponentFixture<ConfirmNewCaseDialogComponent>;
  let store: { newCase: ReturnType<typeof vi.fn> };
  let dialogRef: { close: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    store = { newCase: vi.fn() };
    dialogRef = { close: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [ConfirmNewCaseDialogComponent],
      providers: [
        { provide: CaseStore, useValue: store },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ConfirmNewCaseDialogComponent);
    fixture.detectChanges();
  });

  function button(label: string): HTMLButtonElement {
    return Array.from(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>).find(
      (item) => item.textContent?.trim() === label,
    )!;
  }

  it('shows the confirmation text and confirm action clears the case', () => {
    expect(fixture.nativeElement.textContent).toContain(LABELS.confirmNewCase.text);
    button(LABELS.confirmNewCase.confirm).click();
    expect(store.newCase).toHaveBeenCalledTimes(1);
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('cancel closes the dialog without clearing the case', () => {
    button(LABELS.confirmNewCase.cancel).click();
    expect(store.newCase).not.toHaveBeenCalled();
    expect(dialogRef.close).toHaveBeenCalledWith(false);
  });
});

describe('PhotoPreviewDialogComponent', () => {
  it('renders the enlarged thumbnail with its alt text and a close action', async () => {
    const dialogRef = { close: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [PhotoPreviewDialogComponent],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: { src: 'data:image/jpeg;base64,THUMB', alt: LABELS.chat.photo } },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(PhotoPreviewDialogComponent);
    fixture.detectChanges();
    const image = fixture.nativeElement.querySelector('img') as HTMLImageElement;
    expect(image.src).toContain('data:image/jpeg;base64,THUMB');
    expect(image.alt).toBe(LABELS.chat.photo);
    const close = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    expect(close.textContent?.trim()).toBe(LABELS.chat.close);
    close.click();
    expect(dialogRef.close).toHaveBeenCalledTimes(1);
  });
});

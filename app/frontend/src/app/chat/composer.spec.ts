import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CaseStore } from '../core/case-store';
import { LABELS } from '../core/labels';
import { ComposerComponent } from './composer';

describe('ComposerComponent', () => {
  let fixture: ComponentFixture<ComposerComponent>;
  let store: { streaming: ReturnType<typeof signal<boolean>>; sendMessage: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    store = { streaming: signal(false), sendMessage: vi.fn(() => Promise.resolve()) };
    await TestBed.configureTestingModule({
      imports: [ComposerComponent],
      providers: [{ provide: CaseStore, useValue: store }],
    }).compileComponents();
    fixture = TestBed.createComponent(ComposerComponent);
    fixture.detectChanges();
  });

  function textarea(): HTMLTextAreaElement {
    return fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
  }

  function sendButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
  }

  function input(value: string): void {
    textarea().value = value;
    textarea().dispatchEvent(new Event('input', { bubbles: true }));
    fixture.detectChanges();
  }

  it('has a visible Polish label and disables send for whitespace-only content', () => {
    expect(fixture.nativeElement.querySelector('label')?.textContent?.trim()).toBe(LABELS.chat.composerLabel);
    expect(sendButton().disabled).toBe(true);
    input('  \n  ');
    expect(sendButton().disabled).toBe(true);
  });

  it('sends on Enter and clears the text after sending', () => {
    input('Jakie są kolejne kroki?');
    const event = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true });
    textarea().dispatchEvent(event);

    expect(event.defaultPrevented).toBe(true);
    expect(store.sendMessage).toHaveBeenCalledWith('Jakie są kolejne kroki?');
    fixture.detectChanges();
    expect(textarea().value).toBe('');
  });

  it('does not send or prevent Shift+Enter so the textarea can insert a newline', () => {
    input('Pierwszy wiersz');
    const event = new KeyboardEvent('keydown', {
      key: 'Enter',
      shiftKey: true,
      bubbles: true,
      cancelable: true,
    });
    textarea().dispatchEvent(event);

    expect(event.defaultPrevented).toBe(false);
    expect(store.sendMessage).not.toHaveBeenCalled();
  });

  it('disables send while a reply streams', () => {
    input('Pytanie');
    store.streaming.set(true);
    fixture.detectChanges();
    expect(sendButton().disabled).toBe(true);
  });

  it('offers no file upload, drop zone or attach control (AC-37)', () => {
    const root: HTMLElement = fixture.nativeElement;
    expect(root.querySelector('input[type="file"]')).toBeNull();
    expect(root.querySelector('[class*="drop" i], [data-drop], [ondrop]')).toBeNull();
    const attachButton = Array.from(root.querySelectorAll('button')).find((button) =>
      /załącz|dodaj zdjęcie|zdjęcie|attach|upload|photo|image/i.test(button.textContent ?? button.getAttribute('aria-label') ?? ''),
    );
    expect(attachButton).toBeUndefined();
  });

  it('does not allow 2001 characters and displays the counter from 1800 characters', () => {
    input('x'.repeat(1800));
    expect(fixture.nativeElement.textContent).toContain('1800');
    expect(fixture.nativeElement.textContent).toContain('2000');
    expect(sendButton().disabled).toBe(false);

    input('x'.repeat(2001));
    expect(sendButton().disabled).toBe(true);
    expect(store.sendMessage).not.toHaveBeenCalled();
  });
});

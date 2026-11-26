import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { LABELS } from './core/labels';

describe('App shell', () => {
  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [App],
    }).compileComponents();
  });

  async function render(): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('shows the ElektroDemo text wordmark and the product name in the header (ADR-005 D8)', async () => {
    const el = await render();
    const header = el.querySelector('header');
    expect(header).not.toBeNull();
    expect(header!.querySelector('.wordmark')?.textContent?.trim()).toBe('ElektroDemo');
    expect(header!.textContent).toContain('Copilot serwisowy');
    expect(header!.querySelector('img')).toBeNull();
  });

  it('shows the AC-46 demo note as a note on every screen', async () => {
    const el = await render();
    const note = el.querySelector('[role="note"]');
    expect(note?.textContent?.trim()).toBe(
      'Wersja demonstracyjna — przykładowy regulamin fikcyjnego sklepu. Decyzja jest wstępna.',
    );
    expect(note?.textContent?.trim()).toBe(LABELS.demoNote);
  });
});

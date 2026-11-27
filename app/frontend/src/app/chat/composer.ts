import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { TextFieldModule } from '@angular/cdk/text-field';
import { CaseStore } from '../core/case-store';
import { LABELS } from '../core/labels';

const MESSAGE_MAX_LENGTH = 2000;

@Component({
  selector: 'app-composer',
  imports: [MatButtonModule, TextFieldModule],
  templateUrl: './composer.html',
  styleUrl: './composer.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ComposerComponent {
  protected readonly store = inject(CaseStore);
  protected readonly labels = LABELS.chat;
  protected readonly draft = signal('');
  protected readonly maxLength = MESSAGE_MAX_LENGTH;

  protected get canSend(): boolean {
    const value = this.draft();
    return !!value.trim() && value.length <= MESSAGE_MAX_LENGTH && !this.store.streaming();
  }

  protected onInput(event: Event): void {
    this.draft.set((event.target as HTMLTextAreaElement).value);
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  protected send(): void {
    if (!this.canSend) {
      return;
    }
    const value = this.draft();
    this.draft.set('');
    void this.store.sendMessage(value);
  }
}

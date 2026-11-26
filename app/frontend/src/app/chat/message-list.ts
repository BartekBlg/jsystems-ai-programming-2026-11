import { ChangeDetectionStrategy, Component, ElementRef, effect, inject, output, signal, viewChild } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MarkdownModule, provideMarkdown } from 'ngx-markdown';
import { CaseStore } from '../core/case-store';
import { LABELS } from '../core/labels';
import { ChatMessage } from '../core/models';

@Component({
  selector: 'app-message-list',
  imports: [MatButtonModule, MarkdownModule],
  providers: [provideMarkdown()],
  templateUrl: './message-list.html',
  styleUrl: './message-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MessageListComponent {
  protected readonly store = inject(CaseStore);
  protected readonly labels = LABELS.chat;
  protected readonly newCaseLabel = LABELS.newCase;
  readonly newCaseRequested = output<void>();
  protected readonly retryingIds = signal(new Set<string>());
  private readonly viewport = viewChild<ElementRef<HTMLElement>>('viewport');
  private shouldFollow = true;

  constructor() {
    effect(() => {
      this.store.messages().map((message) => message.text).join('\u0000');
      this.store.streaming();
      if (this.shouldFollow) {
        queueMicrotask(() => this.scrollToBottom());
      }
    });
  }

  protected onScroll(event: Event): void {
    const viewport = event.currentTarget as HTMLElement;
    this.shouldFollow = viewport.scrollHeight - viewport.scrollTop - viewport.clientHeight < 80;
  }

  protected resend(message: ChatMessage): void {
    if (this.retryingIds().has(message.id)) {
      return;
    }
    this.retryingIds.update((ids) => new Set(ids).add(message.id));
    void this.store.resend(message.id).finally(() => {
      this.retryingIds.update((ids) => {
        const next = new Set(ids);
        next.delete(message.id);
        return next;
      });
    });
  }

  protected isRetrying(message: ChatMessage): boolean {
    return this.retryingIds().has(message.id);
  }

  private scrollToBottom(): void {
    const element = this.viewport()?.nativeElement;
    if (element && this.shouldFollow) {
      element.scrollTop = element.scrollHeight;
    }
  }
}

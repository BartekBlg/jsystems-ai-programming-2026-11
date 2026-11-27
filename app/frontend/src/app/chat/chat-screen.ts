import { BreakpointObserver } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CaseStore } from '../core/case-store';
import { LABELS } from '../core/labels';
import { CaseSummaryComponent } from './case-summary';
import { ComposerComponent } from './composer';
import { ConfirmNewCaseDialogComponent } from './confirm-new-case.dialog';
import { MessageListComponent } from './message-list';

@Component({
  selector: 'app-chat-screen',
  imports: [
    MatButtonModule,
    MatDialogModule,
    MatExpansionModule,
    CaseSummaryComponent,
    MessageListComponent,
    ComposerComponent,
  ],
  templateUrl: './chat-screen.html',
  styleUrl: './chat-screen.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatScreenComponent {
  protected readonly store = inject(CaseStore);
  protected readonly labels = LABELS.chat;
  protected readonly wideLayout = signal(false);
  protected readonly replyAnnouncement = signal('');
  private readonly dialog = inject(MatDialog);
  private wasStreaming = false;

  constructor() {
    inject(BreakpointObserver)
      .observe('(min-width: 960px)')
      .pipe(takeUntilDestroyed())
      .subscribe((state) => this.wideLayout.set(state.matches));

    effect(() => {
      const streaming = this.store.streaming();
      const messages = this.store.messages();
      if (this.wasStreaming && !streaming && messages.at(-1)?.role === 'assistant') {
        this.replyAnnouncement.set(LABELS.chat.newReply);
      }
      this.wasStreaming = streaming;
    });
  }

  protected openNewCaseDialog(): void {
    this.dialog.open(ConfirmNewCaseDialogComponent, { maxWidth: 'calc(100vw - 32px)' });
  }
}

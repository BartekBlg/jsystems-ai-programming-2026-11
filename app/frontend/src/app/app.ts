import { Component } from '@angular/core';
import { LABELS } from './core/labels';

@Component({
  selector: 'app-root',
  imports: [],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly labels = LABELS;
}

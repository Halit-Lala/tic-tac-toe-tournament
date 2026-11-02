import { Component, signal } from '@angular/core';
import { MainLayout } from './core/components/layouts/main-layout/main-layout';

@Component({
  imports: [MainLayout],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('tic-tac-toe-tournament-frontend');
}

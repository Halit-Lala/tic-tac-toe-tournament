import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'setup' },
  {
    path: 'setup',
    title: 'Setup',
    loadComponent: () => import('./features/setup/components/setup-tournament/setup-tournament').then((m) => m.SetupTournament),
  },
  { path: '**', redirectTo: 'setup' },
];

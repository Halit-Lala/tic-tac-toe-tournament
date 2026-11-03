import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'setup' },
  {
    path: 'setup',
    title: 'Setup',
    loadComponent: () => 
      import('./features/setup/components/setup-tournament/setup-tournament').then(
        (m) => m.SetupTournament
      ),
  },
  {
    path: 'tournaments',
    loadChildren: () =>
      import('./features/tournament/tournament.routes').then(
        (m) => m.TOURNAMENT_ROUTES
      ),
  },
  { path: '**', redirectTo: 'setup' },
];

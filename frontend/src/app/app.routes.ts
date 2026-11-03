import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'setup' },
  {
    path: 'setup',
    title: 'Setup',
    loadComponent: () => 
      import('./features/setup/components/setup-tournament/setup-tournament')
        .then((m) => m.SetupTournament)
  },
  {
    path: 'tournaments',
    loadChildren: () =>
      import('./features/tournament/tournament.routes')
        .then((m) => m.TOURNAMENT_ROUTES)
  },
  {
    path: 'history',
    title: 'History',
    loadComponent: () => 
      import('./features/history/components/tournament-history/tournament-history')
        .then((m) => m.TournamentHistory)
  },
  {
    path: 'leaderboard',
    title: 'Leaderboard',
    loadComponent: () =>
      import('./features/leaderboard/components/leaderboard/leaderboard')
        .then((m) => m.Leaderboard),
  },
  { path: '**', redirectTo: 'setup' },
];

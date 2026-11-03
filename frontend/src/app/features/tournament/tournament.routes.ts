import { Routes } from '@angular/router';
import { TournamentGame } from './components/tournament-game/tournament-game';
import { TournamentResults } from './components/tournament-results/tournament-results';

export const TOURNAMENT_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: '/setup' },
  {
    path: ':id',
    title: 'Tournament',
    component: TournamentGame,
  },
  {
    path: ':id/results',
    title: 'Results',
    component: TournamentResults,
  },
];
import { Routes } from '@angular/router';
import { TournamentGame } from './components/tournament-game/tournament-game';
import { TournamentResults } from './components/tournament-results/tournament-results';
import { tournamentExitGuard } from './guards/tournament-exit.guard';

export const TOURNAMENT_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: '/setup' },
  {
    path: ':id/results',
    title: 'Results',
    component: TournamentResults,
  },
  {
    path: ':id',
    title: 'Tournament',
    canDeactivate: [tournamentExitGuard],
    component: TournamentGame,
  }
];
import { CanDeactivateFn } from '@angular/router';

export interface TournamentExitAware {
  canLeaveTournament(): boolean;
}

export const tournamentExitGuard: CanDeactivateFn<TournamentExitAware> = (
  component,
) => component.canLeaveTournament();
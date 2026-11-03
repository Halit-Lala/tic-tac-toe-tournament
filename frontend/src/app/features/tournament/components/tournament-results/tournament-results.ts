import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { distinctUntilChanged, filter, map } from 'rxjs';
import { Scoreboard } from '../scoreboard/scoreboard';
import { TournamentStore } from '../../stores/tournament.store';
import { outcomeLabel, playerName } from '../../../../shared/utils/tournament-labels';

@Component({
  selector: 'app-tournament-results',
  imports: [RouterLink, MatButtonModule, Scoreboard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './tournament-results.html',
  styleUrl: './tournament-results.scss',
})
export class TournamentResults {
  protected readonly store = inject(TournamentStore);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly playerName = playerName;
  protected readonly outcomeLabel = outcomeLabel;

  constructor() {
    this.route.paramMap.pipe(
      filter((params) => {
        const id = Number(params.get('id'));
        return Number.isInteger(id) && id > 0;
      }),
      distinctUntilChanged(),
      takeUntilDestroyed(),
    ).subscribe((params) => {
      const id = Number(params.get('id'));
      if (this.store.tournament()?.id !== id) {
        this.store.reset();
        this.store.load(id);
      }
    });
  }

  protected playAgain(): void {
    const tournament = this.store.tournament();
    this.store.reset();

    this.router.navigate(['/setup'], {
      queryParams: tournament
        ? {
            playerX: tournament.playerX.name,
            playerO: tournament.playerO.name,
            format: tournament.format,
          }
        : undefined,
    });
  }
}
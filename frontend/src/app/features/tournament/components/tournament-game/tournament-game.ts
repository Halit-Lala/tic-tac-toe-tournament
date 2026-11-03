import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { distinctUntilChanged, filter, map } from 'rxjs';
import { CellPosition, GameBoard } from '../game-board/game-board';
import { TournamentStore } from '../../stores/tournament.store';
import { Scoreboard } from '../scoreboard/scoreboard';
import { outcomeLabel, playerName } from '../../../../shared/utils/tournament-labels';

@Component({
  selector: 'app-tournament-game',
  imports: [RouterLink, MatButtonModule, GameBoard, Scoreboard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './tournament-game.html',
  styleUrl: './tournament-game.scss',
})
export class TournamentGame {
  protected readonly store = inject(TournamentStore);
  private readonly route = inject(ActivatedRoute);

  protected readonly displayedGame = computed(
    () => this.store.currentGame() ?? this.store.lastGame(),
  );

  protected readonly turn = computed(() => {
    const tournament = this.store.tournament();
    const symbol = this.store.currentGame()?.nextSymbol;
    return tournament && symbol ? { symbol, name: playerName(tournament, symbol) } : null;
  });

  protected readonly lastGameResult = computed(() => {
    const tournament = this.store.tournament();
    const game = this.store.lastGame();
    return tournament && game ? outcomeLabel(tournament, game) : '';
  });

  constructor() {
    this.route.paramMap.pipe(
      filter((params) => {
        const id = Number(params.get('id'));
        return Number.isInteger(id) && id > 0;
      }),
      distinctUntilChanged(),
      takeUntilDestroyed(),
    ).subscribe((params) => {
      this.store.reset();
      this.store.load(Number(params.get('id')));
    });
  }

  protected onCellClick({ row, col }: CellPosition): void {
    this.store.playMove(row, col);
  }

  protected onNextGame(): void {
    this.store.startNextGame();
  }

  protected canLeaveTournament(): boolean {
    if (this.store.isCompleted()) {
      return true;
    }

    return window.confirm(
      'The tournament is not finished! Do you confirm to exit?',
    );
  }
}
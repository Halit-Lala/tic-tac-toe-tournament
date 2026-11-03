import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { PlayerSymbol } from '../../../player/model/player.model';

export interface CellPosition {
  row: number;
  col: number;
}

@Component({
  selector: 'app-game-board',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './game-board.html',
  styleUrl: './game-board.scss',
})
export class GameBoard {
  readonly board = input.required<(PlayerSymbol | null)[][]>();
  readonly disabled = input(false);
  readonly cellClick = output<CellPosition>();
}
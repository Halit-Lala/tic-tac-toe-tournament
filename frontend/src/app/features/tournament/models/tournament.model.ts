import { Player, PlayerSymbol } from "../../player/model/player.model";

export type TournamentFormat = 'BEST_OF_1' | 'BEST_OF_3' | 'BEST_OF_5';
export type TournamentStatus = 'CREATED' | 'IN_PROGRESS' | 'COMPLETED';
export type GameStatus = 'IN_PROGRESS' | 'COMPLETED';
export type GameOutcome = 'X_WINS' | 'O_WINS' | 'DRAW';

export interface Score {
  playerXWins: number;
  playerOWins: number;
  draws: number;
}

export interface Game {
  id: number;
  gameNumber: number;
  startingSymbol: PlayerSymbol;
  nextSymbol: PlayerSymbol | null;
  board: (PlayerSymbol | null)[][];
  status: GameStatus;
  outcome: GameOutcome | null;
}

export interface Tournament {
  id: number;
  playerX: Player;
  playerO: Player;
  format: TournamentFormat;
  status: TournamentStatus;
  winner: Player | null;
  score: Score;
  games: Game[];
}

export interface PlayMoveRequest {
  row: number;
  col: number;
  symbol: PlayerSymbol;
}

export const TOTAL_GAMES: Record<TournamentFormat, number> = {
  BEST_OF_1: 1,
  BEST_OF_3: 3,
  BEST_OF_5: 5,
};
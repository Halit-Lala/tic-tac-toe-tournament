import { PlayerSymbol } from "../../features/player/model/player.model";
import { Game, Tournament } from "../../features/tournament/models/tournament.model";

export function playerName(tournament: Tournament, symbol: PlayerSymbol): string {
  return symbol === 'X' ? tournament.playerX.name : tournament.playerO.name;
}

export function outcomeLabel(tournament: Tournament, game: Game): string {
  switch (game.outcome) {
    case 'X_WINS':
      return `${tournament.playerX.name} won`;
    case 'O_WINS':
      return `${tournament.playerO.name} won`;
    case 'DRAW':
      return 'Draw';
    default:
      return 'In progress';
  }
}
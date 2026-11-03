import { Player } from "../../player/model/player.model";
import { Score, TournamentFormat, TournamentStatus } from "../../tournament/models/tournament.model";

export interface TournamentHistoryEntry {
  id: number;
  playerX: Player;
  playerO: Player;
  format: TournamentFormat;
  status: TournamentStatus;
  winner: Player;
  score: Score;
  createdAt: string;
}
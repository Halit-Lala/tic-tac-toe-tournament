import { Player } from "../../player/model/player.model";

export interface LeaderboardEntry {
  rank: number;
  player: Player;
  tournamentWins: number;
  tournamentsPlayed: number;
  winRate: number;
}

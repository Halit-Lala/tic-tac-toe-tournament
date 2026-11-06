import { PlayerSymbol } from "../../player/model/player.model";
import { TournamentFormat, TournamentMode } from "../../tournament/models/tournament.model";

export interface NewTournamentRequest {
    playerXName: string;
    playerOName: string;
    format: TournamentFormat;
    mode: TournamentMode;
    creatorSeat?: PlayerSymbol;
}
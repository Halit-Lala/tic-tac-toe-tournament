import { TournamentFormat } from "../../tournament/models/tournament.model";

export interface NewTournamentRequest {
    playerXName: string;
    playerOName: string;
    format: TournamentFormat;
}
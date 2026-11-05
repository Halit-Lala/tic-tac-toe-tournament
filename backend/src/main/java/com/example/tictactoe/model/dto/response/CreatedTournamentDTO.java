package com.example.tictactoe.model.dto.response;

import com.example.tictactoe.model.enums.PlayerSymbol;

public record CreatedTournamentDTO(
    TournamentDTO tournament,
    PlayerSymbol seat,
    String seatToken,
    String inviteToken
) {}
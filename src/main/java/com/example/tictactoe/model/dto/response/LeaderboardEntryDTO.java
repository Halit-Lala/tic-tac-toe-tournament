package com.example.tictactoe.model.dto.response;

public record LeaderboardEntryDTO(
    int rank,
    PlayerDTO player,
    int tournamentWins
) {}
package com.example.tictactoe.model.dto.response;

import java.time.Instant;

import com.example.tictactoe.model.enums.TournamentFormat;
import com.example.tictactoe.model.enums.TournamentStatus;

public record TournamentHistoryEntryDTO(
    Long id,
    PlayerDTO playerX,
    PlayerDTO playerO,
    TournamentFormat format,
    TournamentStatus status,
    PlayerDTO winner,
    ScoreDTO score,
    Instant createdAt
) {}


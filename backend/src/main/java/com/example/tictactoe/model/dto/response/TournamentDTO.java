package com.example.tictactoe.model.dto.response;

import java.time.Instant;
import java.util.List;

import com.example.tictactoe.model.enums.TournamentFormat;
import com.example.tictactoe.model.enums.TournamentStatus;

public record TournamentDTO(
    Long id,
    PlayerDTO playerX,
    PlayerDTO playerO,
    TournamentFormat format,
    TournamentStatus status,
    PlayerDTO winner,
    ScoreDTO score,
    List<GameDTO> games,
    Instant createdAt
) {}
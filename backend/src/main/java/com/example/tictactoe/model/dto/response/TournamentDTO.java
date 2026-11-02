package com.example.tictactoe.model.dto.response;

import com.example.tictactoe.model.enums.TournamentFormat;
import com.example.tictactoe.model.enums.TournamentStatus;

import java.util.List;

public record TournamentDTO(
    Long id,
    PlayerDTO playerX,
    PlayerDTO playerO,
    TournamentFormat format,
    TournamentStatus status,
    PlayerDTO winner,
    ScoreDTO score,
    List<GameDTO> games
) {}
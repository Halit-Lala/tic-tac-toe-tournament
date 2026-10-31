package com.example.tictactoe.model.dto.request;

import com.example.tictactoe.model.enums.TournamentFormat;

public record NewTournamentDTO (
    String playerXName,
    String playerOName,
    TournamentFormat format
) {}
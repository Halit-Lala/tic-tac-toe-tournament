package com.example.tictactoe.model.dto.response;

public record ScoreDTO(
    int playerXWins,
    int playerOWins,
    int draws
) {}

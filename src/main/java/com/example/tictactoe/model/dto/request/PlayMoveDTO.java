package com.example.tictactoe.model.dto.request;

import com.example.tictactoe.model.enums.PlayerSymbol;

public record PlayMoveDTO(
    int row,
    int col,
    PlayerSymbol symbol
) {}

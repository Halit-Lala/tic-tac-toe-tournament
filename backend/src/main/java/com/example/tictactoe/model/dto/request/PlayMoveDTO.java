package com.example.tictactoe.model.dto.request;

import com.example.tictactoe.model.enums.PlayerSymbol;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PlayMoveDTO(
    @NotNull @Min(0) @Max(2) Integer row,
    @NotNull @Min(0) @Max(2) Integer col,
    PlayerSymbol symbol
) {}

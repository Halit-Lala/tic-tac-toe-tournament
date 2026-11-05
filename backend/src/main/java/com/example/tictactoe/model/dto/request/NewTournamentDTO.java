package com.example.tictactoe.model.dto.request;

import com.example.tictactoe.model.enums.PlayerSymbol;
import com.example.tictactoe.model.enums.TournamentFormat;
import com.example.tictactoe.model.enums.TournamentMode;
import com.example.tictactoe.validator.DistinctPlayerNames;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@DistinctPlayerNames
public record NewTournamentDTO(
    @NotBlank @Size(max = 50) String playerXName,
    @NotBlank @Size(max = 50) String playerOName,
    @NotNull TournamentFormat format,
    @NotNull TournamentMode mode,
    PlayerSymbol creatorSeat
) {}
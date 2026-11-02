package com.example.tictactoe.model.dto.response;

import com.example.tictactoe.model.enums.GameOutcome;
import com.example.tictactoe.model.enums.GameStatus;
import com.example.tictactoe.model.enums.PlayerSymbol;

import java.util.List;

public record GameDTO(
    Long id,
    int gameNumber,
    PlayerSymbol startingSymbol,
    PlayerSymbol nextSymbol,
    List<List<PlayerSymbol>> board,
    GameStatus status,
    GameOutcome outcome
) {}
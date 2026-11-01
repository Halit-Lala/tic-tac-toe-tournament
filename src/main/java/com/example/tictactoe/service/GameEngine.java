package com.example.tictactoe.service;

import java.util.Arrays;
import java.util.Objects;

import com.example.tictactoe.exception.IllegalMoveException;
import com.example.tictactoe.model.domain.GameState;
import com.example.tictactoe.model.enums.GameOutcome;
import com.example.tictactoe.model.enums.GameStatus;
import com.example.tictactoe.model.enums.PlayerSymbol;
import com.example.tictactoe.util.Constants;

public final class GameEngine {

    private GameEngine() {}

    public static void playMove(GameState state, int cell, PlayerSymbol symbol) {
        validateMove(state, cell, symbol);

        state.setCell(cell, symbol);

        if (hasWon(state, symbol)) {
            GameOutcome outcome = symbol == PlayerSymbol.X ? GameOutcome.X_WINS : GameOutcome.O_WINS;
            state.setResult(GameStatus.COMPLETED, outcome);
        } else if (isFull(state)) {
            state.setResult(GameStatus.COMPLETED, GameOutcome.DRAW);
        }
    }

    public static PlayerSymbol nextSymbol(GameState state) {
        if (state.getStatus() == GameStatus.COMPLETED) {
            return null;
        }
        long occupiedCells = countOccupied(state);
        return occupiedCells % 2 == 0
                ? state.getStartingSymbol()
                : state.getStartingSymbol().opposite();
    }

    private static void validateMove(GameState state, int cell, PlayerSymbol symbol) {
        Objects.requireNonNull(state, "Game state cannot be null");
        Objects.requireNonNull(symbol, "Symbol cannot be null");

        if (state.getStatus() == GameStatus.COMPLETED)
            throw new IllegalMoveException("Game is already completed");
        if (cell < 0 || cell >= Constants.BOARD_CELLS_LENGTH)
            throw new IllegalMoveException("Move is outside the board");
        if (state.getCell(cell) != null)
            throw new IllegalMoveException("Cell is already occupied");
        if (symbol != nextSymbol(state))
            throw new IllegalMoveException("It is not " + symbol + "'s turn");
    }

    private static boolean hasWon(GameState state, PlayerSymbol symbol) {
        for (int[] line : Constants.WIN_LINES) {
            if (state.getCell(line[0]) == symbol
                    && state.getCell(line[1]) == symbol
                    && state.getCell(line[2]) == symbol) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFull(GameState state) {
        return countOccupied(state) == Constants.BOARD_CELLS_LENGTH;
    }

    private static long countOccupied(GameState state) {
        return Arrays.stream(state.getBoard()).filter(Objects::nonNull).count();
    }
}
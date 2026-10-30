package com.example.tictactoe.service;

import org.springframework.stereotype.Service;

import com.example.tictactoe.model.domain.GameState;
import com.example.tictactoe.model.enums.GameOutcome;
import com.example.tictactoe.model.enums.GameStatus;
import com.example.tictactoe.model.enums.PlayerSymbol;
import com.example.tictactoe.util.Constants;

@Service
public class GameEngine {

    public GameState createGame(PlayerSymbol startingSymbol) {
        return new GameState(startingSymbol);
    }

    public void playMove(GameState game, int row, int col) {
        validateMove(game, row, col);

        PlayerSymbol currentSymbol = game.getNextSymbol();

        game.placeSymbol(row, col, currentSymbol);

        if (hasWon(game, currentSymbol)) {
            GameOutcome outcome = currentSymbol == PlayerSymbol.X ? GameOutcome.X_WINS : GameOutcome.O_WINS;

            game.complete(outcome);
            return;
        }

        if (isBoardFull(game)) {
            game.complete(GameOutcome.DRAW);
            return;
        }

        game.switchTurn();
    }

    private void validateMove(GameState game, int row, int col) {
        if (game == null) {
            throw new IllegalArgumentException("Game cannot be null");
        }

        if (game.getStatus() == GameStatus.COMPLETED) {
            throw new IllegalStateException("Game is already completed");
        }

        if (row < 0 || row >= Constants.BOARD_SIZE || col < 0 || col >= Constants.BOARD_SIZE) {
            throw new IllegalArgumentException("Move is outside the board");
        }

        if (game.getCell(row, col) != null) {
            throw new IllegalArgumentException("Cell is already occupied");
        }
    }

    private boolean hasWon(GameState game, PlayerSymbol symbol) {
        for (int row = 0; row < Constants.BOARD_SIZE; row++) {
            if (
                game.getCell(row, 0) == symbol
                && game.getCell(row, 1) == symbol
                && game.getCell(row, 2) == symbol
            ) {
                return true;
            }
        }

        for (int col = 0; col < Constants.BOARD_SIZE; col++) {
            if (
                game.getCell(0, col) == symbol 
                && game.getCell(1, col) == symbol
                && game.getCell(2, col) == symbol
            ) {
                return true;
            }
        }

        boolean mainDiagonal = game.getCell(0, 0) == symbol
                                && game.getCell(1, 1) == symbol
                                && game.getCell(2, 2) == symbol;

        boolean antiDiagonal = game.getCell(0, 2) == symbol
                                && game.getCell(1, 1) == symbol
                                && game.getCell(2, 0) == symbol;

        return mainDiagonal || antiDiagonal;
    }

    private boolean isBoardFull(GameState game) {
        for (int row = 0; row < Constants.BOARD_SIZE; row++) {
            for (int col = 0; col < Constants.BOARD_SIZE; col++) {
                if (game.getCell(row, col) == null) {
                    return false;
                }
            }
        }

        return true;
    }
}
package com.example.tictactoe.model.domain;

import com.example.tictactoe.model.enums.GameOutcome;
import com.example.tictactoe.model.enums.GameStatus;
import com.example.tictactoe.model.enums.PlayerSymbol;
import com.example.tictactoe.util.Constants;

public class GameState {

    private final PlayerSymbol startingSymbol;
    private final PlayerSymbol[] board;
    private GameStatus status;
    private GameOutcome outcome;

    public GameState(
        PlayerSymbol startingSymbol,
        PlayerSymbol[] board,
        GameStatus status,
        GameOutcome outcome
    ) {
        if (startingSymbol == null) {
            throw new IllegalArgumentException("Starting symbol cannot be null");
        }
        if (board == null || board.length != Constants.BOARD_CELLS_LENGTH) {
            throw new IllegalArgumentException("Board must have exactly 9 cells");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        this.startingSymbol = startingSymbol;
        this.board = board.clone();
        this.status = status;
        this.outcome = outcome;
    }

    public static GameState initial(PlayerSymbol startingSymbol) {
        return new GameState(
            startingSymbol,
            new PlayerSymbol[Constants.BOARD_CELLS_LENGTH],
            GameStatus.IN_PROGRESS,
            null
        );
    }

    public PlayerSymbol getStartingSymbol() {
        return startingSymbol;
    }

    public PlayerSymbol getCell(int index) {
        return board[index];
    }

    public PlayerSymbol[] getBoard() {
        return board.clone();
    }

    public GameStatus getStatus() {
        return status;
    }

    public GameOutcome getOutcome() {
        return outcome;
    }

    public void setCell(int index, PlayerSymbol symbol) {
        board[index] = symbol;
    }

    public void setResult(GameStatus status, GameOutcome outcome) {
        this.status = status;
        this.outcome = outcome;
    }
}
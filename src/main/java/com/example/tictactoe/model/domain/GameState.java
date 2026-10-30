package com.example.tictactoe.model.domain;

import com.example.tictactoe.model.enums.GameOutcome;
import com.example.tictactoe.model.enums.GameStatus;
import com.example.tictactoe.model.enums.PlayerSymbol;
import com.example.tictactoe.util.Constants;

public class GameState {

    private final PlayerSymbol[][] board;
    private PlayerSymbol nextSymbol;
    private GameStatus status;
    private GameOutcome outcome;

    public GameState(PlayerSymbol startingSymbol) {
        if (startingSymbol == null) {
            throw new IllegalArgumentException("Starting symbol cannot be null");
        }

        this.board = new PlayerSymbol[Constants.BOARD_SIZE][Constants.BOARD_SIZE];
        this.nextSymbol = startingSymbol;
        this.status = GameStatus.IN_PROGRESS;
    }

    public PlayerSymbol[][] getBoard() {
        PlayerSymbol[][] copy = new PlayerSymbol[Constants.BOARD_SIZE][Constants.BOARD_SIZE];

        for (int row = 0; row < Constants.BOARD_SIZE; row++) {
            copy[row] = board[row].clone();
        }

        return copy;
    }

    public PlayerSymbol getCell(int row, int col) {
        return board[row][col];
    }

    public PlayerSymbol getNextSymbol() {
        return nextSymbol;
    }

    public GameStatus getStatus() {
        return status;
    }

    public GameOutcome getOutcome() {
        return outcome;
    }

    public void placeSymbol(int row, int col, PlayerSymbol symbol) {
        board[row][col] = symbol;
    }

    public void switchTurn() {
        nextSymbol = nextSymbol == PlayerSymbol.X ? PlayerSymbol.O : PlayerSymbol.X;
    }

    public void complete(GameOutcome outcome) {
        this.outcome = outcome;
        this.status = GameStatus.COMPLETED;
    }
}

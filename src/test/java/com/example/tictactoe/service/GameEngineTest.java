package com.example.tictactoe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.tictactoe.model.domain.GameState;
import com.example.tictactoe.model.enums.GameOutcome;
import com.example.tictactoe.model.enums.GameStatus;
import com.example.tictactoe.model.enums.PlayerSymbol;

class GameEngineTest {

    private GameEngine gameEngine;
    private GameState game;

    @BeforeEach
    void setUp() {
        gameEngine = new GameEngine();
        game = gameEngine.createGame(PlayerSymbol.X);
    }

    @Test
    void shouldAlternateTurnsAfterEachMove() {
        gameEngine.playMove(game, 0, 0);

        assertThat(game.getNextSymbol()).isEqualTo(PlayerSymbol.O);

        gameEngine.playMove(game, 1, 0);

        assertThat(game.getNextSymbol()).isEqualTo(PlayerSymbol.X);
    }

    @Test
    void shouldDetectHorizontalWin() {
        gameEngine.playMove(game, 0, 0); // X
        gameEngine.playMove(game, 1, 0); // O
        gameEngine.playMove(game, 0, 1); // X
        gameEngine.playMove(game, 1, 1); // O
        gameEngine.playMove(game, 0, 2); // X wins

        assertThat(game.getStatus()).isEqualTo(GameStatus.COMPLETED);
        assertThat(game.getOutcome()).isEqualTo(GameOutcome.X_WINS);
    }

    @Test
    void shouldDetectVerticalWin() {
        gameEngine.playMove(game, 0, 0); // X
        gameEngine.playMove(game, 0, 1); // O
        gameEngine.playMove(game, 1, 0); // X
        gameEngine.playMove(game, 1, 1); // O
        gameEngine.playMove(game, 2, 0); // X wins

        assertThat(game.getOutcome()).isEqualTo(GameOutcome.X_WINS);
    }

    @Test
    void shouldDetectMainDiagonalWin() {
        gameEngine.playMove(game, 0, 0); // X
        gameEngine.playMove(game, 0, 1); // O
        gameEngine.playMove(game, 1, 1); // X
        gameEngine.playMove(game, 0, 2); // O
        gameEngine.playMove(game, 2, 2); // X wins

        assertThat(game.getOutcome()).isEqualTo(GameOutcome.X_WINS);
    }

    @Test
    void shouldDetectAntiDiagonalWin() {
        gameEngine.playMove(game, 0, 2); // X
        gameEngine.playMove(game, 0, 0); // O
        gameEngine.playMove(game, 1, 1); // X
        gameEngine.playMove(game, 1, 0); // O
        gameEngine.playMove(game, 2, 0); // X wins

        assertThat(game.getStatus()).isEqualTo(GameStatus.COMPLETED);
        assertThat(game.getOutcome()).isEqualTo(GameOutcome.X_WINS);
}

    @Test
    void shouldDetectDraw() {
        gameEngine.playMove(game, 0, 0); // X
        gameEngine.playMove(game, 0, 1); // O
        gameEngine.playMove(game, 0, 2); // X
        gameEngine.playMove(game, 1, 1); // O
        gameEngine.playMove(game, 1, 0); // X
        gameEngine.playMove(game, 1, 2); // O
        gameEngine.playMove(game, 2, 1); // X
        gameEngine.playMove(game, 2, 0); // O
        gameEngine.playMove(game, 2, 2); // X

        assertThat(game.getStatus()).isEqualTo(GameStatus.COMPLETED);
        assertThat(game.getOutcome()).isEqualTo(GameOutcome.DRAW);
    }

    @Test
    void shouldRejectMoveOutsideBoard() {
        assertThatThrownBy(() -> gameEngine.playMove(game, 3, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectMoveOnOccupiedCell() {
        gameEngine.playMove(game, 0, 0);

        assertThatThrownBy(() -> gameEngine.playMove(game, 0, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectMovesAfterGameIsCompleted() {
        gameEngine.playMove(game, 0, 0); // X
        gameEngine.playMove(game, 1, 0); // O
        gameEngine.playMove(game, 0, 1); // X
        gameEngine.playMove(game, 1, 1); // O
        gameEngine.playMove(game, 0, 2); // X wins

        assertThatThrownBy(() -> gameEngine.playMove(game, 2, 2)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldAllowPlayerOToStartTheGame() {
        GameState newGame = gameEngine.createGame(PlayerSymbol.O);

        gameEngine.playMove(newGame, 0, 0);

        assertThat(newGame.getCell(0, 0)).isEqualTo(PlayerSymbol.O);
        assertThat(newGame.getNextSymbol()).isEqualTo(PlayerSymbol.X);
    }
}
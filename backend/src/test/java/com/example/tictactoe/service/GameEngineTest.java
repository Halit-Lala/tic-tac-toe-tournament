package com.example.tictactoe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.tictactoe.exception.IllegalMoveException;
import com.example.tictactoe.model.domain.GameState;
import com.example.tictactoe.model.enums.GameOutcome;
import com.example.tictactoe.model.enums.GameStatus;
import com.example.tictactoe.model.enums.PlayerSymbol;

class GameEngineTest {

    private GameState game;

    @BeforeEach
    void setUp() {
        game = GameState.initial(PlayerSymbol.X);
    }

    @Test
    void shouldAlternateTurnsAfterEachMove() {
        GameEngine.playMove(game, 0, PlayerSymbol.X);

        assertThat(GameEngine.nextSymbol(game)).isEqualTo(PlayerSymbol.O);

        GameEngine.playMove(game, 3, PlayerSymbol.O);

        assertThat(GameEngine.nextSymbol(game)).isEqualTo(PlayerSymbol.X);
    }

    @Test
    void shouldDetectHorizontalWin() {
        GameEngine.playMove(game, 0, PlayerSymbol.X);
        GameEngine.playMove(game, 3, PlayerSymbol.O);
        GameEngine.playMove(game, 1, PlayerSymbol.X);
        GameEngine.playMove(game, 4, PlayerSymbol.O);
        GameEngine.playMove(game, 2, PlayerSymbol.X);

        assertThat(game.getStatus()).isEqualTo(GameStatus.COMPLETED);
        assertThat(game.getOutcome()).isEqualTo(GameOutcome.X_WINS);
    }

    @Test
    void shouldDetectVerticalWin() {
        GameEngine.playMove(game, 0, PlayerSymbol.X);
        GameEngine.playMove(game, 1, PlayerSymbol.O);
        GameEngine.playMove(game, 3, PlayerSymbol.X);
        GameEngine.playMove(game, 4, PlayerSymbol.O);
        GameEngine.playMove(game, 6, PlayerSymbol.X);

        assertThat(game.getStatus()).isEqualTo(GameStatus.COMPLETED);
        assertThat(game.getOutcome()).isEqualTo(GameOutcome.X_WINS);
    }

    @Test
    void shouldDetectMainDiagonalWin() {
        GameEngine.playMove(game, 0, PlayerSymbol.X);
        GameEngine.playMove(game, 1, PlayerSymbol.O);
        GameEngine.playMove(game, 4, PlayerSymbol.X);
        GameEngine.playMove(game, 2, PlayerSymbol.O);
        GameEngine.playMove(game, 8, PlayerSymbol.X);

        assertThat(game.getStatus()).isEqualTo(GameStatus.COMPLETED);
        assertThat(game.getOutcome()).isEqualTo(GameOutcome.X_WINS);
    }

    @Test
    void shouldDetectAntiDiagonalWin() {
        GameEngine.playMove(game, 2, PlayerSymbol.X);
        GameEngine.playMove(game, 0, PlayerSymbol.O);
        GameEngine.playMove(game, 4, PlayerSymbol.X);
        GameEngine.playMove(game, 3, PlayerSymbol.O);
        GameEngine.playMove(game, 6, PlayerSymbol.X);

        assertThat(game.getStatus()).isEqualTo(GameStatus.COMPLETED);
        assertThat(game.getOutcome()).isEqualTo(GameOutcome.X_WINS);
    }

    @Test
    void shouldDetectDraw() {
        GameEngine.playMove(game, 0, PlayerSymbol.X);
        GameEngine.playMove(game, 1, PlayerSymbol.O);
        GameEngine.playMove(game, 2, PlayerSymbol.X);
        GameEngine.playMove(game, 4, PlayerSymbol.O);
        GameEngine.playMove(game, 3, PlayerSymbol.X);
        GameEngine.playMove(game, 5, PlayerSymbol.O);
        GameEngine.playMove(game, 7, PlayerSymbol.X);
        GameEngine.playMove(game, 6, PlayerSymbol.O);
        GameEngine.playMove(game, 8, PlayerSymbol.X);

        assertThat(game.getStatus()).isEqualTo(GameStatus.COMPLETED);
        assertThat(game.getOutcome()).isEqualTo(GameOutcome.DRAW);
    }

    @Test
    void shouldRejectMoveOutsideBoard() {
        assertThatThrownBy(() ->
                GameEngine.playMove(game, 9, PlayerSymbol.X)
        ).isInstanceOf(IllegalMoveException.class);
    }

    @Test
    void shouldRejectNegativeCellIndex() {
        assertThatThrownBy(() ->
                GameEngine.playMove(game, -1, PlayerSymbol.X)
        ).isInstanceOf(IllegalMoveException.class);
    }

    @Test
    void shouldRejectMoveOnOccupiedCell() {
        GameEngine.playMove(game, 0, PlayerSymbol.X);

        assertThatThrownBy(() ->
                GameEngine.playMove(game, 0, PlayerSymbol.O)
        ).isInstanceOf(IllegalMoveException.class);
    }

    @Test
    void shouldRejectMoveWhenItIsNotPlayersTurn() {
        assertThatThrownBy(() ->
                GameEngine.playMove(game, 0, PlayerSymbol.O)
        ).isInstanceOf(IllegalMoveException.class);
    }

    @Test
    void shouldRejectMovesAfterGameIsCompleted() {
        GameEngine.playMove(game, 0, PlayerSymbol.X);
        GameEngine.playMove(game, 3, PlayerSymbol.O);
        GameEngine.playMove(game, 1, PlayerSymbol.X);
        GameEngine.playMove(game, 4, PlayerSymbol.O);
        GameEngine.playMove(game, 2, PlayerSymbol.X);

        assertThatThrownBy(() ->
                GameEngine.playMove(game, 8, PlayerSymbol.O)
        ).isInstanceOf(IllegalMoveException.class);
    }

    @Test
    void shouldAllowPlayerOToStartTheGame() {
        GameState newGame = GameState.initial(PlayerSymbol.O);

        GameEngine.playMove(newGame, 0, PlayerSymbol.O);

        assertThat(newGame.getCell(0)).isEqualTo(PlayerSymbol.O);
        assertThat(GameEngine.nextSymbol(newGame)).isEqualTo(PlayerSymbol.X);
    }

    @Test
    void shouldNotChangeStateWhenMoveIsRejected() {
        GameEngine.playMove(game, 0, PlayerSymbol.X);

        assertThatThrownBy(() -> GameEngine.playMove(game, 0, PlayerSymbol.O))
                .isInstanceOf(IllegalMoveException.class);

        assertThat(game.getCell(0)).isEqualTo(PlayerSymbol.X);
        assertThat(GameEngine.nextSymbol(game)).isEqualTo(PlayerSymbol.O);
        assertThat(game.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
    }

    @Test
    void shouldThrowNullPointerWhenSymbolIsNull() {
        assertThatThrownBy(() -> GameEngine.playMove(game, 0, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldReturnNullAsNextSymbolWhenGameIsCompleted() {
        GameEngine.playMove(game, 0, PlayerSymbol.X);
        GameEngine.playMove(game, 3, PlayerSymbol.O);
        GameEngine.playMove(game, 1, PlayerSymbol.X);
        GameEngine.playMove(game, 4, PlayerSymbol.O);
        GameEngine.playMove(game, 2, PlayerSymbol.X);

        assertThat(GameEngine.nextSymbol(game)).isNull();
    }
}
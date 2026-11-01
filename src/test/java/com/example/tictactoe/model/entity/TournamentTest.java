package com.example.tictactoe.model.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.tictactoe.model.enums.GameOutcome;
import com.example.tictactoe.model.enums.GameStatus;
import com.example.tictactoe.model.enums.PlayerSymbol;
import com.example.tictactoe.model.enums.TournamentFormat;
import com.example.tictactoe.model.enums.TournamentStatus;

class TournamentTest {

    private Tournament tournament;
    private Player playerX;
    private Player playerO;

    @BeforeEach
    void setUp() {
        playerX = new Player("Halit", "halit");
        playerO = new Player("Alba", "alba");

        tournament = new Tournament(playerX, playerO, TournamentFormat.BEST_OF_3);
    }

    private Game addGame(int number, PlayerSymbol starter) {
        Game game = new Game(tournament, number, starter);
        tournament.addGame(game);
        return game;
    }

    private Game addCompletedGame(int number, PlayerSymbol starter, String board, GameOutcome outcome) {
        Game game = addGame(number, starter);
        game.applyState(board, GameStatus.COMPLETED, outcome);
        return game;
    }

    // ---- creation and lifecycle ----

    @Test
    void shouldCreateTournamentWithInitialState() {
        assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.CREATED);
        assertThat(tournament.getPlayerX()).isEqualTo(playerX);
        assertThat(tournament.getPlayerO()).isEqualTo(playerO);
        assertThat(tournament.getFormat()).isEqualTo(TournamentFormat.BEST_OF_3);
        assertThat(tournament.getGames()).isEmpty();
        assertThat(tournament.getWinner()).isNull();
    }

    @Test
    void shouldRejectSamePlayers() {
        assertThatThrownBy(() -> new Tournament(playerX, playerX, TournamentFormat.BEST_OF_1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldStartTournament() {
        tournament.start();

        assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.IN_PROGRESS);
    }

    @Test
    void shouldNotStartTournamentMoreThanOnce() {
        tournament.start();

        assertThatThrownBy(() -> tournament.start()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldNotStartCompletedTournament() {
        tournament.start();
        tournament.complete(playerX);

        assertThatThrownBy(() -> tournament.start()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldCompleteTournamentWithWinner() {
        tournament.start();

        tournament.complete(playerX);

        assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.COMPLETED);
        assertThat(tournament.getWinner()).isEqualTo(playerX);
    }

    @Test
    void shouldCompleteTournamentAsDraw() {
        tournament.start();

        tournament.complete(null);

        assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.COMPLETED);
        assertThat(tournament.getWinner()).isNull();
    }

    @Test
    void shouldNotCompleteTournamentBeforeStarting() {
        assertThatThrownBy(() -> tournament.complete(playerX)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldNotCompleteTournamentWithNonParticipantWinner() {
        tournament.start();

        Player anotherPlayer = new Player("Charlie", "charlie");

        assertThatThrownBy(() -> tournament.complete(anotherPlayer))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldNotCompleteTournamentTwice() {
        tournament.start();
        tournament.complete(playerX);

        assertThatThrownBy(() -> tournament.complete(playerO)).isInstanceOf(IllegalStateException.class);
    }

    // ---- games collection ----

    @Test
    void shouldAddGame() {
        Game game = addGame(1, PlayerSymbol.X);

        assertThat(tournament.getGames()).containsExactly(game);
    }

    @Test
    void shouldNotAllowExternalModificationOfGamesCollection() {
        assertThatThrownBy(() -> tournament.getGames().add(new Game(tournament, 1, PlayerSymbol.X)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // ---- current game ----

    @Test
    void shouldFindCurrentGameWhenOneIsInProgress() {
        Game game = addGame(1, PlayerSymbol.X);

        assertThat(tournament.findCurrentGame()).contains(game);
        assertThat(tournament.getCurrentGame()).isEqualTo(game);
    }

    @Test
    void shouldReturnEmptyCurrentGameWhenNoneInProgress() {
        addCompletedGame(1, PlayerSymbol.X, "XXXOO----", GameOutcome.X_WINS);

        assertThat(tournament.findCurrentGame()).isEmpty();
    }

    @Test
    void shouldThrowWhenGettingCurrentGameAndNoneInProgress() {
        assertThatThrownBy(() -> tournament.getCurrentGame()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldCountWinsDrawsAndCompletedGames() {
        addCompletedGame(1, PlayerSymbol.X, "XXXOO----", GameOutcome.X_WINS);
        addCompletedGame(2, PlayerSymbol.O, "OOOXX-X--", GameOutcome.O_WINS);
        addCompletedGame(3, PlayerSymbol.X, "XOXXOOOXX", GameOutcome.DRAW);
        addGame(4, PlayerSymbol.O);                      // still in progress

        assertThat(tournament.countWins(PlayerSymbol.X)).isEqualTo(1);
        assertThat(tournament.countWins(PlayerSymbol.O)).isEqualTo(1);
        assertThat(tournament.countDraws()).isEqualTo(1);
        assertThat(tournament.countCompletedGames()).isEqualTo(3);
    }

    @Test
     void shouldCountZeroWhenNoGames() {
        assertThat(tournament.countWins(PlayerSymbol.X)).isZero();
        assertThat(tournament.countWins(PlayerSymbol.O)).isZero();
        assertThat(tournament.countDraws()).isZero();
        assertThat(tournament.countCompletedGames()).isZero();
    }
}
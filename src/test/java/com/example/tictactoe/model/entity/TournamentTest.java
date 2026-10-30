
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
    void shouldNotStartGameBeforeTournamentStarts() {
        assertThatThrownBy(() -> tournament.startNextGame()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldStartFirstGameWithX() {
        tournament.start();

        Game game = tournament.startNextGame();

        assertThat(game.getGameNumber()).isEqualTo(1);
        assertThat(game.getStartingSymbol()).isEqualTo(PlayerSymbol.X);
        assertThat(game.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(game.getBoardState()).isEqualTo(Game.EMPTY_BOARD);
        assertThat(tournament.getGames()).containsExactly(game);
    }

    @Test
    void shouldStartSecondGameWithO() {
        tournament.start();

        Game firstGame = tournament.startNextGame();
        firstGame.complete(GameOutcome.X_WINS);

        Game secondGame = tournament.startNextGame();

        assertThat(secondGame.getGameNumber()).isEqualTo(2);
        assertThat(secondGame.getStartingSymbol()).isEqualTo(PlayerSymbol.O);

        assertThat(tournament.getGames()).containsExactly(firstGame, secondGame);
    }

    @Test
    void shouldAlternateStartingSymbolForEachGame() {
        tournament.start();

        Game firstGame = tournament.startNextGame();
        firstGame.complete(GameOutcome.X_WINS);

        Game secondGame = tournament.startNextGame();
        secondGame.complete(GameOutcome.O_WINS);

        Game thirdGame = tournament.startNextGame();

        assertThat(firstGame.getStartingSymbol()).isEqualTo(PlayerSymbol.X);
        assertThat(secondGame.getStartingSymbol()).isEqualTo(PlayerSymbol.O);
        assertThat(thirdGame.getStartingSymbol()).isEqualTo(PlayerSymbol.X);
    }

    @Test
    void shouldNotStartNextGameBeforePreviousGameCompletes() {
        tournament.start();

        tournament.startNextGame();

        assertThatThrownBy(() -> tournament.startNextGame()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldNotExceedMaximumNumberOfGames() {
        Tournament bestOfOne = new Tournament(playerX, playerO, TournamentFormat.BEST_OF_1);

        bestOfOne.start();

        Game firstGame = bestOfOne.startNextGame();
        firstGame.complete(GameOutcome.DRAW);

        assertThatThrownBy(() -> bestOfOne.startNextGame()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldCompleteTournamentWithWinner() {
        tournament.start();

        Game game = tournament.startNextGame();
        game.complete(GameOutcome.X_WINS);

        tournament.complete(playerX);

        assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.COMPLETED);
        assertThat(tournament.getWinner()).isEqualTo(playerX);
    }

    @Test
    void shouldCompleteTournamentAsDraw() {
        tournament.start();

        Game game = tournament.startNextGame();
        game.complete(GameOutcome.DRAW);

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

        Game game = tournament.startNextGame();
        game.complete(GameOutcome.X_WINS);

        Player anotherPlayer = new Player("Charlie", "charlie");

        assertThatThrownBy(() -> tournament.complete(anotherPlayer)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldNotCompleteTournamentTwice() {
        tournament.start();

        Game game = tournament.startNextGame();
        game.complete(GameOutcome.X_WINS);

        tournament.complete(playerX);

        assertThatThrownBy(() -> tournament.complete(playerO)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldNotAllowExternalModificationOfGamesCollection() {
        tournament.start();

        assertThatThrownBy(() -> tournament.getGames().add(
                new Game(tournament, 1, PlayerSymbol.X)
        )).isInstanceOf(UnsupportedOperationException.class);
    }
}

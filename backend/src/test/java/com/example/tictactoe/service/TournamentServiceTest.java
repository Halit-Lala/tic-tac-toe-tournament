package com.example.tictactoe.service;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.tictactoe.exception.IllegalMoveException;
import com.example.tictactoe.exception.ResourceNotFoundException;
import com.example.tictactoe.model.dto.request.NewTournamentDTO;
import com.example.tictactoe.model.dto.request.PlayMoveDTO;
import com.example.tictactoe.model.entity.Game;
import com.example.tictactoe.model.entity.Player;
import com.example.tictactoe.model.entity.Tournament;
import com.example.tictactoe.model.enums.GameOutcome;
import com.example.tictactoe.model.enums.GameStatus;
import com.example.tictactoe.model.enums.PlayerSymbol;
import com.example.tictactoe.model.enums.TournamentFormat;
import com.example.tictactoe.model.enums.TournamentStatus;
import com.example.tictactoe.model.mapper.GameMapper;
import com.example.tictactoe.model.mapper.TournamentMapper;
import com.example.tictactoe.repository.TournamentRepository;
import com.example.tictactoe.util.Constants;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    @Mock
    private PlayerService playerService;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TournamentMapper tournamentMapper;

    @Spy
    private GameMapper gameMapper = new GameMapper();

    @InjectMocks
    private TournamentService tournamentService;

    private Player playerX;
    private Player playerO;

    @BeforeEach
    void setUp() {
        playerX = new Player("Alice", "alice");
        playerO = new Player("Bob", "bob");
    }

    // ---- helpers ----

    private Tournament inProgressTournament(TournamentFormat format) {
        Tournament tournament = new Tournament(playerX, playerO, format);
        tournament.start();
        return tournament;
    }

    private Game addGame(Tournament tournament, int number, PlayerSymbol starter, String board) {
        Game game = new Game(tournament, number, starter);
        game.applyState(board, GameStatus.IN_PROGRESS, null);
        tournament.addGame(game);
        return game;
    }

    private Game addCompletedGame(Tournament tournament, int number, PlayerSymbol starter,
                                  String board, GameOutcome outcome) {
        Game game = new Game(tournament, number, starter);
        game.applyState(board, GameStatus.COMPLETED, outcome);
        tournament.addGame(game);
        return game;
    }

    private void givenTournament(Tournament tournament) {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    }

    // ---- createTournament ----

    @Test
    void shouldCreateTournamentWithFirstGameReady() {
        NewTournamentDTO request = new NewTournamentDTO("Alice", "Bob", TournamentFormat.BEST_OF_3);

        when(playerService.findOrCreatePlayerByName("Alice")).thenReturn(playerX);
        when(playerService.findOrCreatePlayerByName("Bob")).thenReturn(playerO);
        when(tournamentRepository.save(any(Tournament.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        tournamentService.createTournament(request);

        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository).save(captor.capture());

        Tournament saved = captor.getValue();

        assertEquals(playerX, saved.getPlayerX());
        assertEquals(playerO, saved.getPlayerO());
        assertEquals(TournamentFormat.BEST_OF_3, saved.getFormat());
        assertEquals(TournamentStatus.IN_PROGRESS, saved.getStatus());
        assertEquals(1, saved.getGames().size());

        Game firstGame = saved.getGames().get(0);
        assertEquals(1, firstGame.getGameNumber());
        assertEquals(PlayerSymbol.X, firstGame.getStartingSymbol());
        assertEquals(GameStatus.IN_PROGRESS, firstGame.getStatus());
        assertEquals(Constants.EMPTY_BOARD, firstGame.getBoardState());

        verify(playerService).validatePlayerName("Alice");
        verify(playerService).validatePlayerName("Bob");
        verify(tournamentMapper).toDTO(saved);
    }

    @Test
    void shouldThrowExceptionWhenRequestIsNull() {
        assertThrows(IllegalArgumentException.class, () -> tournamentService.createTournament(null));

        verifyNoInteractions(playerService, tournamentRepository, tournamentMapper);
    }

    @Test
    void shouldThrowExceptionWhenPlayerNamesAreIdentical() {
        NewTournamentDTO request = new NewTournamentDTO("Alice", " alice ", TournamentFormat.BEST_OF_3);

        assertThrows(IllegalArgumentException.class, () -> tournamentService.createTournament(request));

        verify(playerService).validatePlayerName("Alice");
        verify(playerService).validatePlayerName(" alice ");
        verify(playerService, never()).findOrCreatePlayerByName(any());
        verifyNoInteractions(tournamentRepository, tournamentMapper);
    }

    @Test
    void shouldThrowExceptionWhenTournamentFormatIsNull() {
        NewTournamentDTO request = new NewTournamentDTO("Alice", "Bob", null);

        assertThrows(IllegalArgumentException.class, () -> tournamentService.createTournament(request));

        verify(playerService, never()).findOrCreatePlayerByName(any());
        verifyNoInteractions(tournamentRepository, tournamentMapper);
    }

    // ---- getTournament ----

    @Test
    void shouldRetrieveTournamentSuccessfully() {
        Tournament tournament = new Tournament(playerX, playerO, TournamentFormat.BEST_OF_3);
        givenTournament(tournament);

        tournamentService.getTournament(1L);

        verify(tournamentRepository).findById(1L);
        verify(tournamentMapper).toDTO(tournament);
    }

    @Test
    void shouldThrowExceptionWhenRetrievingNonExistingTournament() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> tournamentService.getTournament(1L));

        assertEquals("Tournament not found with id: 1", exception.getMessage());
        verifyNoInteractions(tournamentMapper);
    }

    // ---- startNextGame ----

    @Test
    void shouldStartSecondGameWithOAfterFirstGameCompletes() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        addCompletedGame(tournament, 1, PlayerSymbol.X, "XXXOO----", GameOutcome.X_WINS);
        givenTournament(tournament);

        tournamentService.startNextGame(1L);

        assertThat(tournament.getGames()).hasSize(2);
        Game second = tournament.getGames().get(1);
        assertEquals(2, second.getGameNumber());
        assertEquals(PlayerSymbol.O, second.getStartingSymbol());
        assertEquals(GameStatus.IN_PROGRESS, second.getStatus());
        verify(tournamentMapper).toDTO(tournament);
    }

    @Test
    void shouldAlternateStartingSymbolForEachGame() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_5);
        addCompletedGame(tournament, 1, PlayerSymbol.X, "XXXOO----", GameOutcome.X_WINS);
        addCompletedGame(tournament, 2, PlayerSymbol.O, "OOOXX-X--", GameOutcome.O_WINS);
        givenTournament(tournament);

        tournamentService.startNextGame(1L);

        assertEquals(PlayerSymbol.X, tournament.getGames().get(2).getStartingSymbol());
    }

    @Test
    void shouldNotStartNextGameWhileCurrentGameIsInProgress() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        addGame(tournament, 1, PlayerSymbol.X, Constants.EMPTY_BOARD);
        givenTournament(tournament);

        assertThrows(IllegalMoveException.class, () -> tournamentService.startNextGame(1L));

        assertThat(tournament.getGames()).hasSize(1);
        verifyNoInteractions(tournamentMapper);
    }

    @Test
    void shouldNotStartNextGameWhenTournamentIsCompleted() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        addCompletedGame(tournament, 1, PlayerSymbol.X, "XXXOO----", GameOutcome.X_WINS);
        tournament.complete(playerX);
        givenTournament(tournament);

        assertThrows(IllegalMoveException.class, () -> tournamentService.startNextGame(1L));
    }

    @Test
    void shouldNotStartNextGameWhenTournamentIsNotStarted() {
        Tournament tournament = new Tournament(playerX, playerO, TournamentFormat.BEST_OF_3);
        givenTournament(tournament);

        assertThrows(IllegalMoveException.class, () -> tournamentService.startNextGame(1L));
    }

    @Test
    void shouldNotExceedMaximumNumberOfGames() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_1);
        addCompletedGame(tournament, 1, PlayerSymbol.X, "XOXXOOOXX", GameOutcome.DRAW);
        givenTournament(tournament);

        assertThrows(IllegalMoveException.class, () -> tournamentService.startNextGame(1L));

        assertThat(tournament.getGames()).hasSize(1);
    }

    @Test
    void shouldThrowWhenStartingNextGameOfNonExistingTournament() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tournamentService.startNextGame(1L));

        verifyNoInteractions(tournamentMapper);
    }

    // ---- playMove: rejected moves ----

    @Test
    void shouldPlayMoveAndUpdateBoard() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        Game game = addGame(tournament, 1, PlayerSymbol.X, Constants.EMPTY_BOARD);
        givenTournament(tournament);

        tournamentService.playMove(1L, 1, new PlayMoveDTO(0, 0, PlayerSymbol.X));

        assertEquals("X--------", game.getBoardState());
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        assertEquals(TournamentStatus.IN_PROGRESS, tournament.getStatus());
        verify(tournamentMapper).toDTO(tournament);
    }

    @Test
    void shouldRejectMoveOutOfTurn() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        Game game = addGame(tournament, 1, PlayerSymbol.X, Constants.EMPTY_BOARD);
        givenTournament(tournament);

        assertThrows(IllegalMoveException.class,
                () -> tournamentService.playMove(1L, 1, new PlayMoveDTO(0, 0, PlayerSymbol.O)));

        assertEquals(Constants.EMPTY_BOARD, game.getBoardState());
    }

    @Test
    void shouldRejectMoveOnOccupiedCell() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        Game game = addGame(tournament, 1, PlayerSymbol.X, "X--------");
        givenTournament(tournament);

        assertThrows(IllegalMoveException.class,
                () -> tournamentService.playMove(1L, 1, new PlayMoveDTO(0, 0, PlayerSymbol.O)));

        assertEquals("X--------", game.getBoardState());
    }

    @Test
    void shouldRejectMoveForStaleGameNumber() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        addCompletedGame(tournament, 1, PlayerSymbol.X, "XXXOO----", GameOutcome.X_WINS);
        Game current = addGame(tournament, 2, PlayerSymbol.O, Constants.EMPTY_BOARD);
        givenTournament(tournament);

        assertThrows(ResourceNotFoundException.class,
                () -> tournamentService.playMove(1L, 1, new PlayMoveDTO(0, 0, PlayerSymbol.O)));

        assertEquals(Constants.EMPTY_BOARD, current.getBoardState());
        verifyNoInteractions(tournamentMapper);
    }

    @Test
    void shouldRejectMoveWhenNoGameIsInProgress() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        addCompletedGame(tournament, 1, PlayerSymbol.X, "XXXOO----", GameOutcome.X_WINS);
        givenTournament(tournament);

        assertThrows(IllegalMoveException.class,
                () -> tournamentService.playMove(1L, 2, new PlayMoveDTO(1, 2, PlayerSymbol.O)));
    }

    @Test
    void shouldRejectMoveWhenTournamentIsCompleted() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_1);
        addCompletedGame(tournament, 1, PlayerSymbol.X, "XXXOO----", GameOutcome.X_WINS);
        tournament.complete(playerX);
        givenTournament(tournament);

        assertThrows(IllegalMoveException.class,
                () -> tournamentService.playMove(1L, 1, new PlayMoveDTO(1, 2, PlayerSymbol.O)));
    }

    @Test
    void shouldRejectMoveWhenTournamentIsNotStarted() {
        Tournament tournament = new Tournament(playerX, playerO, TournamentFormat.BEST_OF_3);
        givenTournament(tournament);

        assertThrows(IllegalMoveException.class,
                () -> tournamentService.playMove(1L, 1, new PlayMoveDTO(0, 0, PlayerSymbol.X)));
    }

    @Test
    void shouldThrowWhenPlayingMoveOnNonExistingTournament() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> tournamentService.playMove(1L, 1, new PlayMoveDTO(0, 0, PlayerSymbol.X)));

        verifyNoInteractions(tournamentMapper);
    }

    // ---- playMove: game and tournament completion ----

    @Test
    void shouldCompleteGameButKeepTournamentOpenWhenNotDecided() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        Game game = addGame(tournament, 1, PlayerSymbol.X, "XX-OO----");
        givenTournament(tournament);

        tournamentService.playMove(1L, 1, new PlayMoveDTO(0, 2, PlayerSymbol.X));

        assertEquals(GameStatus.COMPLETED, game.getStatus());
        assertEquals(GameOutcome.X_WINS, game.getOutcome());
        assertEquals(TournamentStatus.IN_PROGRESS, tournament.getStatus());
        assertThat(tournament.findCurrentGame()).isEmpty();
        assertThat(tournament.getGames()).hasSize(1);     // next game is not created automatically
    }

    @Test
    void shouldFinishBestOfThreeEarlyWhenOnePlayerWinsTwoGames() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        addCompletedGame(tournament, 1, PlayerSymbol.X, "XXXOO----", GameOutcome.X_WINS);
        Game second = addGame(tournament, 2, PlayerSymbol.O, "OO-XX-O--");   // X to move
        givenTournament(tournament);

        tournamentService.playMove(1L, 2, new PlayMoveDTO(1, 2, PlayerSymbol.X));

        assertEquals(GameOutcome.X_WINS, second.getOutcome());
        assertEquals(TournamentStatus.COMPLETED, tournament.getStatus());
        assertEquals(playerX, tournament.getWinner());
    }

    @Test
    void shouldNotFinishBestOfThreeWhenScoreIsOneOne() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        addCompletedGame(tournament, 1, PlayerSymbol.X, "XXXOO----", GameOutcome.X_WINS);
        addGame(tournament, 2, PlayerSymbol.O, "OO-XX----");                  // O to move
        givenTournament(tournament);

        tournamentService.playMove(1L, 2, new PlayMoveDTO(0, 2, PlayerSymbol.O));

        assertEquals(TournamentStatus.IN_PROGRESS, tournament.getStatus());
        assertNull(tournament.getWinner());
    }

    @Test
    void shouldDeclareTournamentDrawWhenBestOfOneEndsInDraw() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_1);
        Game game = addGame(tournament, 1, PlayerSymbol.X, "XOXXOOOX-");
        givenTournament(tournament);

        tournamentService.playMove(1L, 1, new PlayMoveDTO(2, 2, PlayerSymbol.X));

        assertEquals(GameOutcome.DRAW, game.getOutcome());
        assertEquals(TournamentStatus.COMPLETED, tournament.getStatus());
        assertNull(tournament.getWinner());
    }

    @Test
    void shouldDeclareTournamentDrawWhenWinsAreEqualAfterAllGames() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        addCompletedGame(tournament, 1, PlayerSymbol.X, "XXXOO----", GameOutcome.X_WINS);
        addCompletedGame(tournament, 2, PlayerSymbol.O, "OOOXX-X--", GameOutcome.O_WINS);
        addGame(tournament, 3, PlayerSymbol.X, "XOXXOOOX-");
        givenTournament(tournament);

        tournamentService.playMove(1L, 3, new PlayMoveDTO(2, 2, PlayerSymbol.X));

        assertEquals(TournamentStatus.COMPLETED, tournament.getStatus());
        assertNull(tournament.getWinner());
    }
}
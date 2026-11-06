package com.example.tictactoe.service;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.springframework.context.ApplicationEventPublisher;

import com.example.tictactoe.exception.ForbiddenException;
import com.example.tictactoe.exception.IllegalMoveException;
import com.example.tictactoe.exception.ResourceNotFoundException;
import com.example.tictactoe.model.dto.request.NewTournamentDTO;
import com.example.tictactoe.model.dto.request.PlayMoveDTO;
import com.example.tictactoe.model.dto.response.CreatedTournamentDTO;
import com.example.tictactoe.model.dto.response.SeatAccessDTO;
import com.example.tictactoe.model.entity.Game;
import com.example.tictactoe.model.entity.Player;
import com.example.tictactoe.model.entity.Tournament;
import com.example.tictactoe.model.enums.GameOutcome;
import com.example.tictactoe.model.enums.GameStatus;
import com.example.tictactoe.model.enums.PlayerSymbol;
import com.example.tictactoe.model.enums.TournamentFormat;
import com.example.tictactoe.model.enums.TournamentMode;
import com.example.tictactoe.model.enums.TournamentStatus;
import com.example.tictactoe.model.mapper.GameMapper;
import com.example.tictactoe.model.mapper.TournamentMapper;
import com.example.tictactoe.repository.TournamentRepository;
import com.example.tictactoe.util.Constants;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    private static final String X_TOKEN = "token-x";
    private static final String O_TOKEN = "token-o";
    private static final String INVITE = "invite-token";

    @Mock
    private PlayerService playerService;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TournamentMapper tournamentMapper;

    @Mock
    private ApplicationEventPublisher events;

    @Mock
    private EntityManager entityManager;

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

    /** Online tournament created by X. The invite is still unclaimed. */
    private Tournament onlineAwaitingOpponent() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        tournament.makeOnline(PlayerSymbol.X, X_TOKEN, O_TOKEN, INVITE);
        return tournament;
    }

    /** Online tournament where both seats are taken. */
    private Tournament onlineReady() {
        Tournament tournament = onlineAwaitingOpponent();
        tournament.claimInvite(INVITE);
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

    private PlayMoveDTO move(int row, int col, PlayerSymbol symbol) {
        return new PlayMoveDTO(row, col, symbol);
    }

    // ---- createTournament (local) ----

    @Test
    void shouldCreateLocalTournamentWithFirstGameReady() {
        NewTournamentDTO request = new NewTournamentDTO("Alice", "Bob", TournamentFormat.BEST_OF_3, TournamentMode.LOCAL, null);

        when(playerService.findOrCreatePlayerByName("Alice")).thenReturn(playerX);
        when(playerService.findOrCreatePlayerByName("Bob")).thenReturn(playerO);
        when(tournamentRepository.save(any(Tournament.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreatedTournamentDTO created = tournamentService.createTournament(request);

        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository).save(captor.capture());
        Tournament saved = captor.getValue();

        assertEquals(playerX, saved.getPlayerX());
        assertEquals(playerO, saved.getPlayerO());
        assertEquals(TournamentFormat.BEST_OF_3, saved.getFormat());
        assertEquals(TournamentStatus.IN_PROGRESS, saved.getStatus());
        assertEquals(TournamentMode.LOCAL, saved.getMode());
        assertEquals(1, saved.getGames().size());

        Game firstGame = saved.getGames().get(0);
        assertEquals(1, firstGame.getGameNumber());
        assertEquals(PlayerSymbol.X, firstGame.getStartingSymbol());
        assertEquals(GameStatus.IN_PROGRESS, firstGame.getStatus());
        assertEquals(Constants.EMPTY_BOARD, firstGame.getBoardState());

        // local tournaments have no seats or invites
        assertNull(created.seat());
        assertNull(created.seatToken());
        assertNull(created.inviteToken());

        verify(playerService).validatePlayerName("Alice");
        verify(playerService).validatePlayerName("Bob");
        verify(tournamentMapper).toDTO(saved);
    }

    @Test
    void shouldThrowExceptionWhenRequestIsNull() {
        assertThrows(IllegalArgumentException.class, () -> tournamentService.createTournament(null));

        verifyNoInteractions(playerService, tournamentRepository, tournamentMapper, events);
    }

    @Test
    void shouldThrowExceptionWhenPlayerNamesAreIdentical() {
        NewTournamentDTO request = new NewTournamentDTO("Alice", " alice ", TournamentFormat.BEST_OF_3, TournamentMode.LOCAL, null);

        assertThrows(IllegalArgumentException.class, () -> tournamentService.createTournament(request));

        verify(playerService).validatePlayerName("Alice");
        verify(playerService).validatePlayerName(" alice ");
        verify(playerService, never()).findOrCreatePlayerByName(any());
        verifyNoInteractions(tournamentRepository, tournamentMapper);
    }

    @Test
    void shouldThrowExceptionWhenTournamentFormatIsNull() {
        NewTournamentDTO request = new NewTournamentDTO("Alice", "Bob", null, TournamentMode.LOCAL, null);

        assertThrows(IllegalArgumentException.class, () -> tournamentService.createTournament(request));

        verify(playerService, never()).findOrCreatePlayerByName(any());
        verifyNoInteractions(tournamentRepository, tournamentMapper);
    }

    // ---- createTournament (online) ----

    @Test
    void shouldCreateOnlineTournamentWithSeatAndInvite() {
        NewTournamentDTO request = new NewTournamentDTO(
                "Alice", "Bob", TournamentFormat.BEST_OF_3, TournamentMode.ONLINE, PlayerSymbol.O);

        when(playerService.findOrCreatePlayerByName("Alice")).thenReturn(playerX);
        when(playerService.findOrCreatePlayerByName("Bob")).thenReturn(playerO);
        when(tournamentRepository.save(any(Tournament.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreatedTournamentDTO created = tournamentService.createTournament(request);

        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository).save(captor.capture());
        Tournament saved = captor.getValue();

        assertEquals(TournamentMode.ONLINE, saved.getMode());
        assertThat(saved.isAwaitingOpponent()).isTrue();

        // the creator gets the seat they chose, plus an invite for the other one
        assertEquals(PlayerSymbol.O, created.seat());
        assertNotNull(created.seatToken());
        assertNotNull(created.inviteToken());
        assertThat(created.seatToken()).isNotEqualTo(created.inviteToken());
        assertThat(saved.seatFor(created.seatToken())).contains(PlayerSymbol.O);
    }

    @Test
    void shouldRejectOnlineTournamentWithoutCreatorSeat() {
        NewTournamentDTO request = new NewTournamentDTO(
                "Alice", "Bob", TournamentFormat.BEST_OF_3, TournamentMode.ONLINE, null);

        assertThrows(IllegalArgumentException.class, () -> tournamentService.createTournament(request));

        verifyNoInteractions(tournamentRepository);
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

    // ---- joinTournament ----

    @Test
    void shouldGiveTheOtherSeatWhenInviteIsClaimed() {
        Tournament tournament = onlineAwaitingOpponent();   // created by X, so the invite is for O
        givenTournament(tournament);

        SeatAccessDTO access = tournamentService.joinTournament(1L, INVITE);

        assertEquals(PlayerSymbol.O, access.seat());
        assertEquals(O_TOKEN, access.seatToken());
        assertThat(tournament.isAwaitingOpponent()).isFalse();
        verify(events).publishEvent(any(Object.class));   // the creator sees "opponent joined"
    }

    @Test
    void shouldRejectWrongInviteToken() {
        Tournament tournament = onlineAwaitingOpponent();
        givenTournament(tournament);

        assertThrows(ForbiddenException.class, () -> tournamentService.joinTournament(1L, "wrong"));

        assertThat(tournament.isAwaitingOpponent()).isTrue();
        verifyNoInteractions(events);
    }

    @Test
    void shouldRejectInviteThatWasAlreadyClaimed() {
        Tournament tournament = onlineAwaitingOpponent();
        givenTournament(tournament);

        tournamentService.joinTournament(1L, INVITE);

        assertThrows(ForbiddenException.class, () -> tournamentService.joinTournament(1L, INVITE));
    }

    @Test
    void shouldRejectInviteForLocalTournament() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        givenTournament(tournament);

        assertThrows(ForbiddenException.class, () -> tournamentService.joinTournament(1L, INVITE));
    }

    @Test
    void shouldThrowWhenJoiningNonExistingTournament() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tournamentService.joinTournament(1L, INVITE));
    }

    // ---- startNextGame ----

    @Test
    void shouldStartSecondGameWithOAfterFirstGameCompletes() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        addCompletedGame(tournament, 1, PlayerSymbol.X, "XXXOO----", GameOutcome.X_WINS);
        givenTournament(tournament);

        tournamentService.startNextGame(1L, null);

        assertThat(tournament.getGames()).hasSize(2);
        Game second = tournament.getGames().get(1);
        assertEquals(2, second.getGameNumber());
        assertEquals(PlayerSymbol.O, second.getStartingSymbol());
        assertEquals(GameStatus.IN_PROGRESS, second.getStatus());
        verify(tournamentMapper).toDTO(tournament);
        verify(events).publishEvent(any(Object.class));
    }

    @Test
    void shouldAlternateStartingSymbolForEachGame() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_5);
        addCompletedGame(tournament, 1, PlayerSymbol.X, "XXXOO----", GameOutcome.X_WINS);
        addCompletedGame(tournament, 2, PlayerSymbol.O, "OOOXX-X--", GameOutcome.O_WINS);
        givenTournament(tournament);

        tournamentService.startNextGame(1L, null);

        assertEquals(PlayerSymbol.X, tournament.getGames().get(2).getStartingSymbol());
    }

    @Test
    void shouldNotStartNextGameWhileCurrentGameIsInProgress() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        addGame(tournament, 1, PlayerSymbol.X, Constants.EMPTY_BOARD);
        givenTournament(tournament);

        assertThrows(IllegalMoveException.class, () -> tournamentService.startNextGame(1L, null));

        assertThat(tournament.getGames()).hasSize(1);
        verifyNoInteractions(tournamentMapper, events);
    }

    @Test
    void shouldNotStartNextGameWhenTournamentIsCompleted() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        addCompletedGame(tournament, 1, PlayerSymbol.X, "XXXOO----", GameOutcome.X_WINS);
        tournament.complete(playerX);
        givenTournament(tournament);

        assertThrows(IllegalMoveException.class, () -> tournamentService.startNextGame(1L, null));
    }

    @Test
    void shouldNotStartNextGameWhenTournamentIsNotStarted() {
        Tournament tournament = new Tournament(playerX, playerO, TournamentFormat.BEST_OF_3);
        givenTournament(tournament);

        assertThrows(IllegalMoveException.class, () -> tournamentService.startNextGame(1L, null));
    }

    @Test
    void shouldNotExceedMaximumNumberOfGames() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_1);
        addCompletedGame(tournament, 1, PlayerSymbol.X, "XOXXOOOXX", GameOutcome.DRAW);
        givenTournament(tournament);

        assertThrows(IllegalMoveException.class, () -> tournamentService.startNextGame(1L, null));

        assertThat(tournament.getGames()).hasSize(1);
    }

    @Test
    void shouldThrowWhenStartingNextGameOfNonExistingTournament() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tournamentService.startNextGame(1L, null));

        verifyNoInteractions(tournamentMapper);
    }

    @Test
    void shouldStartNextGameOnlineWithAValidSeatToken() {
        Tournament tournament = onlineReady();
        addCompletedGame(tournament, 1, PlayerSymbol.X, "XXXOO----", GameOutcome.X_WINS);
        givenTournament(tournament);

        tournamentService.startNextGame(1L, O_TOKEN);

        assertThat(tournament.getGames()).hasSize(2);
    }

    @Test
    void shouldRejectNextGameOnlineWithoutToken() {
        Tournament tournament = onlineReady();
        addCompletedGame(tournament, 1, PlayerSymbol.X, "XXXOO----", GameOutcome.X_WINS);
        givenTournament(tournament);

        assertThrows(ForbiddenException.class, () -> tournamentService.startNextGame(1L, null));

        assertThat(tournament.getGames()).hasSize(1);
        verifyNoInteractions(events);
    }

    @Test
    void shouldRejectNextGameOnlineWithWrongToken() {
        Tournament tournament = onlineReady();
        addCompletedGame(tournament, 1, PlayerSymbol.X, "XXXOO----", GameOutcome.X_WINS);
        givenTournament(tournament);

        assertThrows(ForbiddenException.class, () -> tournamentService.startNextGame(1L, "wrong"));
    }

    // ---- playMove: local mode ----

    @Test
    void shouldPlayMoveAndUpdateBoard() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        Game game = addGame(tournament, 1, PlayerSymbol.X, Constants.EMPTY_BOARD);
        givenTournament(tournament);

        tournamentService.playMove(1L, 1, move(0, 0, PlayerSymbol.X), null);

        assertEquals("X--------", game.getBoardState());
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        assertEquals(TournamentStatus.IN_PROGRESS, tournament.getStatus());
        verify(tournamentMapper).toDTO(tournament);
        verify(events).publishEvent(any(Object.class));
    }

    @Test
    void shouldRequireSymbolInLocalMode() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        Game game = addGame(tournament, 1, PlayerSymbol.X, Constants.EMPTY_BOARD);
        givenTournament(tournament);

        assertThrows(IllegalArgumentException.class,
                () -> tournamentService.playMove(1L, 1, move(0, 0, null), null));

        assertEquals(Constants.EMPTY_BOARD, game.getBoardState());
    }

    @Test
    void shouldIgnoreTokenInLocalMode() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        Game game = addGame(tournament, 1, PlayerSymbol.X, Constants.EMPTY_BOARD);
        givenTournament(tournament);

        tournamentService.playMove(1L, 1, move(0, 0, PlayerSymbol.X), "any-token");

        assertEquals("X--------", game.getBoardState());
    }

    @Test
    void shouldRejectMoveOutOfTurn() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        Game game = addGame(tournament, 1, PlayerSymbol.X, Constants.EMPTY_BOARD);
        givenTournament(tournament);

        assertThrows(IllegalMoveException.class,
                () -> tournamentService.playMove(1L, 1, move(0, 0, PlayerSymbol.O), null));

        assertEquals(Constants.EMPTY_BOARD, game.getBoardState());
        verifyNoInteractions(events);   // a rejected move is never broadcast
    }

    @Test
    void shouldRejectMoveOnOccupiedCell() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        Game game = addGame(tournament, 1, PlayerSymbol.X, "X--------");
        givenTournament(tournament);

        assertThrows(IllegalMoveException.class,
                () -> tournamentService.playMove(1L, 1, move(0, 0, PlayerSymbol.O), null));

        assertEquals("X--------", game.getBoardState());
    }

    @Test
    void shouldRejectMoveForStaleGameNumber() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        addCompletedGame(tournament, 1, PlayerSymbol.X, "XXXOO----", GameOutcome.X_WINS);
        Game current = addGame(tournament, 2, PlayerSymbol.O, Constants.EMPTY_BOARD);
        givenTournament(tournament);

        assertThrows(ResourceNotFoundException.class,
                () -> tournamentService.playMove(1L, 1, move(0, 0, PlayerSymbol.O), null));

        assertEquals(Constants.EMPTY_BOARD, current.getBoardState());
        verifyNoInteractions(tournamentMapper);
    }

    @Test
    void shouldRejectMoveWhenNoGameIsInProgress() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        addCompletedGame(tournament, 1, PlayerSymbol.X, "XXXOO----", GameOutcome.X_WINS);
        givenTournament(tournament);

        assertThrows(IllegalMoveException.class,
                () -> tournamentService.playMove(1L, 2, move(1, 2, PlayerSymbol.O), null));
    }

    @Test
    void shouldRejectMoveWhenTournamentIsCompleted() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_1);
        addCompletedGame(tournament, 1, PlayerSymbol.X, "XXXOO----", GameOutcome.X_WINS);
        tournament.complete(playerX);
        givenTournament(tournament);

        assertThrows(IllegalMoveException.class,
                () -> tournamentService.playMove(1L, 1, move(1, 2, PlayerSymbol.O), null));
    }

    @Test
    void shouldRejectMoveWhenTournamentIsNotStarted() {
        Tournament tournament = new Tournament(playerX, playerO, TournamentFormat.BEST_OF_3);
        givenTournament(tournament);

        assertThrows(IllegalMoveException.class,
                () -> tournamentService.playMove(1L, 1, move(0, 0, PlayerSymbol.X), null));
    }

    @Test
    void shouldThrowWhenPlayingMoveOnNonExistingTournament() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> tournamentService.playMove(1L, 1, move(0, 0, PlayerSymbol.X), null));

        verifyNoInteractions(tournamentMapper, events);
    }

    // ---- playMove: online mode ----

    @Test
    void shouldLockTheTournamentBeforePlayingAMove() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        addGame(tournament, 1, PlayerSymbol.X, Constants.EMPTY_BOARD);
        givenTournament(tournament);

        tournamentService.playMove(1L, 1, move(0, 0, PlayerSymbol.X), null);

        verify(entityManager).lock(tournament, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
    }

    @Test
    void shouldPlayOnlineMoveWithTheSymbolOfTheSeat() {
        Tournament tournament = onlineReady();
        Game game = addGame(tournament, 1, PlayerSymbol.X, Constants.EMPTY_BOARD);
        givenTournament(tournament);

        tournamentService.playMove(1L, 1, move(0, 0, null), X_TOKEN);

        assertEquals("X--------", game.getBoardState());
        verify(events).publishEvent(any(Object.class));
    }

    @Test
    void shouldIgnoreSymbolSentByAnOnlineClient() {
        Tournament tournament = onlineReady();
        Game game = addGame(tournament, 1, PlayerSymbol.X, Constants.EMPTY_BOARD);
        givenTournament(tournament);

        // X's token with symbol O in the body: the token wins, so this is an X move
        tournamentService.playMove(1L, 1, move(0, 0, PlayerSymbol.O), X_TOKEN);

        assertEquals("X--------", game.getBoardState());
    }

    @Test
    void shouldRejectOnlineMoveWithoutToken() {
        Tournament tournament = onlineReady();
        Game game = addGame(tournament, 1, PlayerSymbol.X, Constants.EMPTY_BOARD);
        givenTournament(tournament);

        assertThrows(ForbiddenException.class,
                () -> tournamentService.playMove(1L, 1, move(0, 0, PlayerSymbol.X), null));

        assertEquals(Constants.EMPTY_BOARD, game.getBoardState());
        verifyNoInteractions(events);
    }

    @Test
    void shouldRejectOnlineMoveWithWrongToken() {
        Tournament tournament = onlineReady();
        Game game = addGame(tournament, 1, PlayerSymbol.X, Constants.EMPTY_BOARD);
        givenTournament(tournament);

        assertThrows(ForbiddenException.class,
                () -> tournamentService.playMove(1L, 1, move(0, 0, PlayerSymbol.X), "wrong"));

        assertEquals(Constants.EMPTY_BOARD, game.getBoardState());
    }

    @Test
    void shouldRejectOnlineMoveFromThePlayerWhoseTurnItIsNot() {
        Tournament tournament = onlineReady();
        Game game = addGame(tournament, 1, PlayerSymbol.X, Constants.EMPTY_BOARD);
        givenTournament(tournament);

        // a valid seat, but it is X's turn
        assertThrows(IllegalMoveException.class,
                () -> tournamentService.playMove(1L, 1, move(0, 0, null), O_TOKEN));

        assertEquals(Constants.EMPTY_BOARD, game.getBoardState());
        verifyNoInteractions(events);
    }

    @Test
    void shouldRejectOnlineMoveBeforeTheOpponentJoined() {
        Tournament tournament = onlineAwaitingOpponent();
        Game game = addGame(tournament, 1, PlayerSymbol.X, Constants.EMPTY_BOARD);
        givenTournament(tournament);

        assertThrows(IllegalMoveException.class,
                () -> tournamentService.playMove(1L, 1, move(0, 0, null), X_TOKEN));

        assertEquals(Constants.EMPTY_BOARD, game.getBoardState());
    }

    @Test
    void shouldFinishOnlineTournamentWhenTheWinningMoveIsPlayed() {
        Tournament tournament = onlineReady();
        tournament.getFormat();
        Game game = addGame(tournament, 1, PlayerSymbol.X, "XX-OO----");
        givenTournament(tournament);

        tournamentService.playMove(1L, 1, move(0, 2, null), X_TOKEN);

        assertEquals(GameOutcome.X_WINS, game.getOutcome());
        assertEquals(TournamentStatus.IN_PROGRESS, tournament.getStatus());   // best of 3 is not decided yet
    }

    // ---- playMove: game and tournament completion ----

    @Test
    void shouldCompleteGameButKeepTournamentOpenWhenNotDecided() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        Game game = addGame(tournament, 1, PlayerSymbol.X, "XX-OO----");
        givenTournament(tournament);

        tournamentService.playMove(1L, 1, move(0, 2, PlayerSymbol.X), null);

        assertEquals(GameStatus.COMPLETED, game.getStatus());
        assertEquals(GameOutcome.X_WINS, game.getOutcome());
        assertEquals(TournamentStatus.IN_PROGRESS, tournament.getStatus());
        assertThat(tournament.findCurrentGame()).isEmpty();
        assertThat(tournament.getGames()).hasSize(1);   // next game is not created automatically
    }

    @Test
    void shouldFinishBestOfThreeEarlyWhenOnePlayerWinsTwoGames() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        addCompletedGame(tournament, 1, PlayerSymbol.X, "XXXOO----", GameOutcome.X_WINS);
        Game second = addGame(tournament, 2, PlayerSymbol.O, "OO-XX-O--");   // X to move
        givenTournament(tournament);

        tournamentService.playMove(1L, 2, move(1, 2, PlayerSymbol.X), null);

        assertEquals(GameOutcome.X_WINS, second.getOutcome());
        assertEquals(TournamentStatus.COMPLETED, tournament.getStatus());
        assertEquals(playerX, tournament.getWinner());
    }

    @Test
    void shouldNotFinishBestOfThreeWhenScoreIsOneOne() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_3);
        addCompletedGame(tournament, 1, PlayerSymbol.X, "XXXOO----", GameOutcome.X_WINS);
        addGame(tournament, 2, PlayerSymbol.O, "OO-XX----");   // O to move
        givenTournament(tournament);

        tournamentService.playMove(1L, 2, move(0, 2, PlayerSymbol.O), null);

        assertEquals(TournamentStatus.IN_PROGRESS, tournament.getStatus());
        assertNull(tournament.getWinner());
    }

    @Test
    void shouldDeclareTournamentDrawWhenBestOfOneEndsInDraw() {
        Tournament tournament = inProgressTournament(TournamentFormat.BEST_OF_1);
        Game game = addGame(tournament, 1, PlayerSymbol.X, "XOXXOOOX-");
        givenTournament(tournament);

        tournamentService.playMove(1L, 1, move(2, 2, PlayerSymbol.X), null);

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

        tournamentService.playMove(1L, 3, move(2, 2, PlayerSymbol.X), null);

        assertEquals(TournamentStatus.COMPLETED, tournament.getStatus());
        assertNull(tournament.getWinner());
    }
}
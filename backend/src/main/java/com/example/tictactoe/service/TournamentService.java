package com.example.tictactoe.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tictactoe.exception.ForbiddenException;
import com.example.tictactoe.exception.IllegalMoveException;
import com.example.tictactoe.exception.ResourceNotFoundException;
import com.example.tictactoe.model.domain.GameState;
import com.example.tictactoe.model.dto.request.NewTournamentDTO;
import com.example.tictactoe.model.dto.request.PlayMoveDTO;
import com.example.tictactoe.model.dto.response.CreatedTournamentDTO;
import com.example.tictactoe.model.dto.response.PageDTO;
import com.example.tictactoe.model.dto.response.SeatAccessDTO;
import com.example.tictactoe.model.dto.response.TournamentDTO;
import com.example.tictactoe.model.dto.response.TournamentHistoryEntryDTO;
import com.example.tictactoe.model.dto.response.TournamentUpdatedEvent;
import com.example.tictactoe.model.entity.Game;
import com.example.tictactoe.model.entity.Player;
import com.example.tictactoe.model.entity.Tournament;
import com.example.tictactoe.model.enums.GameStatus;
import com.example.tictactoe.model.enums.PlayerSymbol;
import com.example.tictactoe.model.enums.TournamentMode;
import com.example.tictactoe.model.enums.TournamentStatus;
import com.example.tictactoe.model.mapper.GameMapper;
import com.example.tictactoe.model.mapper.TournamentMapper;
import com.example.tictactoe.repository.TournamentRepository;
import com.example.tictactoe.util.Constants;
import com.example.tictactoe.util.TokenGenerator;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@Service
public class TournamentService {

    private final PlayerService playerService;
    private final ApplicationEventPublisher eventPublisher;
    private final TournamentRepository tournamentRepository;
    private final TournamentMapper tournamentMapper;
    private final GameMapper gameMapper;
    private final EntityManager entityManager;


    public TournamentService(
        PlayerService playerService,
        ApplicationEventPublisher eventPublisher,
        TournamentRepository tournamentRepository,
        TournamentMapper tournamentMapper,
        GameMapper gameMapper,
        EntityManager entityManager
    ) {
        this.playerService = playerService;
        this.eventPublisher = eventPublisher;
        this.tournamentRepository = tournamentRepository;
        this.tournamentMapper = tournamentMapper;
        this.gameMapper = gameMapper;
        this.entityManager = entityManager;
    }

    @Transactional
    public CreatedTournamentDTO createTournament(NewTournamentDTO request) {
        validateCreateTournamentRequest(request);

        Player playerX = playerService.findOrCreatePlayerByName(request.playerXName());
        Player playerO = playerService.findOrCreatePlayerByName(request.playerOName());

        Tournament tournament = new Tournament(playerX, playerO, request.format());
        tournament.start();
        createNextGame(tournament);

        PlayerSymbol seat = null;
        String seatToken = null;
        String inviteToken = null;
        if (request.mode() == TournamentMode.ONLINE) {
            seat = request.creatorSeat();
            String xToken = TokenGenerator.newToken();
            String oToken = TokenGenerator.newToken();
            inviteToken = TokenGenerator.newToken();
            tournament.makeOnline(seat, xToken, oToken, inviteToken);
            seatToken = seat == PlayerSymbol.X ? xToken : oToken;
        }

        Tournament savedTournament = tournamentRepository.save(tournament);
        return new CreatedTournamentDTO(tournamentMapper.toDTO(savedTournament), seat, seatToken, inviteToken);
    }

    @Transactional
    public SeatAccessDTO joinTournament(Long tournamentId, String inviteToken) {
        Tournament tournament = findTournamentOrThrow(tournamentId);
        PlayerSymbol seat = tournament.claimInvite(inviteToken);
        publishTournamentUpdate(tournament);
        return new SeatAccessDTO(seat, tournament.tokenOf(seat));
    }

    @Transactional(readOnly = true)
    public TournamentDTO getTournament(Long tournamentId) {
        return tournamentMapper.toDTO(findTournamentOrThrow(tournamentId));
    }

    @Transactional
    public TournamentDTO playMove(
        Long tournamentId,
        int gameNumber,
        PlayMoveDTO move,
        String seatToken
    ) {
        Tournament tournament = findTournamentOrThrow(tournamentId);

        lockTournament(tournament);

        if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
            throw new IllegalMoveException("Tournament is not in progress");
        }
        if (tournament.isAwaitingOpponent()) {
            throw new IllegalMoveException("Waiting for the other player to join");
        }

         Game game = tournament.getCurrentGame();

        if (game.getGameNumber() != gameNumber) {
            throw new ResourceNotFoundException("Game " + gameNumber + " is not the current game");
        }

        PlayerSymbol seat = authorize(tournament, seatToken);

        PlayerSymbol symbol = seat != null ? seat : move.symbol();
        if (symbol == null) {
            throw new IllegalArgumentException("symbol is required");
        }

        GameState gameState = gameMapper.toState(game);

        int cell = move.row() * Constants.BOARD_SIZE + move.col();

        GameEngine.playMove(gameState, cell, symbol);

        game.applyState(
            gameMapper.toBoardString(gameState),
            gameState.getStatus(),
            gameState.getOutcome()
        );

        if (gameState.getStatus() == GameStatus.COMPLETED) {
            completeTournamentIfDecided(tournament);
        }

        return publishTournamentUpdate(tournament);
    }

    @Transactional
    public TournamentDTO startNextGame(Long tournamentId, String seatToken) {
        Tournament tournament = findTournamentOrThrow(tournamentId);

        authorize(tournament, seatToken);
        
        createNextGame(tournament);
        
        return publishTournamentUpdate(tournament);
    }

    private Game createNextGame(Tournament tournament) {
        if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
            throw new IllegalMoveException("Tournament is not in progress");
        }
        if (tournament.findCurrentGame().isPresent()) {
            throw new IllegalMoveException("The current game is still in progress");
        }

        int nextGameNumber = tournament.getGames().size() + 1;

        if (nextGameNumber > tournament.getFormat().getTotalGames()) {
            throw new IllegalMoveException("Tournament has reached its maximum number of games");
        }

        PlayerSymbol startingSymbol = nextGameNumber % 2 == 1 ? PlayerSymbol.X : PlayerSymbol.O;

        Game game = new Game(tournament, nextGameNumber, startingSymbol);
        tournament.addGame(game);
        return game;
    }

    private void completeTournamentIfDecided(Tournament tournament) {
        int xWins = tournament.countWins(PlayerSymbol.X);
        int oWins = tournament.countWins(PlayerSymbol.O);
        int remaining = tournament.getFormat().getTotalGames() - tournament.countCompletedGames();

        boolean decided = remaining == 0 || Math.abs(xWins - oWins) > remaining;

        if (decided) {
            Player winner = xWins > oWins ? tournament.getPlayerX()
                        : oWins > xWins ? tournament.getPlayerO()
                        : null;                      // null = tournament draw
            tournament.complete(winner);
        }
    }

    private Tournament findTournamentOrThrow(Long tournamentId) {
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + tournamentId));
    }

    private void validateCreateTournamentRequest(NewTournamentDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("New tournament request cannot be null");
        }

        if (request.mode() == TournamentMode.ONLINE && request.creatorSeat() == null) {
            throw new IllegalArgumentException("creatorSeat is required for online tournaments");
        }

        playerService.validatePlayerName(request.playerXName());
        playerService.validatePlayerName(request.playerOName());

        if (request.playerXName().strip().equalsIgnoreCase(request.playerOName().strip())) {
            throw new IllegalArgumentException("Player names must be different");
        }

        if (request.format() == null) {
            throw new IllegalArgumentException("Tournament format cannot be null");
        }
    }

    private PlayerSymbol authorize(Tournament tournament, String token) {
        if (tournament.getMode() == TournamentMode.LOCAL) {
            return null;
        }
        return tournament.seatFor(token)
            .orElseThrow(() -> new ForbiddenException("You are not a player in this tournament"));
    }

    private TournamentDTO publishTournamentUpdate(Tournament tournament) {
        TournamentDTO dto = tournamentMapper.toDTO(tournament);

        eventPublisher.publishEvent(new TournamentUpdatedEvent(dto));
        
        return dto;
    }

    private void lockTournament(Tournament tournament) {
        entityManager.lock(
            tournament,
            LockModeType.OPTIMISTIC_FORCE_INCREMENT
        );
    }

    @Transactional(readOnly = true)
    public PageDTO<TournamentHistoryEntryDTO> getTournamentHistory(
        int pageNumber,
        int pageSize
    ) {
        int safePageNumber = Math.max(pageNumber, 0);
        int safePageSize = Math.min(
            Math.max(pageSize, 1),
            Constants.MAX_PAGE_SIZE
        );

        Page<Tournament> tournaments =
            tournamentRepository.findByStatusOrderByCreatedAtDescIdDesc(TournamentStatus.COMPLETED, PageRequest.of(safePageNumber, safePageSize));

        Page<TournamentHistoryEntryDTO> history = tournaments.map(tournamentMapper::toHistoryDTO);

        return new PageDTO<>(
            history.getContent(),
            history.getNumber(),
            history.getSize(),
            history.getTotalElements(),
            history.getTotalPages()
        );
    }
}
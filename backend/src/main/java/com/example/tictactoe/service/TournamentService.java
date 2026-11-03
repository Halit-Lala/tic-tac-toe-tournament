package com.example.tictactoe.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tictactoe.exception.IllegalMoveException;
import com.example.tictactoe.exception.ResourceNotFoundException;
import com.example.tictactoe.model.domain.GameState;
import com.example.tictactoe.model.dto.request.NewTournamentDTO;
import com.example.tictactoe.model.dto.request.PlayMoveDTO;
import com.example.tictactoe.model.dto.response.PageDTO;
import com.example.tictactoe.model.dto.response.TournamentDTO;
import com.example.tictactoe.model.dto.response.TournamentHistoryEntryDTO;
import com.example.tictactoe.model.entity.Game;
import com.example.tictactoe.model.entity.Player;
import com.example.tictactoe.model.entity.Tournament;
import com.example.tictactoe.model.enums.GameStatus;
import com.example.tictactoe.model.enums.PlayerSymbol;
import com.example.tictactoe.model.enums.TournamentStatus;
import com.example.tictactoe.model.mapper.GameMapper;
import com.example.tictactoe.model.mapper.TournamentMapper;
import com.example.tictactoe.repository.TournamentRepository;
import com.example.tictactoe.util.Constants;

@Service
public class TournamentService {

    private final PlayerService playerService;
    private final TournamentRepository tournamentRepository;
    private final TournamentMapper tournamentMapper;
    private final GameMapper gameMapper;

    public TournamentService(
        PlayerService playerService,
        TournamentRepository tournamentRepository,
        TournamentMapper tournamentMapper,
        GameMapper gameMapper
    ) {
        this.playerService = playerService;
        this.tournamentRepository = tournamentRepository;
        this.tournamentMapper = tournamentMapper;
        this.gameMapper = gameMapper;
    }

    @Transactional
    public TournamentDTO createTournament(NewTournamentDTO request) {
        validateCreateTournamentRequest(request);

        Player playerX = playerService.findOrCreatePlayerByName(request.playerXName());
        Player playerO = playerService.findOrCreatePlayerByName(request.playerOName());

        Tournament tournament = new Tournament(playerX, playerO, request.format());
        tournament.start();
        createNextGame(tournament);

        Tournament savedTournament = tournamentRepository.save(tournament);
        return tournamentMapper.toDTO(savedTournament);
    }

    @Transactional(readOnly = true)
    public TournamentDTO getTournament(Long tournamentId) {
        return tournamentMapper.toDTO(findTournamentOrThrow(tournamentId));
    }

    @Transactional
    public TournamentDTO playMove(Long tournamentId, int gameNumber, PlayMoveDTO move) {
        Tournament tournament = findTournamentOrThrow(tournamentId);

        if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
            throw new IllegalMoveException("Tournament is not in progress");
        }

        Game game = tournament.getCurrentGame();

        if (game.getGameNumber() != gameNumber) {
            throw new ResourceNotFoundException("Game " + gameNumber + " is not the current game");
        }

        GameState gameState = gameMapper.toState(game);

        int cell = move.row() * Constants.BOARD_SIZE + move.col();
        
        GameEngine.playMove(gameState, cell, move.symbol());

        game.applyState(
            gameMapper.toBoardString(gameState),
            gameState.getStatus(),
            gameState.getOutcome()
        );

        if (gameState.getStatus() == GameStatus.COMPLETED) {
            completeTournamentIfDecided(tournament);
        }

        return tournamentMapper.toDTO(tournament);
    }

    @Transactional
    public TournamentDTO startNextGame(Long tournamentId) {
        Tournament tournament = findTournamentOrThrow(tournamentId);

        createNextGame(tournament);

        return tournamentMapper.toDTO(tournament);
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

        playerService.validatePlayerName(request.playerXName());
        playerService.validatePlayerName(request.playerOName());

        if (request.playerXName().strip().equalsIgnoreCase(request.playerOName().strip())) {
            throw new IllegalArgumentException("Player names must be different");
        }

        if (request.format() == null) {
            throw new IllegalArgumentException("Tournament format cannot be null");
        }
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
package com.example.tictactoe.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tictactoe.model.dto.request.NewTournamentDTO;
import com.example.tictactoe.model.dto.response.TournamentDTO;
import com.example.tictactoe.model.entity.Player;
import com.example.tictactoe.model.entity.Tournament;
import com.example.tictactoe.model.mapper.TournamentMapper;
import com.example.tictactoe.repository.TournamentRepository;

@Service
public class TournamentService {

    private final PlayerService playerService;
    private final TournamentRepository tournamentRepository;
    private final TournamentMapper tournamentMapper;

    public TournamentService(
        PlayerService playerService,
        TournamentRepository tournamentRepository,
        TournamentMapper tournamentMapper
    ) {
        this.playerService = playerService;
        this.tournamentRepository = tournamentRepository;
        this.tournamentMapper = tournamentMapper;
    }

    @Transactional
    public TournamentDTO createTournament(NewTournamentDTO request) {
        validateCreateTournamentRequest(request);

        Player playerX = playerService.findOrCreatePlayerByName(request.playerXName());
        Player playerO = playerService.findOrCreatePlayerByName(request.playerOName());

        Tournament tournament = new Tournament(
                playerX,
                playerO,
                request.format()
        );

        Tournament savedTournament = tournamentRepository.save(tournament);

        return tournamentMapper.toDTO(savedTournament);
    }

    @Transactional
    public TournamentDTO startTournament(Long tournamentId) {
        Tournament tournament = findTournamentOrThrow(tournamentId);

        tournament.start();
        tournament.startNextGame();

        return tournamentMapper.toDTO(tournament);
    }

    @Transactional(readOnly = true)
    public TournamentDTO getTournament(Long tournamentId) {
        Tournament tournament = findTournamentOrThrow(tournamentId);

        return tournamentMapper.toDTO(tournament);
    }

    private Tournament findTournamentOrThrow(Long tournamentId) {
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found with id: " + tournamentId));
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
}
package com.example.tictactoe.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.tictactoe.model.dto.request.NewTournamentDTO;
import com.example.tictactoe.model.dto.request.PlayMoveDTO;
import com.example.tictactoe.model.dto.response.PageDTO;
import com.example.tictactoe.model.dto.response.TournamentDTO;
import com.example.tictactoe.model.dto.response.TournamentHistoryEntryDTO;
import com.example.tictactoe.service.TournamentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {

    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @PostMapping
    public ResponseEntity<TournamentDTO> create(@Valid  @RequestBody NewTournamentDTO request) {
        TournamentDTO newTournament = tournamentService.createTournament(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newTournament.id())
                .toUri();

        return ResponseEntity.created(location).body(newTournament);
    }

    @GetMapping("/{id}")
    public TournamentDTO get(@PathVariable Long id) {
        TournamentDTO tournament = tournamentService.getTournament(id);

        return tournament;
    }

    @PostMapping("/{id}/games/{gameNumber}/moves")
    public TournamentDTO playMove(
        @PathVariable Long id,
        @PathVariable int gameNumber,
        @Valid @RequestBody PlayMoveDTO move
    ) {
        return tournamentService.playMove(id, gameNumber, move);
    }

    @PostMapping("/{id}/games/next")
    public TournamentDTO startNextGame(@PathVariable Long id) {
        return tournamentService.startNextGame(id);
    }

    @GetMapping
    public PageDTO<TournamentHistoryEntryDTO> getTournamentHistory(
        @RequestParam(defaultValue = "0") int pageNumber,
        @RequestParam(defaultValue = "10") int pageSize
    ) {
        return tournamentService.getTournamentHistory(pageNumber, pageSize);
    }
}
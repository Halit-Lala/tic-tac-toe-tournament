package com.example.tictactoe.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.tictactoe.model.dto.request.JoinRequestDTO;
import com.example.tictactoe.model.dto.request.NewTournamentDTO;
import com.example.tictactoe.model.dto.request.PlayMoveDTO;
import com.example.tictactoe.model.dto.response.CreatedTournamentDTO;
import com.example.tictactoe.model.dto.response.PageDTO;
import com.example.tictactoe.model.dto.response.SeatAccessDTO;
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
    public ResponseEntity<CreatedTournamentDTO> createTournament(@Valid @RequestBody NewTournamentDTO request) {
        CreatedTournamentDTO newTournament = tournamentService.createTournament(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newTournament.tournament().id())
                .toUri();

        return ResponseEntity.created(location).body(newTournament);
    }

    @GetMapping("/{id}")
    public TournamentDTO get(@PathVariable Long id) {
        TournamentDTO tournament = tournamentService.getTournament(id);

        return tournament;
    }

    @PostMapping("/{id}/join")
    public SeatAccessDTO join(
        @PathVariable Long id,
        @Valid @RequestBody JoinRequestDTO request
    ) {
        return tournamentService.joinTournament(id, request.inviteToken());
    }

    @PostMapping("/{id}/games/{gameNumber}/moves")
    public TournamentDTO playMove(
        @PathVariable Long id,
        @PathVariable int gameNumber,
        @RequestHeader(value = "X-Seat-Token", required = false) String seatToken,
        @Valid @RequestBody PlayMoveDTO move
    ) {
        return tournamentService.playMove(id, gameNumber, move, seatToken);
    }

    @PostMapping("/{id}/games/next")
    public TournamentDTO startNextGame(
        @PathVariable Long id,
        @RequestHeader(value = "X-Seat-Token", required = false) String seatToken
    ) {
        return tournamentService.startNextGame(id, seatToken);
    }

    @GetMapping
    public PageDTO<TournamentHistoryEntryDTO> getTournamentHistory(
        @RequestParam(defaultValue = "0") int pageNumber,
        @RequestParam(defaultValue = "10") int pageSize
    ) {
        return tournamentService.getTournamentHistory(pageNumber, pageSize);
    }
}
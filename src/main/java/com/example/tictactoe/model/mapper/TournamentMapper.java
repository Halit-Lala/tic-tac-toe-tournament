package com.example.tictactoe.model.mapper;

import org.springframework.stereotype.Component;

import com.example.tictactoe.model.dto.response.ScoreDTO;
import com.example.tictactoe.model.dto.response.TournamentDTO;
import com.example.tictactoe.model.entity.Game;
import com.example.tictactoe.model.entity.Tournament;

@Component
public class TournamentMapper {

    private final PlayerMapper playerMapper;
    private final GameMapper gameMapper;

    public TournamentMapper(
        PlayerMapper playerMapper,
        GameMapper gameMapper
    ) {
        this.playerMapper = playerMapper;
        this.gameMapper = gameMapper;
    }

    public TournamentDTO toDTO(Tournament tournament) {
        return new TournamentDTO(
            tournament.getId(),
            playerMapper.toDTO(tournament.getPlayerX()),
            playerMapper.toDTO(tournament.getPlayerO()),
            tournament.getFormat(),
            tournament.getStatus(),
            playerMapper.toDTO(tournament.getWinner()),
            calculateScore(tournament),
            gameMapper.toDTOList(tournament.getGames())
        );
    }

    private ScoreDTO calculateScore(Tournament tournament) {
        int playerXWins = 0;
        int playerOWins = 0;
        int draws = 0;

        for (Game game : tournament.getGames()) {
            if (game.getOutcome() == null) {
                continue;
            }

            switch (game.getOutcome()) {
                case X_WINS -> playerXWins++;
                case O_WINS -> playerOWins++;
                case DRAW -> draws++;
            }
        }

        return new ScoreDTO(
            playerXWins,
            playerOWins,
            draws
        );
    }
}
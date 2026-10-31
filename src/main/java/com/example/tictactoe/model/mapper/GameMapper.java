package com.example.tictactoe.model.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.tictactoe.model.dto.response.GameDTO;
import com.example.tictactoe.model.entity.Game;
import com.example.tictactoe.model.enums.GameStatus;
import com.example.tictactoe.model.enums.PlayerSymbol;

@Component
public class GameMapper {

    public GameDTO toDTO(Game game) {
        return new GameDTO(
            game.getId(),
            game.getGameNumber(),
            game.getStartingSymbol(),
            getNextSymbol(game),
            toBoard(game.getBoardState()),
            game.getStatus(),
            game.getOutcome()
        );
    }

    public List<GameDTO> toDTOList(List<Game> games) {
        return games.stream().map(this::toDTO).toList();
    }

    private List<List<PlayerSymbol>> toBoard(String boardState) {
        List<List<PlayerSymbol>> board = new ArrayList<>();

        for (int row = 0; row < 3; row++) {
            List<PlayerSymbol> boardRow = new ArrayList<>();

            for (int col = 0; col < 3; col++) {
                char cell = boardState.charAt(row * 3 + col);

                PlayerSymbol symbol = switch (cell) {
                    case 'X' -> PlayerSymbol.X;
                    case 'O' -> PlayerSymbol.O;
                    default -> null;
                };

                boardRow.add(symbol);
            }

            board.add(Collections.unmodifiableList(boardRow));
        }

        return Collections.unmodifiableList(board);
    }

    private PlayerSymbol getNextSymbol(Game game) {
        if (game.getStatus() == GameStatus.COMPLETED) {
            return null;
        }

        long occupiedCells = game.getBoardState()
                                .chars()
                                .filter(cell -> cell != '-')
                                .count();

        if (occupiedCells % 2 == 0) {
            return game.getStartingSymbol();
        }

        return game.getStartingSymbol() == PlayerSymbol.X ? PlayerSymbol.O : PlayerSymbol.X;
    }
}
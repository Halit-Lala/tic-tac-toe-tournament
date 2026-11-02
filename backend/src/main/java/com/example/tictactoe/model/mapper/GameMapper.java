package com.example.tictactoe.model.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.tictactoe.model.domain.GameState;
import com.example.tictactoe.model.dto.response.GameDTO;
import com.example.tictactoe.model.entity.Game;
import com.example.tictactoe.model.enums.GameStatus;
import com.example.tictactoe.model.enums.PlayerSymbol;
import com.example.tictactoe.util.Constants;

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

    public GameState toState(Game game) {
        String s = game.getBoardState();
        PlayerSymbol[] board = new PlayerSymbol[Constants.BOARD_CELLS_LENGTH];
        for (int i = 0; i < s.length(); i++) {
            board[i] = switch (s.charAt(i)) {
                case 'X' -> PlayerSymbol.X;
                case 'O' -> PlayerSymbol.O;
                default -> null;
            };
        }
        return new GameState(game.getStartingSymbol(), board, game.getStatus(), game.getOutcome());
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

    public String toBoardString(GameState state) {
        StringBuilder sb = new StringBuilder();
        for (PlayerSymbol cell : state.getBoard()) {
            sb.append(cell == null ? '-' : cell.name().charAt(0));
        }
        return sb.toString();
    }

    private PlayerSymbol getNextSymbol(Game game) {
        if (game.getStatus() == GameStatus.COMPLETED) {
            return null;
        }

        long occupiedCells = game.getBoardState()
                                .chars()
                                .filter(cell -> cell != '-')
                                .count();

        return occupiedCells % 2 == 0 ? game.getStartingSymbol() : game.getStartingSymbol().opposite();
    }
}
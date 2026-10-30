package com.example.tictactoe.model.entity;

import java.util.regex.Pattern;

import com.example.tictactoe.model.enums.GameOutcome;
import com.example.tictactoe.model.enums.GameStatus;
import com.example.tictactoe.model.enums.PlayerSymbol;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "game",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_game_tournament_number",
            columnNames = {"tournament_id", "game_number"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Game {

    public static final String EMPTY_BOARD = "---------";
    private static final Pattern VALID_BOARD = Pattern.compile("[XO-]{9}");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(name = "game_number", nullable = false)
    private int gameNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "starting_symbol", nullable = false, length = 1)
    private PlayerSymbol startingSymbol;

    @Column(name = "board_state", nullable = false, length = 9)
    private String boardState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private GameOutcome outcome;

    public Game(Tournament tournament, int gameNumber, PlayerSymbol startingSymbol) {
        if (tournament == null) {
            throw new IllegalArgumentException("Tournament cannot be null");
        }

        if (gameNumber < 1 || gameNumber > 5) {
            throw new IllegalArgumentException("Invalid game number");
        }
        if (startingSymbol == null) {
            throw new IllegalArgumentException("Starting symbol cannot be null");
        }

        this.tournament = tournament;
        this.gameNumber = gameNumber;
        this.startingSymbol = startingSymbol;
        this.boardState = EMPTY_BOARD;
        this.status = GameStatus.IN_PROGRESS;
    }
        
    public void updateBoardState(String boardState) {
        if (boardState == null || !VALID_BOARD.matcher(boardState).matches()) {
            throw new IllegalArgumentException("Board state must contain exactly 9 characters");
        }

        if (status == GameStatus.COMPLETED) {
            throw new IllegalStateException("Cannot update a completed game");
        }

        this.boardState = boardState;
    }

    public void complete(GameOutcome outcome) {
        if (status == GameStatus.COMPLETED) {
            throw new IllegalStateException("Game is already completed");
        }
        if (outcome == null) {
            throw new IllegalArgumentException("Game outcome cannot be null");
        }

        this.outcome = outcome;
        this.status = GameStatus.COMPLETED;
    }

}
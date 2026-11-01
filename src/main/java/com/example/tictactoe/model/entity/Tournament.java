package com.example.tictactoe.model.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.example.tictactoe.model.enums.GameOutcome;
import com.example.tictactoe.model.enums.GameStatus;
import com.example.tictactoe.model.enums.PlayerSymbol;
import com.example.tictactoe.model.enums.TournamentFormat;
import com.example.tictactoe.model.enums.TournamentStatus;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tournament")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_x_id", nullable = false)
    private Player playerX;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_o_id", nullable = false)
    private Player playerO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TournamentFormat format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TournamentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Player winner;

    @OneToMany(
        mappedBy = "tournament",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("gameNumber ASC")
    private List<Game> games = new ArrayList<>();

    public Tournament(
            Player playerX,
            Player playerO,
            TournamentFormat format
    ) {
        if (playerX == null || playerO == null) {
            throw new IllegalArgumentException("Both players are required");
        }

        if (playerX.equals(playerO)) {
            throw new IllegalArgumentException("Tournament players must be different");
        }

        if (format == null) {
            throw new IllegalArgumentException("Tournament format is required");
        }

        this.playerX = playerX;
        this.playerO = playerO;
        this.format = format;
        this.status = TournamentStatus.CREATED;
    }

    public void start() {
        if (status != TournamentStatus.CREATED) {
            throw new IllegalStateException("Only a created tournament can be started");
        }

        this.status = TournamentStatus.IN_PROGRESS;
    }

    public void complete(Player winner) {
        if (status != TournamentStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only an in-progress tournament can be completed");
        }

        if (winner != null && !winner.equals(playerX) && !winner.equals(playerO)) {
            throw new IllegalArgumentException("Winner must be one of the tournament players");
        }

        this.winner = winner;
        this.status = TournamentStatus.COMPLETED;
    }

    public Optional<Game> findCurrentGame() {
        return games.stream()
                .filter(g -> g.getStatus() == GameStatus.IN_PROGRESS)
                .findFirst();
    }

    public Game getCurrentGame() {
        return findCurrentGame()
                .orElseThrow(() -> new IllegalStateException("No game is currently in progress"));
    }

    public int countWins(PlayerSymbol symbol) {
        GameOutcome winning = symbol == PlayerSymbol.X ? GameOutcome.X_WINS : GameOutcome.O_WINS;
        return (int) games.stream()
                .filter(g -> g.getOutcome() == winning)
                .count();
    }

    public int countDraws() {
        return (int) games.stream()
                .filter(g -> g.getOutcome() == GameOutcome.DRAW)
                .count();
    }

    public int countCompletedGames() {
        return (int) games.stream()
                .filter(g -> g.getStatus() == GameStatus.COMPLETED)
                .count();
    }

    public List<Game> getGames() {
        return Collections.unmodifiableList(games);
    }

    public void addGame(Game game) {
        games.add(game);
    }
}
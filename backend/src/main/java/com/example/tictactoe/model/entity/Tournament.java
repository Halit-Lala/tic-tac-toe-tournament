package com.example.tictactoe.model.entity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.example.tictactoe.exception.ForbiddenException;
import com.example.tictactoe.exception.IllegalMoveException;
import com.example.tictactoe.model.enums.GameOutcome;
import com.example.tictactoe.model.enums.GameStatus;
import com.example.tictactoe.model.enums.PlayerSymbol;
import com.example.tictactoe.model.enums.TournamentFormat;
import com.example.tictactoe.model.enums.TournamentMode;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tournament")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Tournament {

    @Version
    private long version;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TournamentMode mode = TournamentMode.LOCAL;

    @Getter(AccessLevel.NONE)
    @Column(name = "player_x_token", length = 64)
    private String playerXToken;
    
    @Getter(AccessLevel.NONE)
    @Column(name = "player_o_token", length = 64)
    private String playerOToken;
    
    @Getter(AccessLevel.NONE)
    @Column(name = "invite_token", length = 64)
    private String inviteToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "invite_seat", length = 1)
    private PlayerSymbol inviteSeat;

    @OneToMany(
        mappedBy = "tournament",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("gameNumber ASC")
    private List<Game> games = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public void makeOnline(PlayerSymbol creatorSeat, String xToken, String oToken, String invite) {
        this.mode = TournamentMode.ONLINE;
        this.playerXToken = xToken;
        this.playerOToken = oToken;
        this.inviteToken = invite;
        this.inviteSeat = creatorSeat.opposite();
    }

    public boolean isAwaitingOpponent() {
        return mode == TournamentMode.ONLINE && inviteToken != null;
    }

    public Optional<PlayerSymbol> seatFor(String token) {
        if (mode != TournamentMode.ONLINE || token == null || token.isBlank()) return Optional.empty();
        if (sameToken(playerXToken, token)) return Optional.of(PlayerSymbol.X);
        if (sameToken(playerOToken, token)) return Optional.of(PlayerSymbol.O);
        return Optional.empty();
    }

    public PlayerSymbol claimInvite(String token) {
        if (!isAwaitingOpponent() || !sameToken(inviteToken, token)) {
            throw new ForbiddenException("This invite is invalid or was already used");
        }
        this.inviteToken = null;
        return inviteSeat;
    }

    public String tokenOf(PlayerSymbol seat) {
        return seat == PlayerSymbol.X ? playerXToken : playerOToken;
    }

    private static boolean sameToken(String expected, String given) {
        return expected != null && given != null && MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8));
    }

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
                .orElseThrow(() -> new IllegalMoveException("No game is currently in progress"));
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
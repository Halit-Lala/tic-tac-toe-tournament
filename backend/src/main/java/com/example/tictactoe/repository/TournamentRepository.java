package com.example.tictactoe.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.tictactoe.model.entity.Tournament;
import com.example.tictactoe.repository.projection.LeaderboardRow;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    @Query(
        value = """
            SELECT
                p.id AS playerId,
                p.name AS name,
                COALESCE(
                    SUM(CASE WHEN t.winner_id = p.id THEN 1 ELSE 0 END),
                    0
                ) AS wins,
                COUNT(t.id) AS played
            FROM player p
            INNER JOIN tournament t
                ON (t.player_x_id = p.id OR t.player_o_id = p.id)
                AND t.status = 'COMPLETED'
            GROUP BY p.id, p.name
            ORDER BY wins DESC, played DESC, p.name ASC
        """,
        countQuery = """
            SELECT COUNT(DISTINCT p.id)
            FROM player p
            INNER JOIN tournament t
                ON (t.player_x_id = p.id OR t.player_o_id = p.id)
                AND t.status = 'COMPLETED'
        """,
        nativeQuery = true
    )
    Page<LeaderboardRow> findLeaderboard(Pageable pageable);

}
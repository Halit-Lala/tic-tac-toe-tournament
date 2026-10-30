package com.example.tictactoe.repository;

import com.example.tictactoe.model.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {

    List<Game> findByTournamentIdOrderByGameNumberAsc(Long tournamentId);

    Optional<Game> findByTournamentIdAndGameNumber(Long tournamentId, int gameNumber);
}
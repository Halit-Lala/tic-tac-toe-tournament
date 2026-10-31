package com.example.tictactoe.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tictactoe.model.entity.Player;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByNormalizedName(String normalizedName);

    List<Player> findTop10ByNormalizedNameContainingOrderByNameAsc(
        String normalizedName
    );

}
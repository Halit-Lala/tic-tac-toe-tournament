package com.example.tictactoe.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tictactoe.model.entity.Tournament;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

}
package com.example.tictactoe.repository.projection;

public interface LeaderboardRow {
    Long getPlayerId();
    String getName();
    long getWins();
    long getPlayed();
}
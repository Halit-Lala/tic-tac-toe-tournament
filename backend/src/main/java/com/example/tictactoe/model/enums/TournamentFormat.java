package com.example.tictactoe.model.enums;

public enum TournamentFormat {

    BEST_OF_1(1),
    BEST_OF_3(3),
    BEST_OF_5(5);

    private final int totalGames;

    TournamentFormat(int totalGames) {
        this.totalGames = totalGames;
    }

    public int getTotalGames() {
        return totalGames;
    }

    public int getWinsRequired() {
        return (totalGames / 2) + 1;
    }
}
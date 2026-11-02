package com.example.tictactoe.model.enums;

public enum PlayerSymbol {
    X,
    O;

    public PlayerSymbol opposite() {
        return this == X ? O : X;
    }
}
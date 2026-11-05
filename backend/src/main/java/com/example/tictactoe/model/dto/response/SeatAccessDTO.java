package com.example.tictactoe.model.dto.response;

import com.example.tictactoe.model.enums.PlayerSymbol;

public record SeatAccessDTO(
    PlayerSymbol seat,
    String seatToken
) {}
package com.example.tictactoe.model.dto.request;

import jakarta.validation.constraints.NotBlank;

public record JoinRequestDTO(
    @NotBlank String inviteToken
) {}

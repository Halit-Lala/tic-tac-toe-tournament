package com.example.tictactoe.model.dto.response;

import java.util.List;

public record PageDTO<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
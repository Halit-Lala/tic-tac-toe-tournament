package com.example.tictactoe.validator;

import com.example.tictactoe.model.dto.request.NewTournamentDTO;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DistinctPlayerNamesValidator implements ConstraintValidator<DistinctPlayerNames, NewTournamentDTO> {

    @Override
    public boolean isValid(NewTournamentDTO dto, ConstraintValidatorContext ctx) {
        if (dto == null || dto.playerXName() == null || dto.playerOName() == null) {
            return true;
        }
        return !dto.playerXName().strip().equalsIgnoreCase(dto.playerOName().strip());
    }
}
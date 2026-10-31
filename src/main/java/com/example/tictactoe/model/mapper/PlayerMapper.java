package com.example.tictactoe.model.mapper;

import org.springframework.stereotype.Component;

import com.example.tictactoe.model.dto.response.PlayerDTO;
import com.example.tictactoe.model.entity.Player;

@Component
public class PlayerMapper {

    public PlayerDTO toDTO(Player player) {
        if (player == null) {
            return null;
        }

        return new PlayerDTO(
                player.getId(),
                player.getName()
        );
    }
}
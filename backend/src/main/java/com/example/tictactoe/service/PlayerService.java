package com.example.tictactoe.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tictactoe.model.dto.response.PlayerDTO;
import com.example.tictactoe.model.entity.Player;
import com.example.tictactoe.model.mapper.PlayerMapper;
import com.example.tictactoe.repository.PlayerRepository;
import com.example.tictactoe.util.Constants;

@Service
public class PlayerService {

    private final PlayerMapper playerMapper;
    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository, PlayerMapper playerMapper) {
        this.playerRepository = playerRepository;
        this.playerMapper = playerMapper;
    }

    @Transactional
    public Player findOrCreatePlayerByName(String name) {
        validatePlayerName(name);

        String normalizedName = normalizeName(name);

        return playerRepository.findByNormalizedName(normalizedName)
                .orElseGet(() -> playerRepository.save(
                    new Player(name.strip(), normalizedName)
                ));
    }

    @Transactional(readOnly = true)
    public List<PlayerDTO> searchPlayers(String search) {
        if (search == null || search.isBlank()) {
            return List.of();
        }

        String normalizedSearch = normalizeName(search);

        return playerMapper.toDTOList(
                playerRepository.findTop10ByNormalizedNameContainingOrderByNameAsc(normalizedSearch)
        );
    }

    public void validatePlayerName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Player name cannot be empty");
        }

        if (name.strip().length() > Constants.MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Player name cannot exceed " + Constants.MAX_NAME_LENGTH + " characters");
        }
    }

    private String normalizeName(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }
}
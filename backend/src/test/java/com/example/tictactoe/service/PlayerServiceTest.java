
package com.example.tictactoe.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.tictactoe.model.dto.response.PlayerDTO;
import com.example.tictactoe.model.entity.Player;
import com.example.tictactoe.model.mapper.PlayerMapper;
import com.example.tictactoe.repository.PlayerRepository;
import com.example.tictactoe.util.Constants;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerMapper playerMapper;

    @InjectMocks
    private PlayerService playerService;

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("John", "john");
    }

    @Test
    void shouldReturnExistingPlayer() {
        when(playerRepository.findByNormalizedName("john"))
                .thenReturn(Optional.of(player));

        Player result = playerService.findOrCreatePlayerByName("John");

        assertSame(player, result);

        verify(playerRepository).findByNormalizedName("john");
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    void shouldCreateNewPlayerWhenNotFound() {
        when(playerRepository.findByNormalizedName("john"))
                .thenReturn(Optional.empty());

        when(playerRepository.save(any(Player.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Player result = playerService.findOrCreatePlayerByName("John");

        assertEquals("John", result.getName());
        assertEquals("john", result.getNormalizedName());

        verify(playerRepository).save(any(Player.class));
    }

    @Test
    void shouldTrimPlayerNameBeforeSaving() {
        when(playerRepository.findByNormalizedName("john"))
                .thenReturn(Optional.empty());

        when(playerRepository.save(any(Player.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Player result = playerService.findOrCreatePlayerByName("  John  ");

        assertEquals("John", result.getName());
        assertEquals("john", result.getNormalizedName());
    }

    @Test
    void shouldNormalizePlayerNameToLowerCase() {
        when(playerRepository.findByNormalizedName("john"))
                .thenReturn(Optional.of(player));

        playerService.findOrCreatePlayerByName("JOHN");

        verify(playerRepository).findByNormalizedName("john");
    }

    @Test
    void shouldThrowExceptionWhenPlayerNameIsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> playerService.findOrCreatePlayerByName(null)
        );

        verifyNoInteractions(playerRepository);
    }

    @Test
    void shouldThrowExceptionWhenPlayerNameIsBlank() {
        assertThrows(
                IllegalArgumentException.class,
                () -> playerService.findOrCreatePlayerByName("   ")
        );

        verifyNoInteractions(playerRepository);
    }

    @Test
    void shouldThrowExceptionWhenPlayerNameExceedsMaximumLength() {
        String name = "A".repeat(Constants.MAX_NAME_LENGTH + 1);

        assertThrows(
                IllegalArgumentException.class,
                () -> playerService.findOrCreatePlayerByName(name)
        );

        verifyNoInteractions(playerRepository);
    }

    @Test
    void shouldReturnEmptyListWhenSearchIsNull() {
        List<PlayerDTO> result = playerService.searchPlayers(null);

        assertTrue(result.isEmpty());

        verifyNoInteractions(playerRepository, playerMapper);
    }

    @Test
    void shouldReturnEmptyListWhenSearchIsBlank() {
        List<PlayerDTO> result = playerService.searchPlayers("   ");

        assertTrue(result.isEmpty());

        verifyNoInteractions(playerRepository, playerMapper);
    }

    @Test
    void shouldSearchPlayersUsingNormalizedSearchTerm() {
        List<Player> players = List.of(player);
        List<PlayerDTO> expected = List.of();

        when(playerRepository
                .findTop10ByNormalizedNameContainingOrderByNameAsc("jo"))
                .thenReturn(players);

        when(playerMapper.toDTOList(players)).thenReturn(expected);

        List<PlayerDTO> result = playerService.searchPlayers("  JO  ");

        assertSame(expected, result);

        verify(playerRepository)
                .findTop10ByNormalizedNameContainingOrderByNameAsc("jo");

        verify(playerMapper).toDTOList(players);
    }
}

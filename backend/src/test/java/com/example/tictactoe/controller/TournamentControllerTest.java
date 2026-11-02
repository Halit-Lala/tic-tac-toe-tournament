package com.example.tictactoe.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.tictactoe.exception.IllegalMoveException;
import com.example.tictactoe.exception.ResourceNotFoundException;
import com.example.tictactoe.model.dto.request.NewTournamentDTO;
import com.example.tictactoe.model.dto.request.PlayMoveDTO;
import com.example.tictactoe.model.dto.response.TournamentDTO;
import com.example.tictactoe.model.entity.Game;
import com.example.tictactoe.model.enums.TournamentFormat;
import com.example.tictactoe.model.enums.TournamentStatus;
import com.example.tictactoe.service.TournamentService;

@WebMvcTest(TournamentController.class)
class TournamentControllerTest {

    private static final String VALID_CREATE_BODY = """
            {"playerXName":"Ann","playerOName":"Bob","format":"BEST_OF_3"}
            """;

    private static final String VALID_MOVE_BODY = """
            {"row":0,"col":0,"symbol":"X"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TournamentService tournamentService;

    // Nested objects are null on purpose: these tests check the HTTP layer, not the mapping.
    private TournamentDTO tournamentDto(long id) {
        return new TournamentDTO(id, null, null, TournamentFormat.BEST_OF_3,
                TournamentStatus.IN_PROGRESS, null, null, List.of());
    }

    // ---- POST /api/tournaments ----

    @Test
    void shouldCreateTournamentAndReturnLocation() throws Exception {
        when(tournamentService.createTournament(any(NewTournamentDTO.class)))
                .thenReturn(tournamentDto(7));

        mockMvc.perform(post("/api/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/tournaments/7")))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void shouldRejectBlankPlayerName() throws Exception {
        mockMvc.perform(post("/api/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerXName":"  ","playerOName":"Bob","format":"BEST_OF_3"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(containsString("playerXName")));

        verifyNoInteractions(tournamentService);
    }

    @Test
    void shouldRejectMissingFormat() throws Exception {
        mockMvc.perform(post("/api/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerXName":"Ann","playerOName":"Bob"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("format")));

        verifyNoInteractions(tournamentService);
    }

    @Test
    void shouldRejectUnknownFormatValue() throws Exception {
        mockMvc.perform(post("/api/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerXName":"Ann","playerOName":"Bob","format":"BEST_OF_7"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tournamentService);
    }

    @Test
    void shouldRejectMalformedJson() throws Exception {
        mockMvc.perform(post("/api/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tournamentService);
    }

    @Test
    void shouldReturn400WhenServiceRejectsInput() throws Exception {
        when(tournamentService.createTournament(any(NewTournamentDTO.class)))
                .thenThrow(new IllegalArgumentException("Player names must be different"));

        mockMvc.perform(post("/api/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").value("Player names must be different"));
    }

    // ---- GET /api/tournaments/{id} ----

    @Test
    void shouldReturnTournament() throws Exception {
        when(tournamentService.getTournament(7L)).thenReturn(tournamentDto(7));

        mockMvc.perform(get("/api/tournaments/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.format").value("BEST_OF_3"));
    }

    @Test
    void shouldReturn404WhenTournamentDoesNotExist() throws Exception {
        when(tournamentService.getTournament(99L))
                .thenThrow(new ResourceNotFoundException("Tournament not found with id: 99"));

        mockMvc.perform(get("/api/tournaments/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not found"))
                .andExpect(jsonPath("$.detail").value("Tournament not found with id: 99"));
    }

    @Test
    void shouldReturn400WhenIdIsNotANumber() throws Exception {
        mockMvc.perform(get("/api/tournaments/abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tournamentService);
    }

    // ---- POST /api/tournaments/{id}/games/{n}/moves ----

    @Test
    void shouldPlayMove() throws Exception {
        when(tournamentService.playMove(eq(7L), eq(1), any(PlayMoveDTO.class)))
                .thenReturn(tournamentDto(7));

        mockMvc.perform(post("/api/tournaments/7/games/1/moves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_MOVE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));

        verify(tournamentService).playMove(eq(7L), eq(1), any(PlayMoveDTO.class));
    }

    @Test
    void shouldRejectRowOutOfRange() throws Exception {
        mockMvc.perform(post("/api/tournaments/7/games/1/moves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"row":3,"col":0,"symbol":"X"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("row")));

        verify(tournamentService, never()).playMove(any(), any(Integer.class), any());
    }

    @Test
    void shouldRejectNegativeColumn() throws Exception {
        mockMvc.perform(post("/api/tournaments/7/games/1/moves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"row":0,"col":-1,"symbol":"X"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("col")));
    }

    @Test
    void shouldRejectMissingFieldsInMove() throws Exception {
        mockMvc.perform(post("/api/tournaments/7/games/1/moves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("row")))
                .andExpect(jsonPath("$.detail").value(containsString("col")))
                .andExpect(jsonPath("$.detail").value(containsString("symbol")));
    }

    @Test
    void shouldRejectUnknownSymbol() throws Exception {
        mockMvc.perform(post("/api/tournaments/7/games/1/moves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"row":0,"col":0,"symbol":"Z"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn409WhenMoveBreaksTheRules() throws Exception {
        when(tournamentService.playMove(eq(7L), eq(1), any(PlayMoveDTO.class)))
                .thenThrow(new IllegalMoveException("Cell is already occupied"));

        mockMvc.perform(post("/api/tournaments/7/games/1/moves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_MOVE_BODY))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Illegal move"))
                .andExpect(jsonPath("$.detail").value("Cell is already occupied"));
    }

    @Test
    void shouldReturn404WhenPlayingMoveOnUnknownTournament() throws Exception {
        when(tournamentService.playMove(eq(99L), eq(1), any(PlayMoveDTO.class)))
                .thenThrow(new ResourceNotFoundException("Tournament not found with id: 99"));

        mockMvc.perform(post("/api/tournaments/99/games/1/moves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_MOVE_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409WhenConcurrentUpdateIsDetected() throws Exception {
        when(tournamentService.playMove(eq(7L), eq(1), any(PlayMoveDTO.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Game.class, 1L));

        mockMvc.perform(post("/api/tournaments/7/games/1/moves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_MOVE_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Concurrent update"));
    }

    @Test
    void shouldReturn500WithoutLeakingDetailsOnUnexpectedError() throws Exception {
        when(tournamentService.playMove(eq(7L), eq(1), any(PlayMoveDTO.class)))
                .thenThrow(new IllegalStateException("internal detail: SELECT * FROM game"));

        mockMvc.perform(post("/api/tournaments/7/games/1/moves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_MOVE_BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Server error"))
                .andExpect(jsonPath("$.detail").value("Unexpected error"));
    }

    // ---- POST /api/tournaments/{id}/games/next ----

    @Test
    void shouldStartNextGame() throws Exception {
        when(tournamentService.startNextGame(7L)).thenReturn(tournamentDto(7));

        mockMvc.perform(post("/api/tournaments/7/games/next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void shouldReturn409WhenNextGameCannotStart() throws Exception {
        when(tournamentService.startNextGame(7L))
                .thenThrow(new IllegalMoveException("The current game is still in progress"));

        mockMvc.perform(post("/api/tournaments/7/games/next"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("The current game is still in progress"));
    }

    // ---- framework errors ----

    @Test
    void shouldReturn405ForWrongHttpMethod() throws Exception {
        mockMvc.perform(get("/api/tournaments/7/games/next"))
                .andExpect(status().isMethodNotAllowed());
    }
}
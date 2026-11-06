package com.example.tictactoe.controller;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.tictactoe.exception.ForbiddenException;
import com.example.tictactoe.exception.IllegalMoveException;
import com.example.tictactoe.exception.ResourceNotFoundException;
import com.example.tictactoe.model.dto.request.NewTournamentDTO;
import com.example.tictactoe.model.dto.request.PlayMoveDTO;
import com.example.tictactoe.model.dto.response.CreatedTournamentDTO;
import com.example.tictactoe.model.dto.response.SeatAccessDTO;
import com.example.tictactoe.model.dto.response.TournamentDTO;
import com.example.tictactoe.model.entity.Game;
import com.example.tictactoe.model.enums.PlayerSymbol;
import com.example.tictactoe.model.enums.TournamentFormat;
import com.example.tictactoe.model.enums.TournamentMode;
import com.example.tictactoe.model.enums.TournamentStatus;
import com.example.tictactoe.service.TournamentService;

@WebMvcTest(TournamentController.class)
class TournamentControllerTest {

    private static final String VALID_CREATE_BODY = """
            {"playerXName":"Ann","playerOName":"Bob","format":"BEST_OF_3", "mode":"LOCAL", "creatorSeat":null}
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
                TournamentStatus.IN_PROGRESS, null, null, List.of(),
                TournamentMode.LOCAL, false, 1L, null);
    }

    // ---- POST /api/tournaments ----

    @Test
    void shouldCreateTournamentAndReturnLocation() throws Exception {
        when(tournamentService.createTournament(any(NewTournamentDTO.class)))
                .thenReturn(new CreatedTournamentDTO(tournamentDto(7), null, null, null));

        mockMvc.perform(post("/api/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/tournaments/7")))
                .andExpect(jsonPath("$.tournament.id").value(7))
                .andExpect(jsonPath("$.tournament.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.seatToken").doesNotExist());
    }

    @Test
    void shouldReturnSeatAndInviteWhenCreatingOnlineTournament() throws Exception {
        when(tournamentService.createTournament(any(NewTournamentDTO.class)))
                .thenReturn(new CreatedTournamentDTO(tournamentDto(7), PlayerSymbol.X, "seat-token", "invite-token"));

        mockMvc.perform(post("/api/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerXName":"Ann","playerOName":"Bob","format":"BEST_OF_3",
                                 "mode":"ONLINE","creatorSeat":"X"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seat").value("X"))
                .andExpect(jsonPath("$.seatToken").value("seat-token"))
                .andExpect(jsonPath("$.inviteToken").value("invite-token"));
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
    void shouldRejectIdenticalNamesIgnoringCaseAndSpaces() throws Exception {
        mockMvc.perform(post("/api/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerXName":"Ann","playerOName":"  ann ","format":"BEST_OF_3"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Player names must be different")));

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
        void shouldRejectMissingMode() throws Exception {
        mockMvc.perform(post("/api/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerXName":"Ann","playerOName":"Bob","format":"BEST_OF_3"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("mode")));

        verifyNoInteractions(tournamentService);
        }

    @Test
    void shouldRejectUnknownMode() throws Exception {
        mockMvc.perform(post("/api/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerXName":"Ann","playerOName":"Bob","format":"BEST_OF_3","mode":"REMOTE"}
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
                .thenThrow(new IllegalArgumentException("creatorSeat is required for online tournaments"));

        mockMvc.perform(post("/api/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerXName":"Ann","playerOName":"Bob","format":"BEST_OF_3","mode":"ONLINE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").value("creatorSeat is required for online tournaments"));
    }

    // ---- GET /api/tournaments/{id} ----

    @Test
    void shouldReturnTournament() throws Exception {
        when(tournamentService.getTournament(7L)).thenReturn(tournamentDto(7));

        mockMvc.perform(get("/api/tournaments/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.format").value("BEST_OF_3"))
                .andExpect(jsonPath("$.mode").value("LOCAL"))
                .andExpect(jsonPath("$.awaitingOpponent").value(false));
    }

    @Test
    void shouldNotRequireATokenToReadATournament() throws Exception {
        when(tournamentService.getTournament(7L)).thenReturn(tournamentDto(7));

        mockMvc.perform(get("/api/tournaments/7"))
                .andExpect(status().isOk());
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

    // ---- POST /api/tournaments/{id}/join ----

    @Test
    void shouldJoinWithInviteToken() throws Exception {
        when(tournamentService.joinTournament(7L, "invite-token"))
                .thenReturn(new SeatAccessDTO(PlayerSymbol.O, "o-token"));

        mockMvc.perform(post("/api/tournaments/7/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inviteToken":"invite-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seat").value("O"))
                .andExpect(jsonPath("$.seatToken").value("o-token"));
    }

    @Test
    void shouldRejectBlankInviteToken() throws Exception {
        mockMvc.perform(post("/api/tournaments/7/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inviteToken":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("inviteToken")));

        verifyNoInteractions(tournamentService);
    }

    @Test
    void shouldReturn403WhenInviteIsInvalidOrAlreadyUsed() throws Exception {
        when(tournamentService.joinTournament(eq(7L), any()))
                .thenThrow(new ForbiddenException("This invite is invalid or was already used"));

        mockMvc.perform(post("/api/tournaments/7/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inviteToken":"used"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail").value("This invite is invalid or was already used"));
    }

    // ---- POST /api/tournaments/{id}/games/{n}/moves ----

    @Test
    void shouldPlayMove() throws Exception {
        when(tournamentService.playMove(eq(7L), eq(1), any(PlayMoveDTO.class), any()))
                .thenReturn(tournamentDto(7));

        mockMvc.perform(post("/api/tournaments/7/games/1/moves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_MOVE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));

        verify(tournamentService).playMove(eq(7L), eq(1), any(PlayMoveDTO.class), any());
    }

    @Test
    void shouldPassTheSeatTokenHeaderToTheService() throws Exception {
        when(tournamentService.playMove(eq(7L), eq(1), any(PlayMoveDTO.class), eq("seat-token")))
                .thenReturn(tournamentDto(7));

        mockMvc.perform(post("/api/tournaments/7/games/1/moves")
                        .header("X-Seat-Token", "seat-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_MOVE_BODY))
                .andExpect(status().isOk());

        verify(tournamentService).playMove(eq(7L), eq(1), any(PlayMoveDTO.class), eq("seat-token"));
    }

    @Test
    void shouldAcceptMoveWithoutSymbolForOnlineGames() throws Exception {
        when(tournamentService.playMove(eq(7L), eq(1), any(PlayMoveDTO.class), eq("seat-token")))
                .thenReturn(tournamentDto(7));

        mockMvc.perform(post("/api/tournaments/7/games/1/moves")
                        .header("X-Seat-Token", "seat-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"row":0,"col":0}
                                """))
                .andExpect(status().isOk());
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

        verify(tournamentService, never()).playMove(any(), anyInt(), any(), any());
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
    void shouldRejectMissingRowAndColumn() throws Exception {
        mockMvc.perform(post("/api/tournaments/7/games/1/moves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("row")))
                .andExpect(jsonPath("$.detail").value(containsString("col")))
                // symbol is optional now: the server derives it from the seat token in online games
                .andExpect(jsonPath("$.detail").value(not(containsString("symbol"))));
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
    void shouldReturn400WhenSymbolIsMissingInALocalGame() throws Exception {
        when(tournamentService.playMove(eq(7L), eq(1), any(PlayMoveDTO.class), any()))
                .thenThrow(new IllegalArgumentException("symbol is required"));

        mockMvc.perform(post("/api/tournaments/7/games/1/moves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"row":0,"col":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("symbol is required"));
    }

    @Test
    void shouldReturn403WhenSeatTokenIsMissingOrWrong() throws Exception {
        when(tournamentService.playMove(eq(7L), eq(1), any(PlayMoveDTO.class), any()))
                .thenThrow(new ForbiddenException("You are not a player in this tournament"));

        mockMvc.perform(post("/api/tournaments/7/games/1/moves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_MOVE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("You are not a player in this tournament"));
    }

    @Test
    void shouldReturn409WhenMoveBreaksTheRules() throws Exception {
        when(tournamentService.playMove(eq(7L), eq(1), any(PlayMoveDTO.class), any()))
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
        when(tournamentService.playMove(eq(99L), eq(1), any(PlayMoveDTO.class), any()))
                .thenThrow(new ResourceNotFoundException("Tournament not found with id: 99"));

        mockMvc.perform(post("/api/tournaments/99/games/1/moves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_MOVE_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409WhenConcurrentUpdateIsDetected() throws Exception {
        when(tournamentService.playMove(eq(7L), eq(1), any(PlayMoveDTO.class), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(Game.class, 1L));

        mockMvc.perform(post("/api/tournaments/7/games/1/moves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_MOVE_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Concurrent update"));
    }

    @Test
    void shouldReturn500WithoutLeakingDetailsOnUnexpectedError() throws Exception {
        when(tournamentService.playMove(eq(7L), eq(1), any(PlayMoveDTO.class), any()))
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
        when(tournamentService.startNextGame(eq(7L), any())).thenReturn(tournamentDto(7));

        mockMvc.perform(post("/api/tournaments/7/games/next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void shouldPassTheSeatTokenHeaderWhenStartingTheNextGame() throws Exception {
        when(tournamentService.startNextGame(7L, "seat-token")).thenReturn(tournamentDto(7));

        mockMvc.perform(post("/api/tournaments/7/games/next")
                        .header("X-Seat-Token", "seat-token"))
                .andExpect(status().isOk());

        verify(tournamentService).startNextGame(7L, "seat-token");
    }

    @Test
    void shouldReturn403WhenNextGameIsRequestedWithoutASeat() throws Exception {
        when(tournamentService.startNextGame(eq(7L), any()))
                .thenThrow(new ForbiddenException("You are not a player in this tournament"));

        mockMvc.perform(post("/api/tournaments/7/games/next"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn409WhenNextGameCannotStart() throws Exception {
        when(tournamentService.startNextGame(eq(7L), any()))
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
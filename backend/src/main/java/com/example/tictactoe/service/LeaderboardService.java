package com.example.tictactoe.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tictactoe.model.dto.response.LeaderboardEntryDTO;
import com.example.tictactoe.model.dto.response.PageDTO;
import com.example.tictactoe.model.dto.response.PlayerDTO;
import com.example.tictactoe.repository.TournamentRepository;
import com.example.tictactoe.repository.projection.LeaderboardRow;
import com.example.tictactoe.util.Constants;

@Service
public class LeaderboardService {

    private final TournamentRepository tournamentRepository;

    public LeaderboardService(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    @Transactional(readOnly = true)
    public PageDTO<LeaderboardEntryDTO> getLeaderboard(
        int pageNumber,
        int pageSize
    ) {
        int safePageNumber = Math.max(pageNumber, 0);
        int safePageSize = Math.min(Math.max(pageSize, 1), Constants.MAX_PAGE_SIZE);

        Page<LeaderboardRow> result = tournamentRepository.findLeaderboard(PageRequest.of(safePageNumber, safePageSize));

        int offset = safePageNumber * safePageSize;
        List<LeaderboardEntryDTO> content = new ArrayList<>();
        for (int i = 0; i < result.getContent().size(); i++) {
            LeaderboardRow row = result.getContent().get(i);
            content.add(new LeaderboardEntryDTO(
                offset + i + 1,
                new PlayerDTO(row.getPlayerId(), row.getName()),
                row.getWins(),
                row.getPlayed(),
                winRate(row.getWins(), row.getPlayed())
            ));
        }

        return new PageDTO<>(
            content,
            safePageNumber,
            safePageSize,
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    private double winRate(long wins, long played) {
        if (played == 0) return 0;
        return Math.round(wins * 1000.0 / played) / 10.0;
    }
}
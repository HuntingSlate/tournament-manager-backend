package com.tournamentmanager.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import com.tournamentmanager.backend.model.Match;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchRequest {

    @NotNull(message = "Tournament ID cannot be null")
    private Long tournamentId;

    private Long team1Id;
    private Long team2Id;

    private Long prevMatch1Id;
    private Long prevMatch2Id;

    @NotNull(message = "Start datetime cannot be null")
    private LocalDateTime startDatetime;

    private LocalDateTime endDatetime;
    private Long winningTeamId;

    private Integer roundNumber;
    private Integer matchNumberInRound;

    private Integer scoreTeam1;
    private Integer scoreTeam2;
    private Match.MatchStatus status;

}
package com.tournamentmanager.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchRequest {

    @NotNull(message = "Tournament ID cannot be null")
    private Long tournamentId;

    @NotNull(message = "Team 1 ID cannot be null")
    private Long team1Id;

    @NotNull(message = "Team 2 ID cannot be null")
    private Long team2Id;

    private Long prevMatch1Id;
    private Long prevMatch2Id;

    @NotNull(message = "Start datetime cannot be null")
    private LocalDateTime startDatetime;

    private LocalDateTime endDatetime;

    private Long winningTeamId;
}
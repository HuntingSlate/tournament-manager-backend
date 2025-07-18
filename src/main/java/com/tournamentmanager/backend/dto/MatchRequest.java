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
    private Long firstTeamId;
    private Long secondTeamId;
    @NotNull(message = "Start datetime cannot be null")
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private Long winningTeamId;
    private Integer bracketLevel;
    private Integer firstTeamScore;
    private Integer secondTeamScore;
    private Match.MatchStatus status;

}
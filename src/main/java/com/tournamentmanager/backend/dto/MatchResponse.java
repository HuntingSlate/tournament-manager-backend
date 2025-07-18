package com.tournamentmanager.backend.dto;

import com.tournamentmanager.backend.model.Match;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchResponse {

    private Long id;
    private Long tournamentId;
    private String tournamentName;
    private Long firstTeamId;
    private String firstTeamName;
    private Long secondTeamId;
    private String secondTeamName;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private Long winningTeamId;
    private String winningTeamName;
    private Integer bracketLevel;
    private Integer firstTeamScore;
    private Integer secondTeamScore;
    private Match.MatchStatus status;
    private List<MatchPlayerStatisticsResponse> firstTeamMatchStatistics;
    private List<MatchPlayerStatisticsResponse> secondTeamMatchStatistics;

}
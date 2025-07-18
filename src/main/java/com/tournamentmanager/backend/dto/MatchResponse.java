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
    private Long team1Id;
    private String team1Name;
    private Long team2Id;
    private String team2Name;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private Long winningTeamId;
    private String winningTeamName;
    private Integer roundNumber;
    private Integer matchNumberInRound;
    private Integer scoreTeam1;
    private Integer scoreTeam2;
    private Match.MatchStatus status;
    private List<MatchPlayerStatisticsResponse> matchStatistics;

}
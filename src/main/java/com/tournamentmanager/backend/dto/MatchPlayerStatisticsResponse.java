package com.tournamentmanager.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchPlayerStatisticsResponse {
    private Long id;
    private Long playerId;
    private String playerNickname;
    private Integer kills;
    private Integer deaths;
    private Integer assists;
}
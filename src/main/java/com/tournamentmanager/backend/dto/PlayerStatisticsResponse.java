package com.tournamentmanager.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStatisticsResponse {
    private Long id;
    private Long playerId;
    private String playerNickname;
    private Long gameId;
    private String gameName;
    private Integer totalKills;
    private Integer totalDeaths;
    private Integer totalAssists;
    private Double averageKills;
    private Double averageDeaths;
    private Double averageAssists;
    private Integer matchesPlayed;
}
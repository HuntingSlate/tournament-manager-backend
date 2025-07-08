package com.tournamentmanager.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchStatisticsRequest {

    @NotNull(message = "Player ID cannot be null")
    private Long playerId;

    @Min(value = 0, message = "Kills must be non-negative")
    private Integer kills;

    @Min(value = 0, message = "Deaths must be non-negative")
    private Integer deaths;

    @Min(value = 0, message = "Assists must be non-negative")
    private Integer assists;
}
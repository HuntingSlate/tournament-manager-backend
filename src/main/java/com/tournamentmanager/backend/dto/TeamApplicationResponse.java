package com.tournamentmanager.backend.dto;

import com.tournamentmanager.backend.model.TeamApplication;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamApplicationResponse {
    private Long id;
    private Long teamId;
    private String teamName;
    private Long tournamentId;
    private String tournamentName;
    private LocalDateTime applicationDate;
    private TeamApplication.ApplicationStatus status;
}
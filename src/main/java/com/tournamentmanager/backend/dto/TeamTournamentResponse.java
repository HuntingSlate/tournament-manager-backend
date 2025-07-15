package com.tournamentmanager.backend.dto;

import com.tournamentmanager.backend.model.Tournament;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TeamTournamentResponse {
    private Long tournamentId;
    private String tournamentName;
    private Tournament.TournamentStatus tournamentStatus;
}

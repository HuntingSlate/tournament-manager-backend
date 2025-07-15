package com.tournamentmanager.backend.dto;

import com.tournamentmanager.backend.model.Tournament;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TournamentResponse {

    private Long id;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String gameName;
    private Long gameId;
    private Long organizerId;
    private String organizerNickname;

    private String postalCode;
    private String city;
    private String street;
    private Integer buildingNumber;
    private Double latitude;
    private Double longitude;
    private boolean isLanTournament;

    private Integer maxTeams;
    private Integer currentTeams;
    private Tournament.TournamentStatus status;

    private List<TournamentTeamResponse> participatingTeams;
}